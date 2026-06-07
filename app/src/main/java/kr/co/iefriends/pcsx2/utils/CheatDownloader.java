package kr.co.iefriends.pcsx2.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Baixa arquivos .pnach de cheat do repositório oficial do PCSX2 no GitHub.
 *
 * Fontes suportadas:
 *  1. PCSX2/cheats  — cheats de jogos
 *     https://raw.githubusercontent.com/PCSX2/cheats/master/{CRC}.pnach
 *
 *  2. PCSX2/widescreen_patches — patches de widescreen (bônus)
 *     https://raw.githubusercontent.com/PCSX2/widescreen_patches/master/widescreen/{CRC}.pnach
 *
 * O CRC é sempre 8 caracteres hexadecimais maiúsculos (ex: B4EC5530).
 */
public class CheatDownloader {

    // ── URLs base ────────────────────────────────────────────────────────────
    private static final String BASE_CHEATS_URL =
            "https://raw.githubusercontent.com/PCSX2/cheats/master/%s.pnach";

    private static final String BASE_WS_URL =
            "https://raw.githubusercontent.com/PCSX2/widescreen_patches/master/widescreen/%s.pnach";

    // Fallback — mirror via jsDelivr CDN (funciona em redes que bloqueiam raw.github)
    private static final String CDN_CHEATS_URL =
            "https://cdn.jsdelivr.net/gh/PCSX2/cheats@master/%s.pnach";

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS    = 20_000;

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // ── Callback público ──────────────────────────────────────────────────────
    public interface Callback {
        /** Chamado na thread principal ao concluir. */
        void onResult(DownloadResult result);
    }

    /** Encapsula o resultado de um download. */
    public static class DownloadResult {
        public final boolean success;
        /** Caminho do arquivo salvo, ou null se falhou. */
        public final String savedPath;
        /** Mensagem de erro, ou null se sucesso. */
        public final String error;
        /** Número de cheats carregados do arquivo baixado. */
        public final int cheatCount;

        public DownloadResult(boolean success, String savedPath, String error, int cheatCount) {
            this.success   = success;
            this.savedPath = savedPath;
            this.error     = error;
            this.cheatCount = cheatCount;
        }

        public static DownloadResult failure(String error) {
            return new DownloadResult(false, null, error, 0);
        }

        public static DownloadResult success(String path, int count) {
            return new DownloadResult(true, path, null, count);
        }
    }

    // ── API principal ─────────────────────────────────────────────────────────

    /**
     * Baixa os cheats do jogo especificado de forma assíncrona.
     *
     * @param context   contexto Android
     * @param serial    serial do jogo (ex: "SLUS-20946") — usado para nomear o arquivo
     * @param crc       CRC inteiro do jogo (ex: 0xB4EC5530)
     * @param overwrite se true, substitui arquivo existente; se false, pula se já existe
     * @param callback  chamado na main thread com o resultado
     */
    public static void downloadCheats(Context context, String serial, int crc,
                                       boolean overwrite, Callback callback) {
        executor.execute(() -> {
            DownloadResult result = downloadSync(context, serial, crc, overwrite);
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    /**
     * Versão síncrona — deve ser chamada em thread de background.
     */
    public static DownloadResult downloadSync(Context context, String serial, int crc,
                                               boolean overwrite) {
        if (context == null || TextUtils.isEmpty(serial) || crc == 0) {
            return DownloadResult.failure("Dados do jogo inválidos (serial ou CRC ausente).");
        }

        File dataRoot = DataDirectoryManager.getDataRoot(context);
        if (dataRoot == null) {
            return DownloadResult.failure("Diretório de dados não encontrado.");
        }

        File cheatsDir = new File(dataRoot, "cheats");
        if (!cheatsDir.exists() && !cheatsDir.mkdirs()) {
            return DownloadResult.failure("Não foi possível criar a pasta cheats/.");
        }

        String crcHex = String.format(Locale.US, "%08X", crc);
        String fileName = CheatDatabase.buildPnachFileName(serial, crc);
        if (fileName == null) fileName = crcHex + ".pnach";

        File destFile = new File(cheatsDir, fileName);

        // Pula download se já existe e overwrite=false
        if (destFile.exists() && !overwrite) {
            int count = CheatManager.loadCheats(context, serial, crc).size();
            return DownloadResult.success(destFile.getAbsolutePath(), count);
        }

        // Tenta as URLs na ordem: GitHub raw → jsDelivr CDN
        String[] urls = {
            String.format(Locale.US, BASE_CHEATS_URL, crcHex),
            String.format(Locale.US, CDN_CHEATS_URL, crcHex),
        };

        String lastError = null;
        for (String urlStr : urls) {
            DownloadResult r = tryDownload(urlStr, destFile);
            if (r.success) {
                // Conta quantos cheats foram baixados
                int count = CheatManager.loadCheats(context, serial, crc).size();
                return DownloadResult.success(destFile.getAbsolutePath(), count);
            }
            lastError = r.error;
            // Apaga arquivo corrompido/vazio se existir
            if (destFile.exists() && destFile.length() == 0) destFile.delete();
        }

        return DownloadResult.failure(lastError != null ? lastError
                : "Cheats não encontrados para CRC " + crcHex + " em nenhuma fonte.");
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private static DownloadResult tryDownload(String urlStr, File dest) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "PCSX2OID/1.0 (Android; cheat-downloader)");
            conn.setInstanceFollowRedirects(true);
            conn.connect();

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return DownloadResult.failure("404 — arquivo não encontrado: " + urlStr);
            }
            if (code != HttpURLConnection.HTTP_OK) {
                return DownloadResult.failure("HTTP " + code + " ao acessar: " + urlStr);
            }

            // Valida Content-Length mínima (arquivo vazio = sem cheats)
            int contentLength = conn.getContentLength();
            if (contentLength == 0) {
                return DownloadResult.failure("Arquivo vazio retornado por: " + urlStr);
            }

            // Baixa e salva
            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                out.flush();
            }

            // Verifica que o arquivo salvo tem conteúdo válido
            if (!dest.exists() || dest.length() == 0) {
                return DownloadResult.failure("Arquivo baixado está vazio.");
            }

            return DownloadResult.success(dest.getAbsolutePath(), 0);

        } catch (IOException e) {
            return DownloadResult.failure("Erro de rede: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Verifica via HEAD request se existe um .pnach para o CRC fornecido,
     * sem baixar o conteúdo. Útil para mostrar um indicador "disponível".
     */
    public static void checkAvailability(int crc, CheckCallback callback) {
        executor.execute(() -> {
            String crcHex = String.format(Locale.US, "%08X", crc);
            String urlStr = String.format(Locale.US, BASE_CHEATS_URL, crcHex);
            boolean available = false;
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8_000);
                conn.setReadTimeout(8_000);
                conn.setRequestMethod("HEAD");
                conn.setRequestProperty("User-Agent", "PCSX2OID/1.0");
                conn.connect();
                available = conn.getResponseCode() == HttpURLConnection.HTTP_OK;
                conn.disconnect();
            } catch (IOException ignored) {}
            final boolean finalAvailable = available;
            mainHandler.post(() -> callback.onResult(finalAvailable));
        });
    }

    public interface CheckCallback {
        void onResult(boolean available);
    }
}

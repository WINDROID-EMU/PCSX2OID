package kr.co.iefriends.pcsx2.utils;

import android.content.Context;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Gerencia a leitura e escrita de cheats no formato .pnach do PCSX2.
 *
 * O PCSX2 carrega cheats de arquivos .pnach na pasta "cheats/" do data root.
 * O nome do arquivo é formado por: &lt;SERIAL&gt;_&lt;CRC8HEX&gt;.pnach
 * Exemplo: SLUS-20946_B4EC5530.pnach
 *
 * Cada arquivo .pnach contém linhas no formato:
 *   // Nome do Cheat
 *   patch=1,EE,2014F694,extended,03E00008
 */
public class CheatManager {

    private CheatManager() {}

    /**
     * Retorna o arquivo .pnach do jogo atual com base no serial e CRC.
     * Tenta o formato novo (SERIAL_CRC.pnach) e o legado (CRC.pnach).
     */
    public static File getCheatFile(Context context, String serial, int crc) {
        File dataRoot = DataDirectoryManager.getDataRoot(context);
        if (dataRoot == null || TextUtils.isEmpty(serial) || crc == 0) return null;

        File cheatsDir = new File(dataRoot, "cheats");
        if (!cheatsDir.exists()) cheatsDir.mkdirs();

        // Formato preferencial: SERIAL_CRC.pnach
        String primaryName = CheatDatabase.buildPnachFileName(serial, crc);
        if (primaryName != null) {
            File f = new File(cheatsDir, primaryName);
            if (f.exists()) return f;
        }

        // Formato legado: CRC.pnach
        String legacyName = CheatDatabase.buildLegacyPnachFileName(crc);
        if (legacyName != null) {
            File f = new File(cheatsDir, legacyName);
            if (f.exists()) return f;
        }

        // Não existe ainda — retorna o caminho do formato primário para criação
        if (primaryName != null) {
            return new File(cheatsDir, primaryName);
        }
        return null;
    }

    /**
     * Lê todos os CheatEntry do arquivo .pnach de um jogo.
     * Retorna lista vazia se o arquivo não existir ou não tiver cheats.
     */
    public static List<CheatEntry> loadCheats(Context context, String serial, int crc) {
        List<CheatEntry> result = new ArrayList<>();
        File file = getCheatFile(context, serial, crc);
        if (file == null || !file.exists()) return result;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            StringBuilder currentBlock = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // Linha de gametitle — ignorar como bloco de cheat
                if (trimmed.startsWith("gametitle=")) {
                    continue;
                }
                // Início de novo bloco de comentário após código existente
                if (trimmed.startsWith("//") && currentBlock.length() > 0
                        && containsPatchLine(currentBlock.toString())) {
                    CheatEntry entry = CheatEntry.fromPnachBlock(currentBlock.toString());
                    if (entry != null) result.add(entry);
                    currentBlock.setLength(0);
                }
                currentBlock.append(line).append("\n");
            }
            // Último bloco
            if (currentBlock.length() > 0 && containsPatchLine(currentBlock.toString())) {
                CheatEntry entry = CheatEntry.fromPnachBlock(currentBlock.toString());
                if (entry != null) result.add(entry);
            }
        } catch (IOException e) {
            DebugLog.e("CheatManager", "Erro lendo cheats: " + e.getMessage());
        }

        return result;
    }

    private static boolean containsPatchLine(String block) {
        for (String line : block.split("\n")) {
            if (line.trim().startsWith("patch=")) return true;
        }
        return false;
    }

    /**
     * Salva a lista de CheatEntry no arquivo .pnach do jogo.
     * Sobrescreve o arquivo existente com os cheats fornecidos.
     * Também adiciona o gametitle no cabeçalho.
     *
     * @param context  contexto Android
     * @param serial   serial do jogo (ex: "SLUS-20946")
     * @param crc      CRC do jogo (ex: 0xB4EC5530)
     * @param entries  lista de cheats a salvar
     * @param gameTitle nome do jogo para o cabeçalho (pode ser null)
     * @return true se salvo com sucesso
     */
    public static boolean saveCheats(Context context, String serial, int crc,
                                     List<CheatEntry> entries, String gameTitle) {
        File file = getCheatFile(context, serial, crc);
        if (file == null) return false;

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        StringBuilder sb = new StringBuilder();

        // Cabeçalho com nome do jogo
        if (!TextUtils.isEmpty(gameTitle)) {
            sb.append("gametitle=").append(gameTitle).append("\n");
        } else {
            String knownTitle = CheatDatabase.getTitleBySerial(serial);
            if (!TextUtils.isEmpty(knownTitle)) {
                sb.append("gametitle=").append(knownTitle).append("\n");
            }
        }
        sb.append("\n");

        for (CheatEntry entry : entries) {
            sb.append(entry.toPnachFormat()).append("\n");
        }

        try (FileWriter writer = new FileWriter(file, false)) {
            writer.write(sb.toString());
            return true;
        } catch (IOException e) {
            DebugLog.e("CheatManager", "Erro salvando cheats: " + e.getMessage());
            return false;
        }
    }

    /**
     * Adiciona um único cheat ao arquivo .pnach do jogo sem remover os existentes.
     */
    public static boolean appendCheat(Context context, String serial, int crc,
                                      CheatEntry entry) {
        List<CheatEntry> existing = loadCheats(context, serial, crc);
        existing.add(entry);
        return saveCheats(context, serial, crc, existing, null);
    }

    /**
     * Remove um cheat pelo índice e salva.
     */
    public static boolean removeCheat(Context context, String serial, int crc, int index) {
        List<CheatEntry> existing = loadCheats(context, serial, crc);
        if (index < 0 || index >= existing.size()) return false;
        existing.remove(index);
        return saveCheats(context, serial, crc, existing, null);
    }

    /**
     * Retorna todos os arquivos .pnach presentes na pasta cheats/.
     */
    public static List<File> listAllCheatFiles(Context context) {
        List<File> result = new ArrayList<>();
        File dataRoot = DataDirectoryManager.getDataRoot(context);
        if (dataRoot == null) return result;

        File cheatsDir = new File(dataRoot, "cheats");
        if (!cheatsDir.exists()) return result;

        File[] files = cheatsDir.listFiles((dir, name) ->
                name.toLowerCase(Locale.US).endsWith(".pnach"));
        if (files != null) {
            for (File f : files) result.add(f);
        }
        return result;
    }

    /**
     * Retorna o nome do jogo pelo serial do arquivo .pnach
     * usando o banco de dados interno e o nome do arquivo como fallback.
     */
    public static String getGameNameForPnach(String fileName) {
        if (TextUtils.isEmpty(fileName)) return "Unknown";
        // Extrai serial do nome do arquivo: SLUS-20946_B4EC5530.pnach
        String base = fileName.replaceAll("\\.pnach$", "");
        String serial = base.contains("_") ? base.substring(0, base.lastIndexOf("_")) : base;
        String title = CheatDatabase.getTitleBySerial(serial);
        return TextUtils.isEmpty(title) ? base : title;
    }
}

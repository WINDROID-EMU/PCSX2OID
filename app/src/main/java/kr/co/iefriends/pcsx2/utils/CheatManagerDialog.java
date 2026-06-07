package kr.co.iefriends.pcsx2.utils;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import kr.co.iefriends.pcsx2.NativeApp;

/**
 * Dialog de gerenciamento de cheats in-game.
 *
 * Funcionalidades:
 *  - Lista cheats do jogo atual (por Serial + CRC)
 *  - Toggle habilitar/desabilitar individualmente
 *  - Download automático do repositório PCSX2/cheats (GitHub)
 *  - Adição manual de cheats (formato patch=...)
 *  - Remoção de cheats
 *  - Salvar + recarregar no emulador sem fechar o jogo
 */
public class CheatManagerDialog extends DialogFragment {

    public static final String TAG = "CheatManagerDialog";

    private String gameSerial;
    private int    gameCrc;
    private String gameTitle;

    private List<CheatEntry> cheatEntries = new ArrayList<>();

    // Referências de UI
    private LinearLayout cheatListContainer;
    private TextView     tvStatus;
    private MaterialButton btnDownload;
    private ProgressBar  progressBar;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static CheatManagerDialog newInstance(String serial, int crc, String title) {
        CheatManagerDialog d = new CheatManagerDialog();
        Bundle args = new Bundle();
        args.putString("serial", serial);
        args.putInt("crc", crc);
        args.putString("title", title);
        d.setArguments(args);
        return d;
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            gameSerial = getArguments().getString("serial", "");
            gameCrc    = getArguments().getInt("crc", 0);
            gameTitle  = getArguments().getString("title", "");
        }
        setStyle(DialogFragment.STYLE_NO_TITLE,
                com.google.android.material.R.style.Theme_Material3_DayNight_Dialog_Alert);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context ctx = requireContext();

        // ── Raiz com scroll ───────────────────────────────────────────────────
        ScrollView root = new ScrollView(ctx);
        root.setFillViewport(true);

        LinearLayout container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(ctx, 20);
        container.setPadding(pad, pad, pad, pad);
        root.addView(container);

        // ── Cabeçalho ─────────────────────────────────────────────────────────
        TextView tvTitle = new TextView(ctx);
        tvTitle.setTextSize(18f);
        tvTitle.setTextColor(resolveColor(ctx, com.google.android.material.R.attr.colorPrimary));
        tvTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, 0, 0, dp(ctx, 2));

        String knownTitle = CheatDatabase.getTitleBySerial(gameSerial);
        String displayTitle = !TextUtils.isEmpty(gameTitle) ? gameTitle
                : (!TextUtils.isEmpty(knownTitle) ? knownTitle : "Jogo Desconhecido");
        tvTitle.setText(displayTitle);
        container.addView(tvTitle);

        TextView tvInfo = new TextView(ctx);
        tvInfo.setTextSize(11f);
        tvInfo.setTextColor(resolveColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant));
        tvInfo.setText("Serial: " + gameSerial + "   CRC: " + String.format("%08X", gameCrc));
        tvInfo.setPadding(0, 0, 0, dp(ctx, 12));
        container.addView(tvInfo);

        // ── Separador ─────────────────────────────────────────────────────────
        View div = new View(ctx);
        div.setBackgroundColor(resolveColor(ctx, com.google.android.material.R.attr.colorOutline));
        LinearLayout.LayoutParams divParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        divParams.bottomMargin = dp(ctx, 12);
        container.addView(div, divParams);

        // ── Botão de download ─────────────────────────────────────────────────
        // Linha: [⬇ Baixar Cheats Online]  [ProgressBar]
        LinearLayout downloadRow = new LinearLayout(ctx);
        downloadRow.setOrientation(LinearLayout.HORIZONTAL);
        downloadRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams dlRowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dlRowParams.bottomMargin = dp(ctx, 8);
        container.addView(downloadRow, dlRowParams);

        btnDownload = new MaterialButton(ctx);
        btnDownload.setText("⬇  Baixar Cheats Online");
        btnDownload.setTextAllCaps(false);
        LinearLayout.LayoutParams dlBtnParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        downloadRow.addView(btnDownload, dlBtnParams);
        btnDownload.setOnClickListener(v -> startDownload(ctx));

        progressBar = new ProgressBar(ctx);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                dp(ctx, 32), dp(ctx, 32));
        pbParams.leftMargin = dp(ctx, 8);
        downloadRow.addView(progressBar, pbParams);

        // ── TextView de status/feedback ───────────────────────────────────────
        tvStatus = new TextView(ctx);
        tvStatus.setTextSize(12f);
        tvStatus.setTextColor(resolveColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant));
        tvStatus.setPadding(0, 0, 0, dp(ctx, 8));
        tvStatus.setVisibility(View.GONE);
        container.addView(tvStatus);

        // ── Lista de cheats ───────────────────────────────────────────────────
        cheatListContainer = new LinearLayout(ctx);
        cheatListContainer.setOrientation(LinearLayout.VERTICAL);
        container.addView(cheatListContainer);

        // ── Separador inferior ────────────────────────────────────────────────
        View div2 = new View(ctx);
        div2.setBackgroundColor(resolveColor(ctx, com.google.android.material.R.attr.colorOutline));
        LinearLayout.LayoutParams div2Params =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        div2Params.topMargin = dp(ctx, 8);
        div2Params.bottomMargin = dp(ctx, 12);
        container.addView(div2, div2Params);

        // ── Ações inferiores ──────────────────────────────────────────────────
        MaterialButton btnAdd = new MaterialButton(ctx, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnAdd.setText("+ Adicionar Cheat Manual");
        btnAdd.setTextAllCaps(false);
        addFullWidthMargin(container, btnAdd, ctx, 0, 6);
        btnAdd.setOnClickListener(v -> showAddCheatDialog());

        MaterialButton btnSave = new MaterialButton(ctx);
        btnSave.setText("💾  Salvar e Aplicar");
        btnSave.setTextAllCaps(false);
        addFullWidthMargin(container, btnSave, ctx, 0, 6);
        btnSave.setOnClickListener(v -> saveAndApply());

        MaterialButton btnClose = new MaterialButton(ctx, null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnClose.setText("Fechar");
        btnClose.setTextAllCaps(false);
        addFullWidthMargin(container, btnClose, ctx, 0, 0);
        btnClose.setOnClickListener(v -> dismiss());

        // Carrega cheats existentes do disco
        loadCheatsFromDisk();

        // Verifica disponibilidade online de forma não-bloqueante
        checkOnlineAvailability();

        // Monta o dialog
        Dialog dialog = new Dialog(ctx,
                com.google.android.material.R.style.Theme_Material3_DayNight_Dialog_Alert);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    // ── Carregamento do disco ─────────────────────────────────────────────────

    private void loadCheatsFromDisk() {
        Context ctx = getContext();
        if (ctx == null) return;
        cheatEntries = CheatManager.loadCheats(ctx, gameSerial, gameCrc);
        rebuildCheatList();
    }

    private void rebuildCheatList() {
        if (cheatListContainer == null) return;
        cheatListContainer.removeAllViews();
        Context ctx = cheatListContainer.getContext();

        if (cheatEntries.isEmpty()) {
            TextView tvEmpty = new TextView(ctx);
            tvEmpty.setText("Nenhum cheat carregado.\nUse \"Baixar Cheats Online\" ou adicione manualmente.");
            tvEmpty.setTextColor(resolveColor(ctx, com.google.android.material.R.attr.colorOnSurfaceVariant));
            tvEmpty.setPadding(0, dp(ctx, 4), 0, dp(ctx, 8));
            cheatListContainer.addView(tvEmpty);
            return;
        }

        for (int i = 0; i < cheatEntries.size(); i++) {
            final int idx = i;
            cheatListContainer.addView(buildCheatCard(ctx, cheatEntries.get(i), idx));
        }
    }

    // ── Download online ───────────────────────────────────────────────────────

    /** Verifica silenciosamente se há cheats disponíveis e atualiza o botão. */
    private void checkOnlineAvailability() {
        if (gameCrc == 0) return;
        CheatDownloader.checkAvailability(gameCrc, available -> {
            if (!isAdded() || btnDownload == null) return;
            if (available) {
                btnDownload.setText("⬇  Baixar Cheats Online  ✓");
            } else {
                btnDownload.setText("⬇  Baixar Cheats Online  ✗");
                btnDownload.setAlpha(0.55f);
            }
        });
    }

    private void startDownload(Context ctx) {
        if (gameCrc == 0 || TextUtils.isEmpty(gameSerial)) {
            Toast.makeText(ctx, "Dados do jogo inválidos.", Toast.LENGTH_SHORT).show();
            return;
        }

        // UI: desativa botão e mostra spinner
        btnDownload.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        setStatus("Conectando ao repositório PCSX2/cheats…", false);

        CheatDownloader.downloadCheats(ctx, gameSerial, gameCrc, true, result -> {
            if (!isAdded()) return;

            progressBar.setVisibility(View.GONE);
            btnDownload.setEnabled(true);

            if (result.success) {
                // Recarrega a lista da tela
                loadCheatsFromDisk();
                setStatus("✅ " + result.cheatCount + " cheat(s) baixados com sucesso!", true);
            } else {
                setStatus("❌ " + result.error, true);
                new MaterialAlertDialogBuilder(ctx)
                        .setTitle("Cheats não encontrados")
                        .setMessage(
                                "Não foi possível encontrar cheats para este jogo no repositório oficial.\n\n" +
                                "CRC: " + String.format("%08X", gameCrc) + "\n\n" +
                                "Detalhe: " + result.error + "\n\n" +
                                "Você pode adicionar cheats manualmente usando o botão abaixo.")
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void setStatus(String msg, boolean autoHide) {
        if (tvStatus == null) return;
        tvStatus.setText(msg);
        tvStatus.setVisibility(View.VISIBLE);
        if (autoHide) {
            tvStatus.postDelayed(() -> {
                if (tvStatus != null) tvStatus.setVisibility(View.GONE);
            }, 5000);
        }
    }

    // ── Card de cheat individual ──────────────────────────────────────────────

    private View buildCheatCard(Context ctx, CheatEntry entry, int index) {
        MaterialCardView card = new MaterialCardView(ctx);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(ctx, 6);
        card.setLayoutParams(p);
        card.setCardElevation(1f);
        card.setRadius(dp(ctx, 10));
        card.setCardBackgroundColor(resolveColor(ctx,
                com.google.android.material.R.attr.colorSurfaceVariant));
        card.setStrokeColor(resolveColor(ctx, com.google.android.material.R.attr.colorOutline));
        card.setStrokeWidth(1);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 8), dp(ctx, 8));
        card.addView(row);

        // Switch habilitar/desabilitar
        MaterialSwitch sw = new MaterialSwitch(ctx);
        sw.setChecked(entry.enabled);
        sw.setPadding(0, 0, dp(ctx, 10), 0);
        sw.setOnCheckedChangeListener((btn, checked) -> entry.enabled = checked);
        row.addView(sw);

        // Textos
        LinearLayout texts = new LinearLayout(ctx);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        row.addView(texts, tp);

        TextView tvName = new TextView(ctx);
        tvName.setText(entry.name);
        tvName.setTextSize(13f);
        tvName.setTextColor(resolveColor(ctx, com.google.android.material.R.attr.colorOnSurface));
        tvName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        texts.addView(tvName);

        if (!TextUtils.isEmpty(entry.codes)) {
            String preview = entry.codes.length() > 45
                    ? entry.codes.substring(0, 45) + "…"
                    : entry.codes;
            TextView tvCode = new TextView(ctx);
            tvCode.setText(preview);
            tvCode.setTextSize(10f);
            tvCode.setTextColor(resolveColor(ctx,
                    com.google.android.material.R.attr.colorOnSurfaceVariant));
            texts.addView(tvCode);
        }

        // Botão deletar
        ImageButton del = new ImageButton(ctx);
        del.setImageResource(android.R.drawable.ic_menu_delete);
        del.setBackground(null);
        del.setPadding(dp(ctx, 6), dp(ctx, 6), dp(ctx, 6), dp(ctx, 6));
        del.setOnClickListener(v -> confirmDelete(index));
        row.addView(del);

        return card;
    }

    // ── Adicionar cheat manual ────────────────────────────────────────────────

    private void showAddCheatDialog() {
        Context ctx = getContext();
        if (ctx == null) return;

        LinearLayout form = new LinearLayout(ctx);
        form.setOrientation(LinearLayout.VERTICAL);
        int p = dp(ctx, 16);
        form.setPadding(p, dp(ctx, 8), p, 0);

        TextInputLayout tilName = new TextInputLayout(ctx, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        tilName.setHint("Nome do Cheat");
        TextInputEditText etName = new TextInputEditText(ctx);
        tilName.addView(etName);
        form.addView(tilName);

        LinearLayout.LayoutParams codesLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        codesLp.topMargin = dp(ctx, 10);

        TextInputLayout tilCodes = new TextInputLayout(ctx, null,
                com.google.android.material.R.attr.textInputOutlinedStyle);
        tilCodes.setHint("Código(s) patch=... (uma por linha)");
        TextInputEditText etCodes = new TextInputEditText(ctx);
        etCodes.setMinLines(3);
        etCodes.setGravity(Gravity.TOP);
        tilCodes.addView(etCodes);
        form.addView(tilCodes, codesLp);

        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Novo Cheat")
                .setView(form)
                .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                .setPositiveButton("Adicionar", (d, w) -> {
                    String name  = etName.getText() != null
                            ? etName.getText().toString().trim() : "";
                    String codes = etCodes.getText() != null
                            ? etCodes.getText().toString().trim() : "";
                    if (TextUtils.isEmpty(name)) {
                        Toast.makeText(ctx, "Informe o nome do cheat",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!codes.contains("patch=")) {
                        Toast.makeText(ctx, "Informe pelo menos um código patch=...",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    cheatEntries.add(new CheatEntry(name, codes, true));
                    rebuildCheatList();
                })
                .show();
    }

    // ── Remover cheat ─────────────────────────────────────────────────────────

    private void confirmDelete(int index) {
        Context ctx = getContext();
        if (ctx == null || index < 0 || index >= cheatEntries.size()) return;
        new MaterialAlertDialogBuilder(ctx)
                .setTitle("Remover Cheat")
                .setMessage("Remover \"" + cheatEntries.get(index).name + "\"?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Remover", (d, w) -> {
                    cheatEntries.remove(index);
                    rebuildCheatList();
                })
                .show();
    }

    // ── Salvar e aplicar ──────────────────────────────────────────────────────

    private void saveAndApply() {
        Context ctx = getContext();
        if (ctx == null) return;

        List<CheatEntry> toSave = new ArrayList<>();
        for (CheatEntry e : cheatEntries) {
            if (e.enabled) toSave.add(e);
        }

        boolean ok = CheatManager.saveCheats(ctx, gameSerial, gameCrc, toSave, gameTitle);
        if (ok) {
            try {
                NativeApp.setEnableCheats(false);
                NativeApp.reloadCheats();
                NativeApp.setEnableCheats(!toSave.isEmpty());
            } catch (Throwable ignored) {}
            Toast.makeText(ctx,
                    toSave.isEmpty()
                            ? "Cheats desativados."
                            : "✅ " + toSave.size() + " cheat(s) aplicado(s)!",
                    Toast.LENGTH_LONG).show();
            dismiss();
        } else {
            Toast.makeText(ctx,
                    "Erro ao salvar. Verifique as permissões de armazenamento.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    private static void addFullWidthMargin(LinearLayout parent, View v,
                                           Context ctx, int topDp, int bottomDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin    = dp(ctx, topDp);
        lp.bottomMargin = dp(ctx, bottomDp);
        parent.addView(v, lp);
    }

    private static int dp(Context ctx, int dp) {
        return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
    }

    private static int resolveColor(Context ctx, int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (ctx.getTheme().resolveAttribute(attr, tv, true)) return tv.data;
        return 0xFF888888;
    }
}

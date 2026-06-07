package kr.co.iefriends.pcsx2.utils;

import android.text.TextUtils;

/**
 * Representa um único cheat code para um jogo PS2.
 *
 * Formato de entrada .pnach do PCSX2:
 *   // nome_do_cheat
 *   patch=1,EE,XXXXXXXX,extended,YYYYYYYY
 *
 * Esta classe mantém o nome (comentário), a(s) linha(s) de código
 * e o estado enabled/disabled (para a UI de gerenciamento).
 */
public class CheatEntry {

    /** Nome legível do cheat (derivado do comentário // no .pnach). */
    public String name;

    /** Linhas de código raw do cheat (ex: "patch=1,EE,..."). */
    public String codes;

    /** Se o cheat está habilitado ou não (para uso na UI). */
    public boolean enabled;

    /** Autor ou fonte do cheat (opcional). */
    public String author;

    public CheatEntry(String name, String codes, boolean enabled) {
        this.name = TextUtils.isEmpty(name) ? "Unnamed Cheat" : name.trim();
        this.codes = codes != null ? codes.trim() : "";
        this.enabled = enabled;
        this.author = "";
    }

    public CheatEntry(String name, String codes) {
        this(name, codes, true);
    }

    /** Serializa para o formato .pnach do PCSX2 */
    public String toPnachFormat() {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(name)) {
            sb.append("// ").append(name).append("\n");
        }
        if (!TextUtils.isEmpty(author)) {
            sb.append("// Author: ").append(author).append("\n");
        }
        if (!TextUtils.isEmpty(codes)) {
            sb.append(codes).append("\n");
        }
        return sb.toString();
    }

    /**
     * Cria um CheatEntry a partir de um bloco de texto .pnach.
     * O bloco pode começar com linhas de comentário e ter linhas patch=.
     */
    public static CheatEntry fromPnachBlock(String block) {
        if (TextUtils.isEmpty(block)) return null;
        String[] lines = block.split("\n");
        StringBuilder nameBuilder = new StringBuilder();
        StringBuilder codeBuilder = new StringBuilder();
        String author = null;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.startsWith("//")) {
                String comment = line.substring(2).trim();
                if (comment.toLowerCase().startsWith("author:")) {
                    author = comment.substring(7).trim();
                } else if (nameBuilder.length() == 0) {
                    nameBuilder.append(comment);
                }
            } else if (line.startsWith("patch=") || line.startsWith("gametitle=")) {
                if (codeBuilder.length() > 0) codeBuilder.append("\n");
                codeBuilder.append(line);
            }
        }

        String name = nameBuilder.length() > 0 ? nameBuilder.toString() : "Cheat";
        String codes = codeBuilder.toString();
        if (TextUtils.isEmpty(codes)) return null;

        CheatEntry entry = new CheatEntry(name, codes, true);
        entry.author = author != null ? author : "";
        return entry;
    }

    @Override
    public String toString() {
        return "CheatEntry{name='" + name + "', enabled=" + enabled + "}";
    }
}

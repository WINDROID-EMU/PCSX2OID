package kr.co.iefriends.pcsx2.utils;

import android.text.TextUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Banco de dados de jogos PS2 para identificação por serial e CRC.
 * Mapeia serial do jogo para o nome e fornece utilitários de lookup.
 *
 * O PCSX2 identifica cheats por CRC do jogo — arquivos .pnach são nomeados
 * como &lt;SERIAL&gt;_&lt;CRC8HEX&gt;.pnach ou simplesmente &lt;CRC8HEX&gt;.pnach.
 */
public final class CheatDatabase {

    private CheatDatabase() {}

    /**
     * Mapa de serial PS2 → nome do jogo.
     * Serials seguem o formato padrão: SLUS-XXXXX, SLES-XXXXX, SCES-XXXXX, etc.
     */
    private static final Map<String, String> SERIAL_TO_TITLE;

    static {
        Map<String, String> map = new HashMap<>();

        // --- América do Norte (SLUS / SCUS) ---
        map.put("SLUS-20062", "Grand Theft Auto III");
        map.put("SLUS-20413", "Grand Theft Auto: Vice City");
        map.put("SLUS-20946", "Grand Theft Auto: San Andreas");
        map.put("SLUS-20989", "Grand Theft Auto: Liberty City Stories");
        map.put("SLUS-21423", "Grand Theft Auto: Vice City Stories");
        map.put("SLUS-20388", "God of War");
        map.put("SLUS-21444", "God of War II");
        map.put("SLUS-20842", "Shadow of the Colossus");
        map.put("SLUS-20769", "Ico");
        map.put("SLUS-20186", "Metal Gear Solid 2: Sons of Liberty");
        map.put("SLUS-20810", "Metal Gear Solid 3: Snake Eater");
        map.put("SLUS-21294", "Metal Gear Solid 3: Subsistence");
        map.put("SLUS-20228", "Kingdom Hearts");
        map.put("SLUS-20892", "Kingdom Hearts II");
        map.put("SLUS-20972", "Kingdom Hearts Re: Chain of Memories");
        map.put("SLUS-20287", "Final Fantasy X");
        map.put("SLUS-20791", "Final Fantasy X-2");
        map.put("SLUS-21441", "Final Fantasy XII");
        map.put("SLUS-20554", "Devil May Cry");
        map.put("SLUS-20642", "Devil May Cry 2");
        map.put("SLUS-20942", "Devil May Cry 3: Dante's Awakening");
        map.put("SLUS-21230", "Devil May Cry 3: Special Edition");
        map.put("SLUS-20097", "Tekken 4");
        map.put("SLUS-20595", "Tekken 5");
        map.put("SLUS-21209", "Tekken 5: Dark Resurrection");
        map.put("SLUS-20173", "Ratchet & Clank");
        map.put("SLUS-20456", "Ratchet & Clank: Going Commando");
        map.put("SLUS-20609", "Ratchet & Clank: Up Your Arsenal");
        map.put("SLUS-21072", "Ratchet: Deadlocked");
        map.put("SLUS-20595", "Tekken 5");
        map.put("SLUS-20256", "Jak and Daxter: The Precursor Legacy");
        map.put("SLUS-20708", "Jak II");
        map.put("SLUS-20985", "Jak 3");
        map.put("SLUS-21227", "Jak X: Combat Racing");
        map.put("SLUS-20731", "Sly Cooper and the Thievius Raccoonus");
        map.put("SLUS-20958", "Sly 2: Band of Thieves");
        map.put("SLUS-21254", "Sly 3: Honor Among Thieves");
        map.put("SLUS-20170", "Crash Bandicoot: The Wrath of Cortex");
        map.put("SLUS-20571", "Crash Nitro Kart");
        map.put("SLUS-20819", "Crash Twinsanity");
        map.put("SLUS-21070", "Crash: Mind Over Mutant");
        map.put("SLUS-20486", "Spyro: Enter the Dragonfly");
        map.put("SLUS-20861", "Spyro: A Hero's Tail");
        map.put("SLUS-20678", "Burnout 3: Takedown");
        map.put("SLUS-21441", "Final Fantasy XII");
        map.put("SLUS-20512", "Dragon Ball Z: Budokai");
        map.put("SLUS-20737", "Dragon Ball Z: Budokai 2");
        map.put("SLUS-20882", "Dragon Ball Z: Budokai 3");
        map.put("SLUS-21152", "Dragon Ball Z: Budokai Tenkaichi");
        map.put("SLUS-21390", "Dragon Ball Z: Budokai Tenkaichi 2");
        map.put("SLUS-21678", "Dragon Ball Z: Budokai Tenkaichi 3");
        map.put("SLUS-20801", "Dragon Ball Z: Infinite World");
        map.put("SLUS-20413", "Grand Theft Auto: Vice City");
        map.put("SCUS-97264", "Gran Turismo 3: A-Spec");
        map.put("SCUS-97436", "Gran Turismo 4");
        map.put("SCUS-97489", "Gran Turismo 4: Prologue");
        map.put("SLUS-20288", "Resident Evil Code: Veronica X");
        map.put("SLUS-20866", "Resident Evil 4");
        map.put("SLUS-20842", "Shadow of the Colossus");
        map.put("SLUS-20508", "Onimusha: Warlords");
        map.put("SLUS-20677", "Onimusha 2: Samurai's Destiny");
        map.put("SLUS-20933", "Onimusha 3: Demon Siege");
        map.put("SLUS-21304", "Onimusha: Dawn of Dreams");
        map.put("SLUS-20228", "Kingdom Hearts");
        map.put("SLUS-20773", "Baldur's Gate: Dark Alliance");
        map.put("SLUS-20388", "God of War");
        map.put("SLUS-20413", "Grand Theft Auto: Vice City");
        map.put("SLUS-20512", "Dragon Ball Z: Budokai");
        map.put("SLUS-20634", "WWE SmackDown! Here Comes the Pain");
        map.put("SLUS-21002", "WWE SmackDown! vs. RAW 2006");
        map.put("SLUS-21350", "WWE SmackDown! vs. RAW 2007");
        map.put("SLUS-21693", "WWE SmackDown vs. RAW 2008");
        map.put("SLUS-20801", "Naruto: Ultimate Ninja");
        map.put("SLUS-21209", "Naruto: Ultimate Ninja 2");
        map.put("SLUS-21598", "Naruto: Ultimate Ninja 3");
        map.put("SLUS-21505", "Naruto: Ultimate Ninja Heroes 2");
        map.put("SLUS-20762", "Prince of Persia: The Sands of Time");
        map.put("SLUS-21036", "Prince of Persia: Warrior Within");
        map.put("SLUS-21299", "Prince of Persia: The Two Thrones");
        map.put("SLUS-20505", "Mortal Kombat: Deadly Alliance");
        map.put("SLUS-20831", "Mortal Kombat: Deception");
        map.put("SLUS-21444", "God of War II");
        map.put("SLUS-20587", "Baldur's Gate: Dark Alliance II");
        map.put("SLUS-20731", "Sly Cooper and the Thievius Raccoonus");
        map.put("SLUS-20488", "Star Wars: Battlefront");
        map.put("SLUS-21240", "Star Wars: Battlefront II");
        map.put("SLUS-20748", "Zone of the Enders");
        map.put("SLUS-20851", "Zone of the Enders: The 2nd Runner");
        map.put("SLUS-20505", "Mortal Kombat: Deadly Alliance");
        map.put("SLUS-20831", "Mortal Kombat: Deception");
        map.put("SLUS-21241", "Mortal Kombat: Armageddon");
        map.put("SLUS-20587", "Baldur's Gate: Dark Alliance II");
        map.put("SLUS-20388", "God of War");
        map.put("SLUS-20456", "Ratchet & Clank: Going Commando");

        // --- Europa (SLES / SCES) ---
        map.put("SLES-50330", "Grand Theft Auto III");
        map.put("SLES-51061", "Grand Theft Auto: Vice City");
        map.put("SLES-52927", "Grand Theft Auto: San Andreas");
        map.put("SCES-50807", "Gran Turismo 3: A-Spec");
        map.put("SCES-51719", "Gran Turismo 4");
        map.put("SCES-52438", "Shadow of the Colossus");
        map.put("SCES-50998", "Ico");
        map.put("SLES-50427", "Metal Gear Solid 2: Sons of Liberty");
        map.put("SLES-82009", "Metal Gear Solid 3: Snake Eater");
        map.put("SLES-53155", "Kingdom Hearts");
        map.put("SLES-54114", "Kingdom Hearts II");
        map.put("SLES-50882", "Final Fantasy X");
        map.put("SLES-51821", "Final Fantasy X-2");
        map.put("SLES-54355", "Final Fantasy XII");
        map.put("SLES-50441", "Devil May Cry");
        map.put("SLES-51615", "Devil May Cry 2");
        map.put("SLES-52529", "Devil May Cry 3: Dante's Awakening");
        map.put("SLES-50490", "Tekken 4");
        map.put("SLES-53017", "Tekken 5");
        map.put("SLES-50916", "God of War");
        map.put("SLES-54902", "God of War II");
        map.put("SLES-52067", "Ratchet & Clank");
        map.put("SLES-51607", "Resident Evil Code: Veronica X");
        map.put("SLES-53702", "Resident Evil 4");
        map.put("SLES-53155", "Kingdom Hearts");
        map.put("SLES-52781", "Dragon Ball Z: Budokai 2");
        map.put("SLES-53330", "Dragon Ball Z: Budokai 3");
        map.put("SLES-54087", "Dragon Ball Z: Budokai Tenkaichi");
        map.put("SLES-54350", "Dragon Ball Z: Budokai Tenkaichi 2");
        map.put("SLES-54575", "Dragon Ball Z: Budokai Tenkaichi 3");
        map.put("SLES-53580", "Prince of Persia: The Sands of Time");
        map.put("SLES-53181", "Naruto: Ultimate Ninja");
        map.put("SLES-54438", "Star Wars: Battlefront II");

        // --- Japão (SLPS / SCPS / SLPM / SCPM) ---
        map.put("SLPM-65077", "Metal Gear Solid 2: Sons of Liberty");
        map.put("SLPM-65834", "Metal Gear Solid 3: Snake Eater");
        map.put("SCPS-15046", "Gran Turismo 3: A-Spec");
        map.put("SCPS-15100", "Gran Turismo 4");
        map.put("SCPS-15116", "Shadow of the Colossus (Wander to Kyozou)");
        map.put("SCPS-10096", "Ico");
        map.put("SLPM-65034", "Grand Theft Auto III");
        map.put("SLPS-25424", "Grand Theft Auto: Vice City");
        map.put("SLPM-66024", "Kingdom Hearts");
        map.put("SLPM-66233", "Kingdom Hearts II");
        map.put("SLPM-65035", "Final Fantasy X");
        map.put("SLPM-65453", "Final Fantasy X-2");
        map.put("SLPM-66268", "Final Fantasy XII");
        map.put("SLPM-65307", "Devil May Cry");
        map.put("SLPM-65374", "Devil May Cry 2");
        map.put("SLPM-65666", "Devil May Cry 3: Dante's Awakening");
        map.put("SLPM-65895", "Tekken 5");
        map.put("SCPS-15014", "God of War");
        map.put("SCPS-15140", "God of War II");
        map.put("SLPM-66270", "Dragon Ball Z: Budokai Tenkaichi 3");
        map.put("SLPM-66604", "Naruto: Ultimate Ninja 3");

        // --- Brasil / América Latina (SLUS-PT / SLUS com localização) ---
        map.put("SLUS-20946", "GTA: San Andreas");

        SERIAL_TO_TITLE = Collections.unmodifiableMap(map);
    }

    /**
     * Retorna o nome do jogo pelo serial, ou null se não encontrado.
     * A busca é feita ignorando maiúsculas/minúsculas e normaliza o separador.
     */
    public static String getTitleBySerial(String serial) {
        if (TextUtils.isEmpty(serial)) return null;
        String normalized = serial.trim().toUpperCase();
        String title = SERIAL_TO_TITLE.get(normalized);
        if (title != null) return title;
        // Tenta com hífen normalizado (ex: "SLUS20062" → "SLUS-20062")
        if (normalized.length() >= 9 && !normalized.contains("-")) {
            String withHyphen = normalized.substring(0, 4) + "-" + normalized.substring(4);
            title = SERIAL_TO_TITLE.get(withHyphen);
        }
        return title;
    }

    /**
     * Retorna o nome do arquivo .pnach esperado pelo PCSX2 para os cheats do jogo.
     * Formato: &lt;SERIAL&gt;_&lt;CRC em 8 hex maiúsculos&gt;.pnach
     * Exemplo: SLUS-20946_B4EC5530.pnach
     */
    public static String buildPnachFileName(String serial, int crc) {
        if (TextUtils.isEmpty(serial) || crc == 0) return null;
        return String.format("%s_%08X.pnach", serial.trim().toUpperCase(), crc);
    }

    /**
     * Também suporta o formato legado: somente CRC em hex.
     * Exemplo: B4EC5530.pnach
     */
    public static String buildLegacyPnachFileName(int crc) {
        if (crc == 0) return null;
        return String.format("%08X.pnach", crc);
    }

    /** Quantidade de jogos conhecidos no banco de dados. */
    public static int getDatabaseSize() {
        return SERIAL_TO_TITLE.size();
    }

    /** Acesso somente leitura ao mapa completo, para listagem. */
    public static Map<String, String> getAllGames() {
        return SERIAL_TO_TITLE;
    }
}

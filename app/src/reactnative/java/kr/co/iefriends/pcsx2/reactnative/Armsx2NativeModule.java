package kr.co.iefriends.pcsx2.reactnative;

import android.app.Activity;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import kr.co.iefriends.pcsx2.NativeApp;
import kr.co.iefriends.pcsx2.utils.CheatDownloader;
import kr.co.iefriends.pcsx2.utils.CheatEntry;
import kr.co.iefriends.pcsx2.utils.CheatManager;
import kr.co.iefriends.pcsx2.utils.DataDirectoryManager;
import kr.co.iefriends.pcsx2.utils.DiscordBridge;
import kr.co.iefriends.pcsx2.utils.RetroAchievementsBridge;

public class Armsx2NativeModule extends ReactContextBaseJavaModule
        implements RetroAchievementsBridge.Listener, DiscordBridge.DiscordStateListener {

    static final String NAME = "Armsx2Bridge";
    private static final String EVENT_RA_STATE = "armsx2.retroAchievements";
    private static final String EVENT_RA_LOGIN = "armsx2.retroAchievementsLogin";
    private static final String EVENT_DISCORD = "armsx2.discord";

    private final ReactApplicationContext reactContext;

    Armsx2NativeModule(ReactApplicationContext context) {
        super(context);
        this.reactContext = context;
        NativeApp.initializeOnce(context.getApplicationContext());
        DiscordBridge.initialize(context);
        RetroAchievementsBridge.setListener(this);
        DiscordBridge.setListener(this);
    }

    @NonNull
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void initialize() {
        super.initialize();
        emitRetroAchievementsState(RetroAchievementsBridge.getLastState());
        emit(EVENT_DISCORD, buildDiscordState());
    }

    @Override
    public void invalidate() {
        RetroAchievementsBridge.setListener(null);
        DiscordBridge.setListener(null);
        super.invalidate();
    }

    private boolean ensureNativeAvailable(Promise promise) {
        if (NativeApp.hasNoNativeBinary) {
            promise.reject("armsx2_native_missing", "Native ARMSX2 binary not bundled in this build.");
            return false;
        }
        return true;
    }

    private void emit(String event, @Nullable WritableMap payload) {
        if (!reactContext.hasActiveReactInstance()) {
            return;
        }
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(event, payload);
    }

    private void emitRetroAchievementsState(@Nullable RetroAchievementsBridge.State state) {
        emit(EVENT_RA_STATE, serializeRetroAchievements(state));
    }

    private WritableMap serializeRetroAchievements(@Nullable RetroAchievementsBridge.State state) {
        WritableMap map = Arguments.createMap();
        if (state == null) {
            map.putBoolean("loggedIn", false);
            return map;
        }
        map.putBoolean("achievementsEnabled", state.achievementsEnabled);
        map.putBoolean("loggedIn", state.loggedIn);
        map.putString("username", state.username);
        map.putString("displayName", state.displayName);
        map.putString("avatarPath", state.avatarPath);
        map.putInt("points", state.points);
        map.putInt("softcorePoints", state.softcorePoints);
        map.putInt("unreadMessages", state.unreadMessages);
        map.putBoolean("hardcorePreference", state.hardcorePreference);
        map.putBoolean("hardcoreActive", state.hardcoreActive);
        map.putBoolean("hasActiveGame", state.hasActiveGame);
        map.putString("gameTitle", state.gameTitle);
        map.putString("richPresence", state.richPresence);
        map.putString("gameIconPath", state.gameIconPath);
        map.putInt("unlockedAchievements", state.unlockedAchievements);
        map.putInt("totalAchievements", state.totalAchievements);
        map.putInt("unlockedPoints", state.unlockedPoints);
        map.putInt("totalPoints", state.totalPoints);
        map.putInt("gameId", state.gameId);
        map.putBoolean("hasLeaderboards", state.hasLeaderboards);
        return map;
    }

    private WritableMap buildDiscordState() {
        WritableMap map = Arguments.createMap();
        map.putBoolean("available", DiscordBridge.isAvailable());
        map.putBoolean("loggedIn", DiscordBridge.isLoggedIn());
        map.putString("username", DiscordBridge.getLoggedInUsername());
        map.putString("avatarUrl", DiscordBridge.getLoggedInAvatarUrl());
        return map;
    }

    // region RetroAchievements
    @ReactMethod
    public void getRetroAchievementsState(Promise promise) {
        promise.resolve(serializeRetroAchievements(RetroAchievementsBridge.getLastState()));
    }

    @ReactMethod
    public void refreshRetroAchievementsState(Promise promise) {
        RetroAchievementsBridge.refreshState();
        promise.resolve(null);
    }

    @ReactMethod
    public void loginRetroAchievements(String username, String password, Promise promise) {
        RetroAchievementsBridge.login(username, password, (success, message) -> {
            WritableMap map = Arguments.createMap();
            map.putBoolean("success", success);
            if (!TextUtils.isEmpty(message)) {
                map.putString("message", message);
            }
            promise.resolve(map);
        });
    }

    @ReactMethod
    public void logoutRetroAchievements(Promise promise) {
        RetroAchievementsBridge.logout();
        promise.resolve(null);
    }

    @ReactMethod
    public void setRetroAchievementsEnabled(boolean enabled, Promise promise) {
        RetroAchievementsBridge.setEnabled(enabled);
        promise.resolve(null);
    }

    @ReactMethod
    public void setRetroAchievementsHardcore(boolean enabled, Promise promise) {
        RetroAchievementsBridge.setHardcore(enabled);
        promise.resolve(null);
    }
    // endregion

    // region Discord
    @ReactMethod
    public void getDiscordProfile(Promise promise) {
        promise.resolve(buildDiscordState());
    }

    @ReactMethod
    public void beginDiscordLogin(Promise promise) {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            promise.reject("armsx2_no_activity", "No activity to start Discord auth");
            return;
        }
        if (!DiscordBridge.isAvailable()) {
            promise.reject("armsx2_discord_unavailable", "Discord SDK not bundled for this build");
            return;
        }
        DiscordBridge.beginAuthorize(activity);
        promise.resolve(true);
    }

    @ReactMethod
    public void logoutDiscord(Promise promise) {
        DiscordBridge.clearTokens();
        promise.resolve(true);
    }
    // endregion

    // region Settings / core
    @ReactMethod
    public void getSetting(String section, String key, String type, Promise promise) {
        if (!ensureNativeAvailable(promise)) return;
        promise.resolve(NativeApp.getSetting(section, key, type));
    }

    @ReactMethod
    public void setSetting(String section, String key, String type, String value, Promise promise) {
        if (!ensureNativeAvailable(promise)) return;
        NativeApp.setSetting(section, key, type, value);
        promise.resolve(true);
    }

    @ReactMethod
    public void refreshBIOS(Promise promise) {
        if (!ensureNativeAvailable(promise)) return;
        NativeApp.refreshBIOS();
        promise.resolve(null);
    }

    @ReactMethod
    public void hasValidVm(Promise promise) {
        if (!ensureNativeAvailable(promise)) return;
        promise.resolve(NativeApp.hasValidVm());
    }

    @ReactMethod
    public void setPadVibration(boolean enabled, Promise promise) {
        if (!ensureNativeAvailable(promise)) return;
        NativeApp.setPadVibration(enabled);
        promise.resolve(null);
    }

    @ReactMethod
    public void convertIsoToChd(String path, Promise promise) {
        if (!ensureNativeAvailable(promise)) return;
        if (!NativeApp.hasNativeTools) {
            promise.reject("armsx2_native_tools_missing", "Native tools library not bundled in this build.");
            return;
        }
        new Thread(() -> {
            int result = NativeApp.convertIsoToChd(path);
            promise.resolve(result);
        }, "ARMSX2-CHD").start();
    }

    @ReactMethod
    public void getDataRoot(Promise promise) {
        promise.resolve(DataDirectoryManager.getDataRoot(reactContext).getAbsolutePath());
    }

    @ReactMethod
    public void setDataRootOverride(String path, Promise promise) {
        NativeApp.setDataRootOverride(path);
        NativeApp.reinitializeDataRoot(path);
        promise.resolve(path);
    }
    // endregion

    // region Cheats
    /**
     * Retorna informações do jogo em execução: serial, crc (hex string) e título.
     */
    @ReactMethod
    public void getGameInfo(Promise promise) {
        if (!ensureNativeAvailable(promise)) return;
        try {
            String serial = NativeApp.getGameSerial();
            int crc = NativeApp.getGameCRC();
            WritableMap map = Arguments.createMap();
            map.putString("serial", serial != null ? serial : "");
            map.putString("crc", String.format("%08X", crc));
            String title = CheatDatabase.getTitleBySerial(serial);
            map.putString("title", title != null ? title : "");
            map.putInt("databaseSize", CheatDatabase.getDatabaseSize());
            promise.resolve(map);
        } catch (Throwable t) {
            promise.reject("armsx2_game_info_error", t.getMessage());
        }
    }

    /**
     * Retorna a lista de cheats do jogo atual como array de objetos {name, codes, enabled}.
     */
    @ReactMethod
    public void getCheats(Promise promise) {
        if (!ensureNativeAvailable(promise)) return;
        try {
            String serial = NativeApp.getGameSerial();
            int crc = NativeApp.getGameCRC();
            java.util.List<CheatEntry> entries = CheatManager.loadCheats(reactContext, serial, crc);
            WritableArray arr = Arguments.createArray();
            for (CheatEntry e : entries) {
                WritableMap m = Arguments.createMap();
                m.putString("name", e.name);
                m.putString("codes", e.codes);
                m.putBoolean("enabled", e.enabled);
                m.putString("author", e.author != null ? e.author : "");
                arr.pushMap(m);
            }
            promise.resolve(arr);
        } catch (Throwable t) {
            promise.reject("armsx2_cheats_error", t.getMessage());
        }
    }

    /**
     * Salva/substitui a lista completa de cheats para o jogo atual e recarrega.
     * Recebe um array de objetos {name, codes, enabled}.
     */
    @ReactMethod
    public void saveCheats(com.facebook.react.bridge.ReadableArray cheatsArray, Promise promise) {
        if (!ensureNativeAvailable(promise)) return;
        try {
            String serial = NativeApp.getGameSerial();
            int crc = NativeApp.getGameCRC();
            java.util.List<CheatEntry> entries = new java.util.ArrayList<>();
            for (int i = 0; i < cheatsArray.size(); i++) {
                com.facebook.react.bridge.ReadableMap m = cheatsArray.getMap(i);
                if (m == null) continue;
                String name = m.hasKey("name") ? m.getString("name") : "Cheat";
                String codes = m.hasKey("codes") ? m.getString("codes") : "";
                boolean enabled = !m.hasKey("enabled") || m.getBoolean("enabled");
                CheatEntry e = new CheatEntry(name, codes, enabled);
                if (m.hasKey("author")) e.author = m.getString("author");
                if (enabled) entries.add(e); // salva apenas habilitados
            }
            boolean ok = CheatManager.saveCheats(reactContext, serial, crc, entries, null);
            if (ok) {
                NativeApp.setEnableCheats(false);
                NativeApp.reloadCheats();
                NativeApp.setEnableCheats(!entries.isEmpty());
            }
            promise.resolve(ok);
        } catch (Throwable t) {
            promise.reject("armsx2_save_cheats_error", t.getMessage());
        }
    }

    /**
     * Recarrega os cheats do disco e reaplica ao emulador.
     */
    @ReactMethod
    public void reloadCheats(Promise promise) {
        if (!ensureNativeAvailable(promise)) return;
        try {
            NativeApp.reloadCheats();
            promise.resolve(true);
        } catch (Throwable t) {
            promise.reject("armsx2_reload_cheats_error", t.getMessage());
        }
    }

    /**
     * Verifica se existem cheats online para o jogo atual.
     */
    @ReactMethod
    public void checkCheatsOnline(Promise promise) {
        if (!ensureNativeAvailable(promise)) return;
        try {
            int crc = NativeApp.getGameCRC();
            if (crc == 0) {
                promise.resolve(false);
                return;
            }
            CheatDownloader.checkAvailability(crc, available -> {
                promise.resolve(available);
            });
        } catch (Throwable t) {
            promise.reject("armsx2_check_cheats_error", t.getMessage());
        }
    }

    /**
     * Baixa os cheats online para o jogo atual.
     */
    @ReactMethod
    public void downloadCheats(Promise promise) {
        if (!ensureNativeAvailable(promise)) return;
        try {
            String serial = NativeApp.getGameSerial();
            int crc = NativeApp.getGameCRC();
            if (crc == 0 || TextUtils.isEmpty(serial)) {
                promise.reject("armsx2_download_cheats_error", "Nenhum jogo em execução ou ID inválido.");
                return;
            }
            CheatDownloader.downloadCheats(reactContext, serial, crc, true, result -> {
                WritableMap map = Arguments.createMap();
                map.putBoolean("success", result.success);
                if (result.success) {
                    map.putInt("cheatCount", result.cheatCount);
                } else {
                    map.putString("error", result.error);
                }
                promise.resolve(map);
            });
        } catch (Throwable t) {
            promise.reject("armsx2_download_cheats_error", t.getMessage());
        }
    }
    // endregion

    // region Bridge callbacks
    @Override
    public void onStateUpdated(RetroAchievementsBridge.State state) {
        emitRetroAchievementsState(state);
    }

    @Override
    public void onLoginRequested(int reason) {
        WritableMap map = Arguments.createMap();
        map.putInt("reason", reason);
        emit(EVENT_RA_LOGIN, map);
    }

    @Override
    public void onLoginSuccess(String username, int points, int softPoints, int unreadMessages) {
        emitRetroAchievementsState(RetroAchievementsBridge.getLastState());
    }

    @Override
    public void onHardcoreModeChanged(boolean enabled) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("hardcoreEnabled", enabled);
        emit(EVENT_RA_STATE, map);
    }

    @Override
    public void onLoginStateChanged(boolean loggedIn) {
        emit(EVENT_DISCORD, buildDiscordState());
    }

    @Override
    public void onError(String message) {
        WritableMap map = buildDiscordState();
        if (!TextUtils.isEmpty(message)) {
            map.putString("error", message);
        }
        emit(EVENT_DISCORD, map);
    }

    @Override
    public void onUserInfoUpdated(String username) {
        emit(EVENT_DISCORD, buildDiscordState());
    }
    // endregion
}

/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamSharedLibraryLoader;
import java.io.PrintStream;

public class SteamAPI {
    private static boolean isRunning = false;
    private static boolean isNativeAPILoaded = false;

    public static void loadLibraries() throws SteamException {
        SteamAPI.loadLibraries(null);
    }

    public static void loadLibraries(String libraryPath) throws SteamException {
        if (isNativeAPILoaded) {
            return;
        }
        if (libraryPath == null && SteamSharedLibraryLoader.DEBUG) {
            String sdkPath = SteamSharedLibraryLoader.getSdkRedistributableBinPath();
            SteamSharedLibraryLoader.loadLibrary("steam_api", sdkPath);
        } else {
            SteamSharedLibraryLoader.loadLibrary("steam_api", libraryPath);
        }
        SteamSharedLibraryLoader.loadLibrary("steamworks4j", libraryPath);
        isNativeAPILoaded = true;
    }

    public static void skipLoadLibraries() {
        isNativeAPILoaded = true;
    }

    public static boolean restartAppIfNecessary(int appId) throws SteamException {
        if (!isNativeAPILoaded) {
            throw new SteamException("Native libraries not loaded.\nEnsure to call SteamAPI.loadLibraries() first!");
        }
        return SteamAPI.nativeRestartAppIfNecessary(appId);
    }

    public static boolean init() throws SteamException {
        if (!isNativeAPILoaded) {
            throw new SteamException("Native libraries not loaded.\nEnsure to call SteamAPI.loadLibraries() first!");
        }
        isRunning = SteamAPI.nativeInit();
        return isRunning;
    }

    public static void shutdown() {
        isRunning = false;
        SteamAPI.nativeShutdown();
    }

    public static boolean isSteamRunning() {
        return SteamAPI.isSteamRunning(false);
    }

    public static boolean isSteamRunning(boolean checkNative) {
        return isRunning && (!checkNative || SteamAPI.isSteamRunningNative());
    }

    public static void printDebugInfo(PrintStream stream) {
        stream.println("  Steam API initialized: " + isRunning);
        stream.println("  Steam client active: " + SteamAPI.isSteamRunning());
    }

    static boolean isIsNativeAPILoaded() {
        return isNativeAPILoaded;
    }

    private static native boolean nativeRestartAppIfNecessary(int var0);

    public static native void releaseCurrentThreadMemory();

    private static native boolean nativeInit();

    private static native void nativeShutdown();

    public static native void runCallbacks();

    private static native boolean isSteamRunningNative();
}


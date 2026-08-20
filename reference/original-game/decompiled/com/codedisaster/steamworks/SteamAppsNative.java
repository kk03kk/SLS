/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

final class SteamAppsNative {
    SteamAppsNative() {
    }

    static native boolean isSubscribed();

    static native boolean isLowViolence();

    static native boolean isCybercafe();

    static native boolean isVACBanned();

    static native String getCurrentGameLanguage();

    static native String getAvailableGameLanguages();

    static native boolean isSubscribedApp(int var0);

    static native boolean isDlcInstalled(int var0);

    static native int getEarliestPurchaseUnixTime(int var0);

    static native boolean isSubscribedFromFreeWeekend();

    static native int getDLCCount();

    static native void installDLC(int var0);

    static native void uninstallDLC(int var0);

    static native long getAppOwner();

    static native int getAppBuildId();
}


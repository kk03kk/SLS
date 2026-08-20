/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamScreenshotsCallbackAdapter;
import java.nio.ByteBuffer;

final class SteamScreenshotsNative {
    SteamScreenshotsNative() {
    }

    static native long createCallback(SteamScreenshotsCallbackAdapter var0);

    static native int writeScreenshot(ByteBuffer var0, int var1, int var2, int var3);

    static native int addScreenshotToLibrary(String var0, String var1, int var2, int var3);

    static native void triggerScreenshot();

    static native void hookScreenshots(boolean var0);

    static native boolean setLocation(int var0, String var1);

    static native boolean tagUser(int var0, long var1);

    static native boolean tagPublishedFile(int var0, long var1);

    static native boolean isScreenshotsHooked();
}


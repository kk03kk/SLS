/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamUserCallbackAdapter;
import java.nio.ByteBuffer;

final class SteamUserNative {
    SteamUserNative() {
    }

    static native long createCallback(SteamUserCallbackAdapter var0);

    static native long getSteamID();

    static native int initiateGameConnection(ByteBuffer var0, int var1, int var2, long var3, int var5, short var6, boolean var7);

    static native void terminateGameConnection(int var0, short var1);

    static native void startVoiceRecording();

    static native void stopVoiceRecording();

    static native int getAvailableVoice(int[] var0);

    static native int getVoice(ByteBuffer var0, int var1, int var2, int[] var3);

    static native int decompressVoice(ByteBuffer var0, int var1, int var2, ByteBuffer var3, int var4, int var5, int[] var6, int var7);

    static native int getVoiceOptimalSampleRate();

    static native int getAuthSessionTicket(ByteBuffer var0, int var1, int var2, int[] var3);

    static native int beginAuthSession(ByteBuffer var0, int var1, int var2, long var3);

    static native void endAuthSession(long var0);

    static native void cancelAuthTicket(int var0);

    static native int userHasLicenseForApp(long var0, int var2);

    static native long requestEncryptedAppTicket(long var0, ByteBuffer var2, int var3, int var4);

    static native boolean getEncryptedAppTicket(ByteBuffer var0, int var1, int var2, int[] var3);

    static native boolean isBehindNAT();

    static native void advertiseGame(long var0, int var2, short var3);
}


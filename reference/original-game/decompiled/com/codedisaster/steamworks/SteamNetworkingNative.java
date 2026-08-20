/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamNetworking;
import com.codedisaster.steamworks.SteamNetworkingCallbackAdapter;
import java.nio.ByteBuffer;

final class SteamNetworkingNative {
    SteamNetworkingNative() {
    }

    static native long createCallback(SteamNetworkingCallbackAdapter var0);

    static native boolean sendP2PPacket(boolean var0, long var1, ByteBuffer var3, int var4, int var5, int var6, int var7);

    static native boolean isP2PPacketAvailable(boolean var0, int[] var1, int var2);

    static native boolean readP2PPacket(boolean var0, ByteBuffer var1, int var2, int var3, int[] var4, long[] var5, int var6);

    static native boolean acceptP2PSessionWithUser(boolean var0, long var1);

    static native boolean closeP2PSessionWithUser(boolean var0, long var1);

    static native boolean closeP2PChannelWithUser(boolean var0, long var1, int var3);

    static native boolean getP2PSessionState(boolean var0, long var1, SteamNetworking.P2PSessionState var3);

    static native boolean allowP2PPacketRelay(boolean var0, boolean var1);
}


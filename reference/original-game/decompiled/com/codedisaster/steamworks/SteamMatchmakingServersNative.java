/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamMatchmakingGameServerItem;
import com.codedisaster.steamworks.SteamMatchmakingKeyValuePair;

final class SteamMatchmakingServersNative {
    SteamMatchmakingServersNative() {
    }

    static native long requestInternetServerList(int var0, SteamMatchmakingKeyValuePair[] var1, int var2, long var3);

    static native long requestLANServerList(int var0, long var1);

    static native long requestFriendsServerList(int var0, SteamMatchmakingKeyValuePair[] var1, int var2, long var3);

    static native long requestFavoritesServerList(int var0, SteamMatchmakingKeyValuePair[] var1, int var2, long var3);

    static native long requestHistoryServerList(int var0, SteamMatchmakingKeyValuePair[] var1, int var2, long var3);

    static native long requestSpectatorServerList(int var0, SteamMatchmakingKeyValuePair[] var1, int var2, long var3);

    static native void releaseRequest(long var0);

    static native boolean getServerDetails(long var0, int var2, SteamMatchmakingGameServerItem var3);

    static native void cancelQuery(long var0);

    static native void refreshQuery(long var0);

    static native boolean isRefreshing(long var0);

    static native int getServerCount(long var0);

    static native void refreshServer(long var0, int var2);

    static native int pingServer(int var0, short var1, long var2);

    static native int playerDetails(int var0, short var1, long var2);

    static native int serverRules(int var0, short var1, long var2);

    static native void cancelServerQuery(int var0);
}


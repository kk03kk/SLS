/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamMatchmaking;
import com.codedisaster.steamworks.SteamMatchmakingCallbackAdapter;
import com.codedisaster.steamworks.SteamMatchmakingKeyValuePair;
import java.nio.ByteBuffer;

final class SteamMatchmakingNative {
    SteamMatchmakingNative() {
    }

    static native long createCallback(SteamMatchmakingCallbackAdapter var0);

    static native int getFavoriteGameCount();

    static native boolean getFavoriteGame(int var0, int[] var1, int[] var2, short[] var3, short[] var4, int[] var5, int[] var6);

    static native int addFavoriteGame(int var0, int var1, short var2, short var3, int var4, int var5);

    static native boolean removeFavoriteGame(int var0, int var1, short var2, short var3, int var4);

    static native long requestLobbyList(long var0);

    static native void addRequestLobbyListStringFilter(String var0, String var1, int var2);

    static native void addRequestLobbyListNumericalFilter(String var0, int var1, int var2);

    static native void addRequestLobbyListNearValueFilter(String var0, int var1);

    static native void addRequestLobbyListFilterSlotsAvailable(int var0);

    static native void addRequestLobbyListDistanceFilter(int var0);

    static native void addRequestLobbyListResultCountFilter(int var0);

    static native void addRequestLobbyListCompatibleMembersFilter(long var0);

    static native long getLobbyByIndex(int var0);

    static native long createLobby(long var0, int var2, int var3);

    static native long joinLobby(long var0, long var2);

    static native void leaveLobby(long var0);

    static native boolean inviteUserToLobby(long var0, long var2);

    static native int getNumLobbyMembers(long var0);

    static native long getLobbyMemberByIndex(long var0, int var2);

    static native String getLobbyData(long var0, String var2);

    static native boolean setLobbyData(long var0, String var2, String var3);

    static native String getLobbyMemberData(long var0, long var2, String var4);

    static native void setLobbyMemberData(long var0, String var2, String var3);

    static native int getLobbyDataCount(long var0);

    static native boolean getLobbyDataByIndex(long var0, int var2, SteamMatchmakingKeyValuePair var3);

    static native boolean deleteLobbyData(long var0, String var2);

    static native boolean sendLobbyChatMsg(long var0, ByteBuffer var2, int var3, int var4);

    static native boolean sendLobbyChatMsg(long var0, String var2);

    static native int getLobbyChatEntry(long var0, int var2, SteamMatchmaking.ChatEntry var3, ByteBuffer var4, int var5, int var6);

    static native boolean requestLobbyData(long var0);

    static native void setLobbyGameServer(long var0, int var2, short var3, long var4);

    static native boolean getLobbyGameServer(long var0, int[] var2, short[] var3, long[] var4);

    static native boolean setLobbyMemberLimit(long var0, int var2);

    static native int getLobbyMemberLimit(long var0);

    static native boolean setLobbyType(long var0, int var2);

    static native boolean setLobbyJoinable(long var0, boolean var2);

    static native long getLobbyOwner(long var0);

    static native boolean setLobbyOwner(long var0, long var2);

    static native boolean setLinkedLobby(long var0, long var2);
}


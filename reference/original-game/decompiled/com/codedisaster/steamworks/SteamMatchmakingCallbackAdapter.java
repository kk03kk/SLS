/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamCallbackAdapter;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamMatchmaking;
import com.codedisaster.steamworks.SteamMatchmakingCallback;
import com.codedisaster.steamworks.SteamResult;

class SteamMatchmakingCallbackAdapter
extends SteamCallbackAdapter<SteamMatchmakingCallback> {
    private static final SteamMatchmaking.ChatMemberStateChange[] stateChangeValues = SteamMatchmaking.ChatMemberStateChange.values();

    SteamMatchmakingCallbackAdapter(SteamMatchmakingCallback callback) {
        super(callback);
    }

    void onFavoritesListChanged(int ip, int queryPort, int connPort, int appID, int flags, boolean add, int accountID) {
        ((SteamMatchmakingCallback)this.callback).onFavoritesListChanged(ip, queryPort, connPort, appID, flags, add, accountID);
    }

    void onLobbyInvite(long steamIDUser, long steamIDLobby, long gameID) {
        ((SteamMatchmakingCallback)this.callback).onLobbyInvite(new SteamID(steamIDUser), new SteamID(steamIDLobby), gameID);
    }

    void onLobbyEnter(long steamIDLobby, int chatPermissions, boolean blocked, int response) {
        ((SteamMatchmakingCallback)this.callback).onLobbyEnter(new SteamID(steamIDLobby), chatPermissions, blocked, SteamMatchmaking.ChatRoomEnterResponse.byValue(response));
    }

    void onLobbyDataUpdate(long steamIDLobby, long steamIDMember, boolean success) {
        ((SteamMatchmakingCallback)this.callback).onLobbyDataUpdate(new SteamID(steamIDLobby), new SteamID(steamIDMember), success);
    }

    void onLobbyChatUpdate(long steamIDLobby, long steamIDUserChanged, long steamIDMakingChange, int stateChange) {
        SteamID lobby = new SteamID(steamIDLobby);
        SteamID userChanged = new SteamID(steamIDUserChanged);
        SteamID makingChange = new SteamID(steamIDMakingChange);
        for (SteamMatchmaking.ChatMemberStateChange value : stateChangeValues) {
            if (!SteamMatchmaking.ChatMemberStateChange.isSet(value, stateChange)) continue;
            ((SteamMatchmakingCallback)this.callback).onLobbyChatUpdate(lobby, userChanged, makingChange, value);
        }
    }

    void onLobbyChatMessage(long steamIDLobby, long steamIDUser, int entryType, int chatID) {
        ((SteamMatchmakingCallback)this.callback).onLobbyChatMessage(new SteamID(steamIDLobby), new SteamID(steamIDUser), SteamMatchmaking.ChatEntryType.byValue(entryType), chatID);
    }

    void onLobbyGameCreated(long steamIDLobby, long steamIDGameServer, int ip, short port) {
        ((SteamMatchmakingCallback)this.callback).onLobbyGameCreated(new SteamID(steamIDLobby), new SteamID(steamIDGameServer), ip, port);
    }

    void onLobbyMatchList(int lobbiesMatching) {
        ((SteamMatchmakingCallback)this.callback).onLobbyMatchList(lobbiesMatching);
    }

    void onLobbyKicked(long steamIDLobby, long steamIDAdmin, boolean kickedDueToDisconnect) {
        ((SteamMatchmakingCallback)this.callback).onLobbyKicked(new SteamID(steamIDLobby), new SteamID(steamIDAdmin), kickedDueToDisconnect);
    }

    void onLobbyCreated(int result, long steamIDLobby) {
        ((SteamMatchmakingCallback)this.callback).onLobbyCreated(SteamResult.byValue(result), new SteamID(steamIDLobby));
    }

    void onFavoritesListAccountsUpdated(int result) {
        ((SteamMatchmakingCallback)this.callback).onFavoritesListAccountsUpdated(SteamResult.byValue(result));
    }
}


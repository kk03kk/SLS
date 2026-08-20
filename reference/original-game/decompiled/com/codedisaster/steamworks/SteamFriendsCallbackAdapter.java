/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamCallbackAdapter;
import com.codedisaster.steamworks.SteamFriends;
import com.codedisaster.steamworks.SteamFriendsCallback;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamResult;

class SteamFriendsCallbackAdapter
extends SteamCallbackAdapter<SteamFriendsCallback> {
    private static final SteamFriends.PersonaChange[] personaChangeValues = SteamFriends.PersonaChange.values();

    SteamFriendsCallbackAdapter(SteamFriendsCallback callback) {
        super(callback);
    }

    void onSetPersonaNameResponse(boolean success, boolean localSuccess, int result) {
        ((SteamFriendsCallback)this.callback).onSetPersonaNameResponse(success, localSuccess, SteamResult.byValue(result));
    }

    void onPersonaStateChange(long steamID, int changeFlags) {
        SteamID id = new SteamID(steamID);
        for (SteamFriends.PersonaChange value : personaChangeValues) {
            if (!SteamFriends.PersonaChange.isSet(value, changeFlags)) continue;
            ((SteamFriendsCallback)this.callback).onPersonaStateChange(id, value);
        }
    }

    void onGameOverlayActivated(boolean active) {
        ((SteamFriendsCallback)this.callback).onGameOverlayActivated(active);
    }

    void onGameLobbyJoinRequested(long steamIDLobby, long steamIDFriend) {
        ((SteamFriendsCallback)this.callback).onGameLobbyJoinRequested(new SteamID(steamIDLobby), new SteamID(steamIDFriend));
    }

    void onAvatarImageLoaded(long steamID, int image, int width, int height) {
        ((SteamFriendsCallback)this.callback).onAvatarImageLoaded(new SteamID(steamID), image, width, height);
    }

    void onFriendRichPresenceUpdate(long steamIDFriend, int appID) {
        ((SteamFriendsCallback)this.callback).onFriendRichPresenceUpdate(new SteamID(steamIDFriend), appID);
    }

    void onGameRichPresenceJoinRequested(long steamIDFriend, String connect) {
        ((SteamFriendsCallback)this.callback).onGameRichPresenceJoinRequested(new SteamID(steamIDFriend), connect);
    }

    void onGameServerChangeRequested(String server, String password) {
        ((SteamFriendsCallback)this.callback).onGameServerChangeRequested(server, password);
    }
}


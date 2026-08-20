/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.integrations.steam;

import com.codedisaster.steamworks.SteamFriends;
import com.codedisaster.steamworks.SteamFriendsCallback;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamResult;

public class SFCallback
implements SteamFriendsCallback {
    @Override
    public void onSetPersonaNameResponse(boolean success, boolean localSuccess, SteamResult result) {
    }

    @Override
    public void onPersonaStateChange(SteamID steamID, SteamFriends.PersonaChange change) {
    }

    @Override
    public void onGameOverlayActivated(boolean active) {
    }

    @Override
    public void onGameLobbyJoinRequested(SteamID steamIDLobby, SteamID steamIDFriend) {
    }

    @Override
    public void onAvatarImageLoaded(SteamID steamID, int image, int width, int height) {
    }

    @Override
    public void onFriendRichPresenceUpdate(SteamID steamIDFriend, int appID) {
    }

    @Override
    public void onGameRichPresenceJoinRequested(SteamID steamIDFriend, String connect) {
    }

    @Override
    public void onGameServerChangeRequested(String server, String password) {
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamFriends;
import com.codedisaster.steamworks.SteamFriendsCallbackAdapter;

final class SteamFriendsNative {
    SteamFriendsNative() {
    }

    static native long createCallback(SteamFriendsCallbackAdapter var0);

    static native String getPersonaName();

    static native long setPersonaName(long var0, String var2);

    static native int getPersonaState();

    static native int getFriendCount(int var0);

    static native long getFriendByIndex(int var0, int var1);

    static native int getFriendRelationship(long var0);

    static native int getFriendPersonaState(long var0);

    static native String getFriendPersonaName(long var0);

    static native boolean getFriendGamePlayed(long var0, SteamFriends.FriendGameInfo var2);

    static native void setInGameVoiceSpeaking(long var0, boolean var2);

    static native int getSmallFriendAvatar(long var0);

    static native int getMediumFriendAvatar(long var0);

    static native int getLargeFriendAvatar(long var0);

    static native boolean requestUserInformation(long var0, boolean var2);

    static native void activateGameOverlay(String var0);

    static native void activateGameOverlayToUser(String var0, long var1);

    static native void activateGameOverlayToWebPage(String var0, int var1);

    static native void activateGameOverlayToStore(int var0, int var1);

    static native void setPlayedWith(long var0);

    static native void activateGameOverlayInviteDialog(long var0);

    static native boolean setRichPresence(String var0, String var1);

    static native void clearRichPresence();

    static native String getFriendRichPresence(long var0, String var2);

    static native int getFriendRichPresenceKeyCount(long var0);

    static native String getFriendRichPresenceKeyByIndex(long var0, int var2);

    static native void requestFriendRichPresence(long var0);

    static native boolean inviteUserToGame(long var0, String var2);

    static native int getCoplayFriendCount();

    static native long getCoplayFriend(int var0);

    static native int getFriendCoplayTime(long var0);

    static native int getFriendCoplayGame(long var0);
}


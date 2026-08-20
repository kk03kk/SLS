/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamCallbackAdapter;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamLeaderboardEntriesHandle;
import com.codedisaster.steamworks.SteamLeaderboardHandle;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUserStatsCallback;

class SteamUserStatsCallbackAdapter
extends SteamCallbackAdapter<SteamUserStatsCallback> {
    SteamUserStatsCallbackAdapter(SteamUserStatsCallback callback) {
        super(callback);
    }

    void onUserStatsReceived(long gameId, long steamIDUser, int result) {
        ((SteamUserStatsCallback)this.callback).onUserStatsReceived(gameId, new SteamID(steamIDUser), SteamResult.byValue(result));
    }

    void onUserStatsStored(long gameId, int result) {
        ((SteamUserStatsCallback)this.callback).onUserStatsStored(gameId, SteamResult.byValue(result));
    }

    void onUserStatsUnloaded(long steamIDUser) {
        ((SteamUserStatsCallback)this.callback).onUserStatsUnloaded(new SteamID(steamIDUser));
    }

    void onUserAchievementStored(long gameId, boolean isGroupAchievement, String achievementName, int curProgress, int maxProgress) {
        ((SteamUserStatsCallback)this.callback).onUserAchievementStored(gameId, isGroupAchievement, achievementName, curProgress, maxProgress);
    }

    void onLeaderboardFindResult(long handle, boolean found) {
        ((SteamUserStatsCallback)this.callback).onLeaderboardFindResult(new SteamLeaderboardHandle(handle), found);
    }

    void onLeaderboardScoresDownloaded(long handle, long entries, int numEntries) {
        ((SteamUserStatsCallback)this.callback).onLeaderboardScoresDownloaded(new SteamLeaderboardHandle(handle), new SteamLeaderboardEntriesHandle(entries), numEntries);
    }

    void onLeaderboardScoreUploaded(boolean success, long handle, int score, boolean changed, int globalRankNew, int globalRankPrevious) {
        ((SteamUserStatsCallback)this.callback).onLeaderboardScoreUploaded(success, new SteamLeaderboardHandle(handle), score, changed, globalRankNew, globalRankPrevious);
    }

    void onNumberOfCurrentPlayersReceived(boolean success, int players) {
        ((SteamUserStatsCallback)this.callback).onNumberOfCurrentPlayersReceived(success, players);
    }

    void onGlobalStatsReceived(long gameId, int result) {
        ((SteamUserStatsCallback)this.callback).onGlobalStatsReceived(gameId, SteamResult.byValue(result));
    }
}


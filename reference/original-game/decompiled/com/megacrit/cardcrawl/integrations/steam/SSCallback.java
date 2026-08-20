/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.integrations.steam;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamLeaderboardEntriesHandle;
import com.codedisaster.steamworks.SteamLeaderboardEntry;
import com.codedisaster.steamworks.SteamLeaderboardHandle;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUserStatsCallback;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.integrations.steam.SteamIntegration;
import com.megacrit.cardcrawl.screens.leaderboards.LeaderboardEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SSCallback
implements SteamUserStatsCallback {
    private static final Logger logger = LogManager.getLogger(SSCallback.class.getName());
    private SteamIntegration steamIntegration;

    public SSCallback(SteamIntegration steamIntegration) {
        this.steamIntegration = steamIntegration;
    }

    @Override
    public void onGlobalStatsReceived(long arg0, SteamResult arg1) {
        logger.info("1Bloop: " + arg0);
    }

    @Override
    public void onLeaderboardFindResult(SteamLeaderboardHandle handle, boolean found) {
        logger.info("onLeaderboardFindResult");
        if (found) {
            switch (SteamIntegration.task) {
                case UPLOAD_DAILY: {
                    SteamIntegration.lbHandle = handle;
                    SteamIntegration.uploadDailyLeaderboardHelper();
                    break;
                }
                case UPLOAD: {
                    SteamIntegration.lbHandle = handle;
                    SteamIntegration.uploadLeaderboardHelper();
                    break;
                }
                case RETRIEVE_DAILY: {
                    SteamIntegration.lbHandle = handle;
                    SteamIntegration.getLeaderboardEntryHelper();
                    break;
                }
                case RETRIEVE: {
                    SteamIntegration.lbHandle = handle;
                    SteamIntegration.getLeaderboardEntryHelper();
                    break;
                }
            }
        }
    }

    @Override
    public void onLeaderboardScoreUploaded(boolean success, SteamLeaderboardHandle handle, int score, boolean changed, int globalRankNew, int globalRankPrevious) {
        if (!success) {
            logger.info("Failed to upload leaderboard data: " + score);
        } else if (!changed) {
            logger.info("Leaderboard data not changed for data: " + score);
        } else {
            logger.info("Successfully uploaded leaderboard data: " + score);
        }
        this.steamIntegration.didCompleteCallback(success);
    }

    @Override
    public void onNumberOfCurrentPlayersReceived(boolean success, int players) {
    }

    @Override
    public void onLeaderboardScoresDownloaded(SteamLeaderboardHandle handle, SteamLeaderboardEntriesHandle entries, int numEntries) {
        if (SteamIntegration.task == SteamIntegration.LeaderboardTask.RETRIEVE) {
            logger.info("Downloaded " + numEntries + " entries");
            int[] details = new int[16];
            CardCrawlGame.mainMenuScreen.leaderboardsScreen.entries.clear();
            for (int i = 0; i < numEntries; ++i) {
                SteamLeaderboardEntry entry = new SteamLeaderboardEntry();
                if (SteamIntegration.steamStats.getDownloadedLeaderboardEntry(entries, i, entry, details)) {
                    int rTemp = entry.getGlobalRank();
                    if (i == 0) {
                        CardCrawlGame.mainMenuScreen.leaderboardsScreen.currentStartIndex = rTemp;
                    } else if (i == numEntries) {
                        CardCrawlGame.mainMenuScreen.leaderboardsScreen.currentEndIndex = rTemp;
                    }
                    CardCrawlGame.mainMenuScreen.leaderboardsScreen.entries.add(new LeaderboardEntry(rTemp, SteamIntegration.steamFriends.getFriendPersonaName(entry.getSteamIDUser()), entry.getScore(), SteamIntegration.gettingTime, SteamIntegration.accountId != -1 && SteamIntegration.accountId == entry.getSteamIDUser().getAccountID()));
                    continue;
                }
                logger.info("FAILED TO GET LEADERBOARD ENTRY: " + i);
            }
            CardCrawlGame.mainMenuScreen.leaderboardsScreen.waiting = false;
        } else if (SteamIntegration.task == SteamIntegration.LeaderboardTask.RETRIEVE_DAILY) {
            logger.info("[DAILY] Downloaded " + numEntries + " entries");
            int[] details = new int[16];
            CardCrawlGame.mainMenuScreen.dailyScreen.entries.clear();
            for (int i = 0; i < numEntries; ++i) {
                SteamLeaderboardEntry entry = new SteamLeaderboardEntry();
                if (SteamIntegration.steamStats.getDownloadedLeaderboardEntry(entries, i, entry, details)) {
                    int rTemp = entry.getGlobalRank();
                    if (i == 0) {
                        CardCrawlGame.mainMenuScreen.dailyScreen.currentStartIndex = rTemp;
                    } else if (i == numEntries) {
                        CardCrawlGame.mainMenuScreen.dailyScreen.currentEndIndex = rTemp;
                    }
                    CardCrawlGame.mainMenuScreen.dailyScreen.entries.add(new LeaderboardEntry(rTemp, SteamIntegration.steamFriends.getFriendPersonaName(entry.getSteamIDUser()), entry.getScore(), SteamIntegration.gettingTime, SteamIntegration.accountId != -1 && SteamIntegration.accountId == entry.getSteamIDUser().getAccountID()));
                    continue;
                }
                logger.info("FAILED TO GET LEADERBOARD ENTRY: " + i);
            }
            CardCrawlGame.mainMenuScreen.dailyScreen.waiting = false;
        }
    }

    @Override
    public void onUserAchievementStored(long arg0, boolean arg1, String arg2, int arg3, int arg4) {
        logger.info("Achievement Stored");
    }

    @Override
    public void onUserStatsReceived(long arg0, SteamID arg1, SteamResult arg2) {
        logger.info("SteamID: " + arg1.getAccountID());
        logger.info("APPID: " + arg0);
    }

    @Override
    public void onUserStatsStored(long arg0, SteamResult arg1) {
        logger.info("Stat Stored");
    }

    @Override
    public void onUserStatsUnloaded(SteamID arg0) {
    }
}


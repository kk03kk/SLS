/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.integrations.steam;

import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamApps;
import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamFriends;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamLeaderboardHandle;
import com.codedisaster.steamworks.SteamRemoteStorage;
import com.codedisaster.steamworks.SteamUser;
import com.codedisaster.steamworks.SteamUserStats;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.daily.TimeHelper;
import com.megacrit.cardcrawl.integrations.DistributorFactory;
import com.megacrit.cardcrawl.integrations.PublisherIntegration;
import com.megacrit.cardcrawl.integrations.steam.SFCallback;
import com.megacrit.cardcrawl.integrations.steam.SRCallback;
import com.megacrit.cardcrawl.integrations.steam.SSCallback;
import com.megacrit.cardcrawl.integrations.steam.SUCallback;
import com.megacrit.cardcrawl.integrations.steam.SteamTicker;
import com.megacrit.cardcrawl.screens.leaderboards.FilterButton;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SteamIntegration
implements PublisherIntegration {
    private static final Logger logger = LogManager.getLogger(SteamIntegration.class.getName());
    private static String[] TEXT = null;
    static SteamUserStats steamStats;
    private static SteamUser steamUser;
    private static SteamApps steamApps;
    static SteamFriends steamFriends;
    private static Thread thread;
    static int accountId;
    static SteamLeaderboardHandle lbHandle;
    static LeaderboardTask task;
    private static boolean retrieveGlobal;
    static boolean gettingTime;
    private static int lbScore;
    private static int startIndex;
    private static int endIndex;
    private static boolean isUploadingScore;
    private static Queue<StatTuple> statsToUpload;

    public SteamIntegration() {
        if (!Settings.isDev || Settings.isModded) {
            try {
                SteamAPI.loadLibraries();
                if (SteamAPI.init()) {
                    logger.info("[SUCCESS] Steam API initialized successfully.");
                    steamStats = new SteamUserStats(new SSCallback(this));
                    steamUser = new SteamUser(new SUCallback());
                    steamApps = new SteamApps();
                    steamFriends = new SteamFriends(new SFCallback());
                    logger.info("BUILD ID: " + steamApps.getAppBuildId());
                    logger.info("CURRENT LANG: " + steamApps.getCurrentGameLanguage());
                    SteamID id = steamApps.getAppOwner();
                    accountId = id.getAccountID();
                    logger.info("ACCOUNT ID: " + accountId);
                    thread = new Thread(new SteamTicker());
                    thread.setName("SteamTicker");
                    thread.start();
                } else {
                    logger.info("[FAILURE] Steam API failed to initialize correctly.");
                }
            }
            catch (SteamException e) {
                e.printStackTrace();
            }
        }
        if (SteamAPI.isSteamRunning()) {
            SteamIntegration.requestGlobalStats(365);
        }
    }

    @Override
    public boolean isInitialized() {
        return steamUser != null && steamStats != null;
    }

    public ArrayList<String> getAllCloudFiles() {
        SteamRemoteStorage remoteStorage = new SteamRemoteStorage(new SRCallback());
        int numFiles = remoteStorage.getFileCount();
        logger.info("Num of files: " + numFiles);
        ArrayList<String> files = new ArrayList<String>();
        for (int i = 0; i < numFiles; ++i) {
            int[] sizes = new int[1];
            String file = remoteStorage.getFileNameAndSize(i, sizes);
            boolean exists = remoteStorage.fileExists(file);
            if (exists) {
                files.add(file);
            }
            logger.info("# " + i + " : name=" + file + ", size=" + sizes[0] + ", exists=" + (exists ? "yes" : "no"));
        }
        remoteStorage.dispose();
        return files;
    }

    @Override
    public void deleteAllCloudFiles() {
        this.deleteCloudFiles(this.getAllCloudFiles());
        logger.info("Deleted all Cloud Files");
    }

    private void deleteCloudFiles(ArrayList<String> files) {
        SteamRemoteStorage remoteStorage = new SteamRemoteStorage(new SRCallback());
        for (String file : files) {
            logger.info("Deleting file: " + file);
            remoteStorage.fileDelete(file);
        }
        remoteStorage.dispose();
    }

    public static String basename(String path) {
        Path p = Paths.get(path, new String[0]);
        return p.getFileName().toString();
    }

    @Override
    public void unlockAchievement(String id) {
        logger.info("unlockAchievement: " + id);
        if (steamStats != null) {
            if (steamStats.setAchievement(id)) {
                steamStats.storeStats();
            } else {
                logger.info("[ERROR] Could not find achievement " + id);
            }
        }
    }

    public static void removeAllAchievementsBeCarefulNotToPush() {
        if (Settings.isDev && Settings.isBeta && steamStats != null && steamStats.resetAllStats(true)) {
            steamStats.storeStats();
        }
    }

    @Override
    public boolean incrementStat(String id, int incrementAmt) {
        logger.info("incrementStat: " + id);
        if (steamStats != null) {
            if (steamStats.setStatI(id, this.getStat(id) + incrementAmt)) {
                return true;
            }
            logger.info("Stat: " + id + " not found.");
            return false;
        }
        logger.info("[ERROR] Could not find stat " + id);
        return false;
    }

    @Override
    public int getStat(String id) {
        logger.info("getStat: " + id);
        if (steamStats != null) {
            return steamStats.getStatI(id, 0);
        }
        return -1;
    }

    @Override
    public boolean setStat(String id, int value) {
        logger.info("setStat: " + id);
        if (steamStats != null) {
            if (steamStats.setStatI(id, value)) {
                logger.info(id + " stat set to " + value);
                return true;
            }
            logger.info("Stat: " + id + " not found.");
            return false;
        }
        logger.info("[ERROR] Could not find stat " + id);
        return false;
    }

    @Override
    public long getGlobalStat(String id) {
        logger.info("getGlobalStat");
        if (steamStats != null) {
            return steamStats.getGlobalStat(id, 0L);
        }
        return -1L;
    }

    private static void requestGlobalStats(int i) {
        logger.info("requestGlobalStats");
        if (steamStats != null) {
            steamStats.requestGlobalStats(i);
        }
    }

    @Override
    public void getLeaderboardEntries(AbstractPlayer.PlayerClass pClass, FilterButton.RegionSetting rSetting, FilterButton.LeaderboardType lType, int start, int end) {
        task = LeaderboardTask.RETRIEVE;
        startIndex = start;
        endIndex = end;
        gettingTime = lType == FilterButton.LeaderboardType.FASTEST_WIN;
        retrieveGlobal = rSetting == FilterButton.RegionSetting.GLOBAL;
        if (steamStats != null) {
            steamStats.findLeaderboard(SteamIntegration.createGetLeaderboardString(pClass, lType));
        }
    }

    @Override
    public void getDailyLeaderboard(long date, int start, int end) {
        task = LeaderboardTask.RETRIEVE_DAILY;
        startIndex = start;
        endIndex = end;
        retrieveGlobal = true;
        gettingTime = false;
        if (steamStats != null) {
            StringBuilder leaderboardRetrieveString = new StringBuilder("DAILY_");
            leaderboardRetrieveString.append(Long.toString(date));
            if (Settings.isBeta) {
                leaderboardRetrieveString.append("_BETA");
            }
            steamStats.findOrCreateLeaderboard(leaderboardRetrieveString.toString(), SteamUserStats.LeaderboardSortMethod.Descending, SteamUserStats.LeaderboardDisplayType.Numeric);
        }
    }

    private static String createGetLeaderboardString(AbstractPlayer.PlayerClass pClass, FilterButton.LeaderboardType lType) {
        String retVal = "";
        switch (pClass) {
            case IRONCLAD: {
                retVal = retVal + "IRONCLAD";
                break;
            }
            case THE_SILENT: {
                retVal = retVal + "SILENT";
                break;
            }
            case DEFECT: {
                retVal = retVal + "DEFECT";
                break;
            }
            case WATCHER: {
                retVal = retVal + "WATCHER";
                break;
            }
        }
        switch (lType) {
            case AVG_FLOOR: {
                retVal = retVal + "_AVG_FLOOR";
                break;
            }
            case AVG_SCORE: {
                retVal = retVal + "_AVG_SCORE";
                break;
            }
            case CONSECUTIVE_WINS: {
                retVal = retVal + "_CONSECUTIVE_WINS";
                break;
            }
            case FASTEST_WIN: {
                retVal = retVal + "_FASTEST_WIN";
                break;
            }
            case HIGH_SCORE: {
                retVal = retVal + "_HIGH_SCORE";
                break;
            }
            case SPIRE_LEVEL: {
                retVal = retVal + "_SPIRE_LEVEL";
                break;
            }
        }
        if (Settings.isBeta) {
            retVal = retVal + "_BETA";
        }
        return retVal;
    }

    @Override
    public void uploadLeaderboardScore(String name, int score) {
        if (steamUser == null || steamStats == null) {
            return;
        }
        if (isUploadingScore) {
            statsToUpload.add(new StatTuple(name, score));
        } else {
            logger.info(String.format("Uploading Steam Leaderboard score (%s: %d)", name, score));
            isUploadingScore = true;
            task = LeaderboardTask.UPLOAD;
            lbScore = score;
            steamStats.findLeaderboard(name);
        }
    }

    @Override
    public void uploadDailyLeaderboardScore(String name, int score) {
        if (!TimeHelper.isOfflineMode()) {
            if (steamUser == null || steamStats == null) {
                logger.info("User is NOT connected to Steam, unable to upload daily score.");
                return;
            }
            if (isUploadingScore) {
                statsToUpload.add(new StatTuple(name, score));
            } else {
                logger.info(String.format("Uploading [DAILY] Steam Leaderboard score (%s: %d)", name, score));
                isUploadingScore = true;
                task = LeaderboardTask.UPLOAD_DAILY;
                lbScore = score;
                steamStats.findOrCreateLeaderboard(name, SteamUserStats.LeaderboardSortMethod.Descending, SteamUserStats.LeaderboardDisplayType.Numeric);
            }
        }
    }

    void didCompleteCallback(boolean success) {
        logger.info("didCompleteCallback");
        isUploadingScore = false;
        if (statsToUpload.size() > 0) {
            StatTuple uploadMe = statsToUpload.remove();
            this.uploadLeaderboardScore(uploadMe.stat, uploadMe.score);
        }
    }

    static void uploadLeaderboardHelper() {
        logger.info("uploadLeaderboardHelper");
        steamStats.uploadLeaderboardScore(lbHandle, SteamUserStats.LeaderboardUploadScoreMethod.KeepBest, lbScore, new int[0]);
    }

    static void uploadDailyLeaderboardHelper() {
        logger.info("uploadDailyLeaderboardHelper");
        steamStats.uploadLeaderboardScore(lbHandle, SteamUserStats.LeaderboardUploadScoreMethod.KeepBest, lbScore, new int[0]);
    }

    static void getLeaderboardEntryHelper() {
        if (task == LeaderboardTask.RETRIEVE) {
            if (retrieveGlobal) {
                logger.info("Downloading GLOBAL entries: " + startIndex + " - " + endIndex);
                if (CardCrawlGame.mainMenuScreen.leaderboardsScreen.viewMyScore) {
                    steamStats.downloadLeaderboardEntries(lbHandle, SteamUserStats.LeaderboardDataRequest.GlobalAroundUser, -9, 10);
                    CardCrawlGame.mainMenuScreen.leaderboardsScreen.viewMyScore = false;
                } else {
                    steamStats.downloadLeaderboardEntries(lbHandle, SteamUserStats.LeaderboardDataRequest.Global, startIndex, endIndex);
                }
            } else {
                logger.info("Downloading FRIEND entries: " + startIndex + " - " + endIndex);
                steamStats.downloadLeaderboardEntries(lbHandle, SteamUserStats.LeaderboardDataRequest.Friends, startIndex, endIndex);
            }
        } else if (task == LeaderboardTask.RETRIEVE_DAILY) {
            if (CardCrawlGame.mainMenuScreen.dailyScreen.viewMyScore) {
                steamStats.downloadLeaderboardEntries(lbHandle, SteamUserStats.LeaderboardDataRequest.GlobalAroundUser, -9, 10);
                CardCrawlGame.mainMenuScreen.dailyScreen.viewMyScore = false;
            } else {
                logger.info("Downloading GLOBAL entries: " + startIndex + " - " + endIndex);
                steamStats.downloadLeaderboardEntries(lbHandle, SteamUserStats.LeaderboardDataRequest.Global, startIndex, endIndex);
            }
        }
    }

    @Override
    public void setRichPresenceDisplayPlaying(int floor, int ascension, String character) {
        if (TEXT == null) {
            TEXT = CardCrawlGame.languagePack.getUIString((String)"RichPresence").TEXT;
        }
        if (Settings.isDailyRun) {
            String msg = String.format(TEXT[0], floor);
            logger.debug("Setting Rich Presence: " + msg);
            this.setRichPresenceData("status", msg);
        } else if (Settings.isTrial) {
            String msg = String.format(TEXT[1], floor);
            logger.debug("Setting Rich Presence: " + msg);
            this.setRichPresenceData("status", msg);
        } else if (Settings.language == Settings.GameLanguage.ENG || Settings.language == Settings.GameLanguage.DEU || Settings.language == Settings.GameLanguage.THA || Settings.language == Settings.GameLanguage.TUR || Settings.language == Settings.GameLanguage.KOR || Settings.language == Settings.GameLanguage.RUS || Settings.language == Settings.GameLanguage.SPA || Settings.language == Settings.GameLanguage.DUT) {
            String msg = String.format(TEXT[4] + character + TEXT[2], ascension, floor);
            logger.debug("Setting Rich Presence: " + msg);
            this.setRichPresenceData("status", msg);
        } else {
            String msg = String.format(character + TEXT[2] + TEXT[4], floor, ascension);
            logger.debug("Setting Rich Presence: " + msg);
            this.setRichPresenceData("status", msg);
        }
        this.setRichPresenceData("steam_display", "#Status");
    }

    @Override
    public void setRichPresenceDisplayPlaying(int floor, String character) {
        if (TEXT == null) {
            TEXT = CardCrawlGame.languagePack.getUIString((String)"RichPresence").TEXT;
        }
        if (Settings.isDailyRun) {
            String msg = String.format(TEXT[0], floor);
            logger.debug("Setting Rich Presence: " + msg);
            this.setRichPresenceData("status", msg);
        } else if (Settings.isTrial) {
            String msg = String.format(TEXT[1], floor);
            logger.debug("Setting Rich Presence: " + msg);
            this.setRichPresenceData("status", msg);
        } else {
            String msg = String.format(character + TEXT[2], floor);
            logger.debug("Setting Rich Presence: " + msg);
            this.setRichPresenceData("status", msg);
        }
        this.setRichPresenceData("steam_display", "#Status");
    }

    @Override
    public void setRichPresenceDisplayInMenu() {
        if (TEXT == null) {
            TEXT = CardCrawlGame.languagePack.getUIString((String)"RichPresence").TEXT;
        }
        logger.debug("Setting Rich Presence: " + String.format(TEXT[3], new Object[0]));
        this.setRichPresenceData("status", TEXT[3]);
        this.setRichPresenceData("steam_display", "#Status");
    }

    @Override
    public int getNumUnlockedAchievements() {
        int retVal = 0;
        ArrayList<String> keys = new ArrayList<String>();
        keys.add("ADRENALINE");
        keys.add("ASCEND_0");
        keys.add("ASCEND_10");
        keys.add("ASCEND_20");
        keys.add("AUTOMATON");
        keys.add("BARRICADED");
        keys.add("CATALYST");
        keys.add("CHAMP");
        keys.add("COLLECTOR");
        keys.add("COME_AT_ME");
        keys.add("COMMON_SENSE");
        keys.add("CROW");
        keys.add("DONUT");
        keys.add("EMERALD");
        keys.add("EMERALD_PLUS");
        keys.add("FOCUSED");
        keys.add("GHOST_GUARDIAN");
        keys.add("GUARDIAN");
        keys.add("IMPERVIOUS");
        keys.add("INFINITY");
        keys.add("JAXXED");
        keys.add("LUCKY_DAY");
        keys.add("MINIMALIST");
        keys.add("NEON");
        keys.add("NINJA");
        keys.add("ONE_RELIC");
        keys.add("PERFECT");
        keys.add("PLAGUE");
        keys.add("POWERFUL");
        keys.add("PURITY");
        keys.add("RUBY");
        keys.add("RUBY_PLUS");
        keys.add("SAPPHIRE");
        keys.add("SAPPHIRE_PLUS");
        keys.add("AMETHYST");
        keys.add("AMETHYST_PLUS");
        keys.add("SHAPES");
        keys.add("SHRUG_IT_OFF");
        keys.add("SLIME_BOSS");
        keys.add("SPEED_CLIMBER");
        keys.add("THE_ENDING");
        keys.add("THE_PACT");
        keys.add("TIME_EATER");
        keys.add("TRANSIENT");
        keys.add("YOU_ARE_NOTHING");
        for (String s : keys) {
            if (!steamStats.isAchieved(s, false)) continue;
            ++retVal;
        }
        return retVal;
    }

    @Override
    public DistributorFactory.Distributor getType() {
        return DistributorFactory.Distributor.STEAM;
    }

    private void setRichPresenceData(String key, String value) {
        if (steamFriends != null && !steamFriends.setRichPresence(key, value)) {
            logger.info("Failed to set Steam Rich Presence: key=" + key + " value=" + value);
        }
    }

    @Override
    public void dispose() {
        if (this.isInitialized()) {
            SteamAPI.shutdown();
        }
    }

    static {
        accountId = -1;
        lbHandle = null;
        task = null;
        retrieveGlobal = true;
        gettingTime = false;
        lbScore = 0;
        startIndex = 0;
        endIndex = 0;
        isUploadingScore = false;
        statsToUpload = new LinkedList<StatTuple>();
    }

    private static class StatTuple {
        String stat;
        int score;

        StatTuple(String statName, int scoreVal) {
            this.stat = statName;
            this.score = scoreVal;
        }
    }

    static enum LeaderboardTask {
        RETRIEVE,
        RETRIEVE_DAILY,
        UPLOAD,
        UPLOAD_DAILY;

    }
}


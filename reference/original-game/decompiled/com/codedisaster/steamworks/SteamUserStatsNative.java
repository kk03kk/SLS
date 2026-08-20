/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamLeaderboardEntry;
import com.codedisaster.steamworks.SteamUserStatsCallbackAdapter;

final class SteamUserStatsNative {
    SteamUserStatsNative() {
    }

    static native long createCallback(SteamUserStatsCallbackAdapter var0);

    static native boolean requestCurrentStats();

    static native boolean getStat(String var0, int[] var1);

    static native boolean setStat(String var0, int var1);

    static native boolean getStat(String var0, float[] var1);

    static native boolean setStat(String var0, float var1);

    static native boolean getAchievement(String var0, boolean[] var1);

    static native boolean setAchievement(String var0);

    static native boolean clearAchievement(String var0);

    static native boolean storeStats();

    static native boolean indicateAchievementProgress(String var0, int var1, int var2);

    static native int getNumAchievements();

    static native String getAchievementName(int var0);

    static native boolean resetAllStats(boolean var0);

    static native long findOrCreateLeaderboard(long var0, String var2, int var3, int var4);

    static native long findLeaderboard(long var0, String var2);

    static native String getLeaderboardName(long var0);

    static native int getLeaderboardEntryCount(long var0);

    static native int getLeaderboardSortMethod(long var0);

    static native int getLeaderboardDisplayType(long var0);

    static native long downloadLeaderboardEntries(long var0, long var2, int var4, int var5, int var6);

    static native long downloadLeaderboardEntriesForUsers(long var0, long var2, long[] var4, int var5);

    static native boolean getDownloadedLeaderboardEntry(long var0, int var2, SteamLeaderboardEntry var3, int[] var4, int var5);

    static native boolean getDownloadedLeaderboardEntry(long var0, int var2, SteamLeaderboardEntry var3);

    static native long uploadLeaderboardScore(long var0, long var2, int var4, int var5, int[] var6, int var7);

    static native long uploadLeaderboardScore(long var0, long var2, int var4, int var5);

    static native long getNumberOfCurrentPlayers(long var0);

    static native long requestGlobalStats(long var0, int var2);

    static native boolean getGlobalStat(String var0, long[] var1);

    static native boolean getGlobalStat(String var0, double[] var1);

    static native int getGlobalStatHistory(String var0, long[] var1, int var2);

    static native int getGlobalStatHistory(String var0, double[] var1, int var2);
}


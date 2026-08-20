/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.integrations;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.integrations.DistributorFactory;
import com.megacrit.cardcrawl.screens.leaderboards.FilterButton;

public interface PublisherIntegration {
    public boolean isInitialized();

    public void dispose();

    public void deleteAllCloudFiles();

    public boolean setStat(String var1, int var2);

    public int getStat(String var1);

    public boolean incrementStat(String var1, int var2);

    public long getGlobalStat(String var1);

    public void uploadDailyLeaderboardScore(String var1, int var2);

    public void uploadLeaderboardScore(String var1, int var2);

    public void unlockAchievement(String var1);

    public void getLeaderboardEntries(AbstractPlayer.PlayerClass var1, FilterButton.RegionSetting var2, FilterButton.LeaderboardType var3, int var4, int var5);

    public void getDailyLeaderboard(long var1, int var3, int var4);

    public void setRichPresenceDisplayPlaying(int var1, String var2);

    public void setRichPresenceDisplayPlaying(int var1, int var2, String var3);

    public void setRichPresenceDisplayInMenu();

    public int getNumUnlockedAchievements();

    public DistributorFactory.Distributor getType();
}


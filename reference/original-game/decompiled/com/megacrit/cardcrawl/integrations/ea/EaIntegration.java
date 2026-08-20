/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.integrations.ea;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.integrations.DistributorFactory;
import com.megacrit.cardcrawl.integrations.PublisherIntegration;
import com.megacrit.cardcrawl.screens.leaderboards.FilterButton;

public class EaIntegration
implements PublisherIntegration {
    @Override
    public boolean isInitialized() {
        return false;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void deleteAllCloudFiles() {
    }

    @Override
    public boolean setStat(String id, int value) {
        return false;
    }

    @Override
    public int getStat(String id) {
        return 0;
    }

    @Override
    public boolean incrementStat(String id, int incrementAmt) {
        return false;
    }

    @Override
    public long getGlobalStat(String id) {
        return 0L;
    }

    @Override
    public void uploadDailyLeaderboardScore(String name, int score) {
    }

    @Override
    public void uploadLeaderboardScore(String name, int score) {
    }

    @Override
    public void unlockAchievement(String id) {
    }

    @Override
    public void getLeaderboardEntries(AbstractPlayer.PlayerClass pClass, FilterButton.RegionSetting rSetting, FilterButton.LeaderboardType lType, int start, int end) {
    }

    @Override
    public void getDailyLeaderboard(long date, int start, int end) {
    }

    @Override
    public void setRichPresenceDisplayPlaying(int floor, String character) {
    }

    @Override
    public void setRichPresenceDisplayPlaying(int floor, int ascension, String character) {
    }

    @Override
    public void setRichPresenceDisplayInMenu() {
    }

    @Override
    public DistributorFactory.Distributor getType() {
        return DistributorFactory.Distributor.EA;
    }

    private void setRichPresenceData(String state, String details) {
    }

    @Override
    public int getNumUnlockedAchievements() {
        return 0;
    }
}


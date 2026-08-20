/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.leaderboards;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.leaderboards.LeaderboardScreen;
import com.megacrit.cardcrawl.screens.stats.CharStat;

public class LeaderboardEntry {
    public int rank;
    public int score;
    public String name;
    public boolean isTime = false;
    private Color color = Settings.CREAM_COLOR.cpy();
    private static final float START_Y = 800.0f * Settings.scale;
    private static final float LINE_SPACING = -32.0f * Settings.scale;

    public LeaderboardEntry(int rank, String name, int score, boolean isTime, boolean isYou) {
        this.rank = rank;
        this.name = name.length() > 18 ? name.substring(0, 18) + "..." : name;
        this.score = score;
        this.isTime = isTime;
        if (isYou) {
            this.color = Settings.GREEN_TEXT_COLOR.cpy();
        }
    }

    public void render(SpriteBatch sb, int index) {
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.panelNameFont, Integer.toString(this.rank), LeaderboardScreen.RANK_X, (float)index * LINE_SPACING + START_Y, this.color);
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.leaderboardFont, this.name, LeaderboardScreen.NAME_X, (float)index * LINE_SPACING + START_Y, this.color);
        if (this.isTime) {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.panelNameFont, CharStat.formatHMSM(this.score), LeaderboardScreen.SCORE_X, (float)index * LINE_SPACING + START_Y, this.color);
        } else {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.panelNameFont, Integer.toString(this.score), LeaderboardScreen.SCORE_X, (float)index * LINE_SPACING + START_Y, this.color);
        }
    }
}


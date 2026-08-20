/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.stats;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.stats.AchievementGrid;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class AchievementItem {
    private TextureAtlas.AtlasRegion img;
    public Hitbox hb = new Hitbox(160.0f * Settings.scale, 160.0f * Settings.scale);
    private String title;
    private String desc;
    public String key;
    public boolean isUnlocked;
    private static final Color LOCKED_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.8f);

    public AchievementItem(String title, String desc, String imgUrl, String key, boolean hidden) {
        this.isUnlocked = UnlockTracker.achievementPref.getBoolean(key, false);
        this.key = key;
        if (this.isUnlocked) {
            CardCrawlGame.publisherIntegration.unlockAchievement(key);
            this.title = title;
            this.desc = desc;
            if (StatsScreen.atlas != null) {
                this.img = StatsScreen.atlas.findRegion("unlocked/" + imgUrl);
            }
        } else if (hidden) {
            this.title = AchievementGrid.NAMES[26];
            this.desc = AchievementGrid.TEXT[26];
            if (StatsScreen.atlas != null) {
                this.img = StatsScreen.atlas.findRegion("locked/" + imgUrl);
            }
        } else {
            this.title = title;
            this.desc = desc;
            if (StatsScreen.atlas != null) {
                this.img = StatsScreen.atlas.findRegion("locked/" + imgUrl);
            }
        }
    }

    public AchievementItem(String title, String desc, String imgUrl, String key) {
        this(title, desc, imgUrl, key, false);
    }

    public void update() {
        if (this.hb != null) {
            this.hb.update();
            if (this.hb.hovered) {
                TipHelper.renderGenericTip((float)InputHelper.mX + 100.0f * Settings.scale, InputHelper.mY, this.title, this.desc);
            }
        }
    }

    public void render(SpriteBatch sb, float x, float y) {
        if (!this.isUnlocked) {
            sb.setColor(LOCKED_COLOR);
        } else {
            sb.setColor(Color.WHITE);
        }
        if (this.hb.hovered) {
            sb.draw(this.img, x - (float)this.img.packedWidth / 2.0f, y - (float)this.img.packedHeight / 2.0f, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, Settings.scale * 1.1f, Settings.scale * 1.1f, 0.0f);
        } else {
            sb.draw(this.img, x - (float)this.img.packedWidth / 2.0f, y - (float)this.img.packedHeight / 2.0f, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, Settings.scale, Settings.scale, 0.0f);
        }
        this.hb.move(x, y);
        this.hb.render(sb);
    }

    public void reloadImg() {
        this.img = StatsScreen.atlas.findRegion(this.img.name);
    }
}


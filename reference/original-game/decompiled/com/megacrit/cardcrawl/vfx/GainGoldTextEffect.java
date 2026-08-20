/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class GainGoldTextEffect
extends AbstractGameEffect {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("GainGoldTextEffect");
    public static final String[] TEXT = GainGoldTextEffect.uiStrings.TEXT;
    private static int totalGold = 0;
    private int gold = 0;
    private boolean reachedCenter = false;
    private float x;
    private float y;
    private float destinationY;
    private static final float WAIT_TIME = 1.0f;
    private float waitTimer = 1.0f;
    private float fadeTimer = 1.0f;
    private static final float FADE_Y_SPEED = 100.0f * Settings.scale;
    private static final float TEXT_DURATION = 3.0f;

    public GainGoldTextEffect(int startingAmount) {
        this.x = AbstractDungeon.player.hb.cX;
        this.y = AbstractDungeon.player.hb.cY;
        this.destinationY = this.y + 150.0f * Settings.scale;
        this.duration = 3.0f;
        this.startingDuration = 3.0f;
        this.reachedCenter = false;
        this.gold = startingAmount;
        totalGold = startingAmount;
        this.color = Color.GOLD.cpy();
    }

    @Override
    public void update() {
        if (this.waitTimer > 0.0f) {
            this.gold = totalGold;
            if (!this.reachedCenter && this.y != this.destinationY) {
                this.y = MathUtils.lerp(this.y, this.destinationY, Gdx.graphics.getDeltaTime() * 9.0f);
                if (Math.abs(this.y - this.destinationY) < Settings.UI_SNAP_THRESHOLD) {
                    this.y = this.destinationY;
                    this.reachedCenter = true;
                }
            } else {
                this.waitTimer -= Gdx.graphics.getDeltaTime();
                this.gold = this.waitTimer < 0.0f ? totalGold : totalGold;
            }
        } else {
            this.y += Gdx.graphics.getDeltaTime() * FADE_Y_SPEED;
            this.fadeTimer -= Gdx.graphics.getDeltaTime();
            this.color.a = this.fadeTimer;
            if (this.fadeTimer < 0.0f) {
                this.isDone = true;
            }
        }
    }

    public boolean ping(int amount) {
        if (this.waitTimer > 0.0f) {
            this.waitTimer = 1.0f;
            totalGold += amount;
            return true;
        }
        return false;
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.isDone) {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, "+ " + Integer.toString(this.gold) + TEXT[0], this.x, this.y, this.color);
        }
    }

    @Override
    public void dispose() {
    }
}


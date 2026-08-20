/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.FlyingSpikeEffect;

public class PowerExpireTextEffect
extends AbstractGameEffect {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("PowerExpireTextEffect");
    public static final String[] TEXT = PowerExpireTextEffect.uiStrings.TEXT;
    private static final float TEXT_DURATION = 2.0f;
    private static final float STARTING_OFFSET_Y = 80.0f * Settings.scale;
    private static final float TARGET_OFFSET_Y = 160.0f * Settings.scale;
    private float x;
    private float y;
    private float offsetY;
    private float h;
    private String msg;
    private boolean spikeEffectTriggered = false;
    private TextureAtlas.AtlasRegion region;

    public PowerExpireTextEffect(float x, float y, String msg, TextureAtlas.AtlasRegion region) {
        this.duration = 2.0f;
        this.startingDuration = 2.0f;
        this.msg = msg;
        this.x = x - 64.0f * Settings.scale;
        this.y = y;
        this.color = Color.WHITE.cpy();
        this.offsetY = STARTING_OFFSET_Y;
        this.region = region;
        this.scale = Settings.scale * 0.7f;
    }

    @Override
    public void update() {
        if (this.duration < this.startingDuration * 0.8f && !this.spikeEffectTriggered && !Settings.DISABLE_EFFECTS) {
            int i;
            this.spikeEffectTriggered = true;
            for (i = 0; i < 10; ++i) {
                AbstractDungeon.effectsQueue.add(new FlyingSpikeEffect(this.x - MathUtils.random(20.0f) * Settings.scale + 70.0f * Settings.scale, this.y + MathUtils.random(STARTING_OFFSET_Y, TARGET_OFFSET_Y) * Settings.scale, 0.0f, MathUtils.random(50.0f, 400.0f) * Settings.scale, 0.0f, Settings.BLUE_TEXT_COLOR));
            }
            for (i = 0; i < 10; ++i) {
                AbstractDungeon.effectsQueue.add(new FlyingSpikeEffect(this.x + MathUtils.random(20.0f) * Settings.scale, this.y + MathUtils.random(STARTING_OFFSET_Y, TARGET_OFFSET_Y) * Settings.scale, 0.0f, MathUtils.random(-400.0f, -50.0f) * Settings.scale, 0.0f, Settings.BLUE_TEXT_COLOR));
            }
        }
        this.offsetY = Interpolation.exp10In.apply(TARGET_OFFSET_Y, STARTING_OFFSET_Y, this.duration / 2.0f);
        this.color.a = Interpolation.exp10Out.apply(0.0f, 1.0f, this.duration / 2.0f);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
            this.duration = 0.0f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.losePowerFont, this.msg, this.x, this.y + this.offsetY, this.color);
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.losePowerFont, TEXT[0], this.x, this.y + this.offsetY - 40.0f * Settings.scale, this.color);
        if (this.region != null) {
            sb.setColor(this.color);
            sb.setBlendFunction(770, 1);
            sb.draw(this.region, this.x - (float)(this.region.packedWidth / 2) - 64.0f * Settings.scale, this.y + this.h + this.offsetY - (float)(this.region.packedHeight / 2) - 30.0f * Settings.scale, (float)this.region.packedWidth / 2.0f, (float)this.region.packedHeight / 2.0f, this.region.packedWidth, this.region.packedHeight, this.scale, this.scale, this.rotation);
            sb.setBlendFunction(770, 771);
        }
    }

    @Override
    public void dispose() {
    }
}


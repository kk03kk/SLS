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
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class BiteEffect
extends AbstractGameEffect {
    private static TextureAtlas.AtlasRegion top;
    private static TextureAtlas.AtlasRegion bot;
    private float x;
    private float y;
    private float sY;
    private float dY;
    private float y2;
    private float sY2;
    private float dY2;
    private static final float DUR = 1.0f;
    private boolean playedSfx = false;

    public BiteEffect(float x, float y, Color c) {
        if (top == null) {
            top = ImageMaster.vfxAtlas.findRegion("combat/biteTop");
            bot = ImageMaster.vfxAtlas.findRegion("combat/biteBot");
        }
        this.x = x - (float)BiteEffect.top.packedWidth / 2.0f;
        this.sY = y - (float)BiteEffect.top.packedHeight / 2.0f + 150.0f * Settings.scale;
        this.dY = y - 0.0f * Settings.scale;
        this.y = this.sY;
        this.sY2 = y - (float)(BiteEffect.top.packedHeight / 2) - 100.0f * Settings.scale;
        this.dY2 = y - 90.0f * Settings.scale;
        this.y2 = this.sY2;
        this.duration = 1.0f;
        this.startingDuration = 1.0f;
        this.color = c;
        this.scale = 1.0f * Settings.scale;
    }

    public BiteEffect(float x, float y) {
        this(x, y, new Color(0.7f, 0.9f, 1.0f, 0.0f));
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < this.startingDuration - 0.3f && !this.playedSfx) {
            this.playedSfx = true;
            CardCrawlGame.sound.play("EVENT_VAMP_BITE", 0.05f);
        }
        if (this.duration > this.startingDuration / 2.0f) {
            this.color.a = Interpolation.fade.apply(1.0f, 0.0f, (this.duration - 0.5f) * 2.0f);
            this.y = Interpolation.bounceIn.apply(this.dY, this.sY, (this.duration - 0.5f) * 2.0f);
            this.y2 = Interpolation.bounceIn.apply(this.dY2, this.sY2, (this.duration - 0.5f) * 2.0f);
        } else {
            this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.duration * 2.0f);
            this.y = Interpolation.fade.apply(this.sY, this.dY, this.duration * 2.0f);
            this.y2 = Interpolation.fade.apply(this.sY2, this.dY2, this.duration * 2.0f);
        }
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(top, this.x, this.y, (float)BiteEffect.top.packedWidth / 2.0f, (float)BiteEffect.top.packedHeight / 2.0f, BiteEffect.top.packedWidth, BiteEffect.top.packedHeight, this.scale + MathUtils.random(-0.05f, 0.05f), this.scale + MathUtils.random(-0.05f, 0.05f), 0.0f);
        sb.draw(bot, this.x, this.y2, (float)BiteEffect.top.packedWidth / 2.0f, (float)BiteEffect.top.packedHeight / 2.0f, BiteEffect.top.packedWidth, BiteEffect.top.packedHeight, this.scale + MathUtils.random(-0.05f, 0.05f), this.scale + MathUtils.random(-0.05f, 0.05f), 0.0f);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


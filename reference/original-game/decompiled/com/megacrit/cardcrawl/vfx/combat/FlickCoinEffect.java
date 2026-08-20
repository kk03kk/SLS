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
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.ShineSparkleEffect;
import com.megacrit.cardcrawl.vfx.combat.AdditiveSlashImpactEffect;

public class FlickCoinEffect
extends AbstractGameEffect {
    private static TextureAtlas.AtlasRegion img;
    private float sX;
    private float sY;
    private float cX;
    private float cY;
    private float dX;
    private float dY;
    private float yOffset;
    private float bounceHeight;
    private static final float DUR = 0.5f;
    private boolean playedSfx = false;
    private float sparkleTimer = 0.0f;

    public FlickCoinEffect(float srcX, float srcY, float destX, float destY) {
        if (img == null) {
            img = ImageMaster.vfxAtlas.findRegion("combat/empowerCircle1");
        }
        this.sX = srcX;
        this.sY = srcY;
        this.cX = this.sX;
        this.cY = this.sY;
        this.dX = destX;
        this.dY = destY - 100.0f * Settings.scale;
        this.rotation = 0.0f;
        this.duration = 0.5f;
        this.color = new Color(1.0f, 1.0f, 0.0f, 0.0f);
        this.bounceHeight = this.sY > this.dY ? 600.0f * Settings.scale : this.dY - this.sY + 600.0f * Settings.scale;
    }

    @Override
    public void update() {
        if (!this.playedSfx) {
            this.playedSfx = true;
            CardCrawlGame.sound.playA("ATTACK_WHIFF_2", MathUtils.random(0.7f, 0.8f));
        }
        this.sparkleTimer -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.4f && this.sparkleTimer < 0.0f) {
            for (int i = 0; i < MathUtils.random(2, 5); ++i) {
                AbstractDungeon.effectsQueue.add(new ShineSparkleEffect(this.cX, this.cY + this.yOffset));
            }
            this.sparkleTimer = MathUtils.random(0.05f, 0.1f);
        }
        this.cX = Interpolation.linear.apply(this.dX, this.sX, this.duration / 0.5f);
        this.cY = Interpolation.linear.apply(this.dY, this.sY, this.duration / 0.5f);
        this.rotation = this.dX > this.sX ? (this.rotation -= Gdx.graphics.getDeltaTime() * 1000.0f) : (this.rotation += Gdx.graphics.getDeltaTime() * 1000.0f);
        if (this.duration > 0.25f) {
            this.color.a = Interpolation.exp5In.apply(1.0f, 0.0f, (this.duration - 0.25f) / 0.2f) * Settings.scale;
            this.yOffset = Interpolation.circleIn.apply(this.bounceHeight, 0.0f, (this.duration - 0.25f) / 0.25f) * Settings.scale;
        } else {
            this.yOffset = Interpolation.circleOut.apply(0.0f, this.bounceHeight, this.duration / 0.25f) * Settings.scale;
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
            CardCrawlGame.sound.playA("GOLD_GAIN", MathUtils.random(0.0f, 0.1f));
            AbstractDungeon.effectsQueue.add(new AdditiveSlashImpactEffect(this.dX, this.dY + 100.0f * Settings.scale, Color.GOLD.cpy()));
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(new Color(0.4f, 1.0f, 1.0f, this.color.a / 5.0f));
        sb.setColor(this.color);
        sb.draw(img, this.cX - (float)(FlickCoinEffect.img.packedWidth / 2), this.cY - (float)(FlickCoinEffect.img.packedHeight / 2) + this.yOffset, (float)FlickCoinEffect.img.packedWidth / 2.0f, (float)FlickCoinEffect.img.packedHeight / 2.0f, FlickCoinEffect.img.packedWidth, FlickCoinEffect.img.packedHeight, this.scale * 0.7f, this.scale * 0.4f, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


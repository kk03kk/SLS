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
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class IronWaveParticle
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 0.5f;
    private float x;
    private float y;
    private float targetY;
    private static TextureAtlas.AtlasRegion img;
    private boolean impactHook = false;

    public IronWaveParticle(float x, float y) {
        if (img == null) {
            img = ImageMaster.vfxAtlas.findRegion("combat/weightyImpact");
        }
        this.scale = Settings.scale;
        this.x = x - (float)IronWaveParticle.img.packedWidth / 2.0f;
        this.y = (float)Settings.HEIGHT - (float)IronWaveParticle.img.packedHeight / 2.0f;
        this.duration = 0.5f;
        this.targetY = y - 180.0f * Settings.scale;
        this.rotation = 0.0f;
        this.color = new Color(1.0f, 1.0f, 0.1f, 0.0f);
    }

    @Override
    public void update() {
        this.y = Interpolation.fade.apply(Settings.HEIGHT, this.targetY, 1.0f - this.duration / 0.5f);
        this.scale += Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else if (this.duration < 0.2f) {
            if (!this.impactHook) {
                this.impactHook = true;
                CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.SHORT, false);
            }
            this.color.a = Interpolation.pow2Out.apply(0.0f, 1.0f, this.duration * 5.0f);
        } else {
            this.color.a = Interpolation.fade.apply(1.0f, 0.0f, this.duration / 0.5f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        this.color.g = 1.0f;
        sb.setColor(this.color);
        sb.draw(img, this.x, this.y + 140.0f * Settings.scale, (float)IronWaveParticle.img.packedWidth / 2.0f, (float)IronWaveParticle.img.packedHeight / 2.0f, (float)IronWaveParticle.img.packedWidth / 2.0f, (float)IronWaveParticle.img.packedHeight * (this.duration + 0.2f) * 3.0f, this.scale * MathUtils.random(0.99f, 1.01f) * 0.5f, this.scale * MathUtils.random(0.99f, 1.01f) * 2.0f * (this.duration + 0.8f), this.rotation);
        this.color.g = 0.7f;
        sb.setColor(this.color);
        sb.draw(img, this.x - 50.0f * Settings.scale, this.y + 140.0f * Settings.scale, (float)IronWaveParticle.img.packedWidth / 2.0f, (float)IronWaveParticle.img.packedHeight / 2.0f, (float)IronWaveParticle.img.packedWidth / 2.0f, (float)IronWaveParticle.img.packedHeight * (this.duration + 0.2f) * 2.0f, this.scale * MathUtils.random(0.99f, 1.01f) * 0.6f, this.scale * MathUtils.random(0.99f, 1.01f) * 2.0f * (this.duration + 0.8f), this.rotation);
        this.color.g = 0.5f;
        sb.setColor(this.color);
        sb.draw(img, this.x - 100.0f * Settings.scale, this.y + 140.0f * Settings.scale, (float)IronWaveParticle.img.packedWidth / 2.0f, (float)IronWaveParticle.img.packedHeight / 2.0f, IronWaveParticle.img.packedWidth, (float)IronWaveParticle.img.packedHeight * (this.duration + 0.2f) * 1.0f, this.scale * MathUtils.random(0.99f, 1.01f) * 0.75f, this.scale * MathUtils.random(0.99f, 1.01f) * 2.0f * (this.duration + 0.8f), this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


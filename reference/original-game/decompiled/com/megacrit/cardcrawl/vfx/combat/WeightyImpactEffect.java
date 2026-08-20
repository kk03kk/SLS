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
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.UpgradeShineParticleEffect;
import com.megacrit.cardcrawl.vfx.combat.DamageImpactCurvyEffect;

public class WeightyImpactEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 1.0f;
    private float x;
    private float y;
    private float targetY;
    private static TextureAtlas.AtlasRegion img;
    private boolean impactHook = false;

    public WeightyImpactEffect(float x, float y) {
        this(x, y, new Color(1.0f, 1.0f, 0.1f, 0.0f));
    }

    public WeightyImpactEffect(float x, float y, Color newColor) {
        if (img == null) {
            img = ImageMaster.vfxAtlas.findRegion("combat/weightyImpact");
        }
        this.scale = Settings.scale;
        this.x = x - (float)WeightyImpactEffect.img.packedWidth / 2.0f;
        this.y = (float)Settings.HEIGHT - (float)WeightyImpactEffect.img.packedHeight / 2.0f;
        this.duration = 1.0f;
        this.targetY = y - 180.0f * Settings.scale;
        this.rotation = MathUtils.random(-1.0f, 1.0f);
        this.color = newColor;
    }

    @Override
    public void update() {
        this.y = Interpolation.fade.apply(Settings.HEIGHT, this.targetY, 1.0f - this.duration / 1.0f);
        this.scale += Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
            CardCrawlGame.sound.playA("ATTACK_IRON_2", -0.5f);
        } else if (this.duration < 0.2f) {
            if (!this.impactHook) {
                int i;
                this.impactHook = true;
                AbstractDungeon.effectsQueue.add(new BorderFlashEffect(Color.ORANGE));
                CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.MED, true);
                for (i = 0; i < 5; ++i) {
                    AbstractDungeon.effectsQueue.add(new DamageImpactCurvyEffect(this.x + (float)WeightyImpactEffect.img.packedWidth / 2.0f, this.y + (float)WeightyImpactEffect.img.packedWidth / 2.0f));
                }
                for (i = 0; i < 30; ++i) {
                    AbstractDungeon.effectsQueue.add(new UpgradeShineParticleEffect(this.x + MathUtils.random(-100.0f, 100.0f) * Settings.scale + (float)WeightyImpactEffect.img.packedWidth / 2.0f, this.y + MathUtils.random(-50.0f, 120.0f) * Settings.scale + (float)WeightyImpactEffect.img.packedHeight / 2.0f));
                }
            }
            this.color.a = Interpolation.fade.apply(0.0f, 0.5f, 0.2f / this.duration);
        } else {
            this.color.a = Interpolation.pow2Out.apply(0.6f, 0.0f, this.duration / 1.0f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        this.color.g = 1.0f;
        sb.setColor(this.color);
        sb.draw(img, this.x, this.y + 140.0f * Settings.scale, (float)WeightyImpactEffect.img.packedWidth / 2.0f, (float)WeightyImpactEffect.img.packedHeight / 2.0f, WeightyImpactEffect.img.packedWidth, (float)WeightyImpactEffect.img.packedHeight * (this.duration + 0.2f) * 5.0f, this.scale * MathUtils.random(0.99f, 1.01f) * 0.3f, this.scale * MathUtils.random(0.99f, 1.01f) * 2.0f * (this.duration + 0.8f), this.rotation);
        this.color.g = 0.6f;
        sb.setColor(this.color);
        sb.draw(img, this.x, this.y + 40.0f * Settings.scale, (float)WeightyImpactEffect.img.packedWidth / 2.0f, (float)WeightyImpactEffect.img.packedHeight / 2.0f, WeightyImpactEffect.img.packedWidth, (float)WeightyImpactEffect.img.packedHeight * (this.duration + 0.2f) * 5.0f, this.scale * MathUtils.random(0.99f, 1.01f) * 0.7f, this.scale * MathUtils.random(0.99f, 1.01f) * 1.3f * (this.duration + 0.8f), this.rotation);
        this.color.g = 0.2f;
        sb.setColor(this.color);
        sb.draw(img, this.x, this.y, (float)WeightyImpactEffect.img.packedWidth / 2.0f, (float)WeightyImpactEffect.img.packedHeight / 2.0f, WeightyImpactEffect.img.packedWidth, (float)WeightyImpactEffect.img.packedHeight * (this.duration + 0.2f) * 5.0f, this.scale * MathUtils.random(0.99f, 1.01f), this.scale * MathUtils.random(0.99f, 1.01f) * (this.duration + 0.8f), this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


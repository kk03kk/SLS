/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;

public class ViceCrushEffect
extends AbstractGameEffect {
    private static TextureAtlas.AtlasRegion img;
    private boolean impactHook = false;
    private float x;
    private float x2;
    private float y;
    private float startX;
    private float startX2;
    private float targetX;
    private float targetX2;

    public ViceCrushEffect(float x, float y) {
        if (img == null) {
            img = ImageMaster.vfxAtlas.findRegion("combat/weightyImpact");
        }
        this.startX = x - 300.0f * Settings.scale - (float)ViceCrushEffect.img.packedWidth / 2.0f;
        this.startX2 = x + 300.0f * Settings.scale - (float)ViceCrushEffect.img.packedWidth / 2.0f;
        this.targetX = x - 120.0f * Settings.scale - (float)ViceCrushEffect.img.packedWidth / 2.0f;
        this.targetX2 = x + 120.0f * Settings.scale - (float)ViceCrushEffect.img.packedWidth / 2.0f;
        this.x = this.startX;
        this.x2 = this.startX2;
        this.y = y - (float)ViceCrushEffect.img.packedHeight / 2.0f;
        this.scale = 1.1f;
        this.duration = 0.7f;
        this.startingDuration = 0.7f;
        this.rotation = 90.0f;
        this.color = Color.PURPLE.cpy();
        this.color.a = 0.0f;
    }

    @Override
    public void update() {
        if (this.duration == this.startingDuration) {
            CardCrawlGame.sound.playA("ATTACK_MAGIC_FAST_3", -0.4f);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else if (this.duration < 0.2f) {
            if (!this.impactHook) {
                this.impactHook = true;
                AbstractDungeon.effectsQueue.add(new BorderFlashEffect(Color.PURPLE));
                CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.HIGH, ScreenShake.ShakeDur.MED, true);
            }
            this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.duration * 5.0f);
        } else {
            this.color.a = Interpolation.fade.apply(1.0f, 0.0f, this.duration / this.startingDuration);
        }
        this.scale += 1.1f * Gdx.graphics.getDeltaTime();
        this.x = Interpolation.fade.apply(this.targetX, this.startX, this.duration / this.startingDuration);
        this.x2 = Interpolation.fade.apply(this.targetX2, this.startX2, this.duration / this.startingDuration);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(new Color(0.5f, 0.5f, 0.9f, this.color.a));
        sb.draw(img, this.x, this.y, (float)ViceCrushEffect.img.packedWidth / 2.0f, (float)ViceCrushEffect.img.packedHeight / 2.0f, ViceCrushEffect.img.packedWidth, ViceCrushEffect.img.packedHeight, this.scale * this.scale * Settings.scale * 0.5f, this.scale * Settings.scale * (this.duration + 0.8f), this.rotation);
        sb.draw(img, this.x2, this.y, (float)ViceCrushEffect.img.packedWidth / 2.0f, (float)ViceCrushEffect.img.packedHeight / 2.0f, ViceCrushEffect.img.packedWidth, ViceCrushEffect.img.packedHeight, this.scale * this.scale * Settings.scale * 0.5f, this.scale * Settings.scale * (this.duration + 0.8f), this.rotation - 180.0f);
        sb.setColor(new Color(0.7f, 0.5f, 0.9f, this.color.a));
        sb.draw(img, this.x, this.y, (float)ViceCrushEffect.img.packedWidth / 2.0f, (float)ViceCrushEffect.img.packedHeight / 2.0f, ViceCrushEffect.img.packedWidth, ViceCrushEffect.img.packedHeight, this.scale * this.scale * Settings.scale * 0.75f, this.scale * Settings.scale * (this.duration + 0.8f), this.rotation);
        sb.draw(img, this.x2, this.y, (float)ViceCrushEffect.img.packedWidth / 2.0f, (float)ViceCrushEffect.img.packedHeight / 2.0f, ViceCrushEffect.img.packedWidth, ViceCrushEffect.img.packedHeight, this.scale * this.scale * Settings.scale * 0.75f, this.scale * Settings.scale * (this.duration + 0.8f), this.rotation - 180.0f);
        sb.setColor(this.color);
        sb.draw(img, this.x, this.y, (float)ViceCrushEffect.img.packedWidth / 2.0f, (float)ViceCrushEffect.img.packedHeight / 2.0f, ViceCrushEffect.img.packedWidth, ViceCrushEffect.img.packedHeight, this.scale * this.scale * Settings.scale, this.scale * Settings.scale * (this.duration + 1.0f), this.rotation);
        sb.draw(img, this.x2, this.y, (float)ViceCrushEffect.img.packedWidth / 2.0f, (float)ViceCrushEffect.img.packedHeight / 2.0f, ViceCrushEffect.img.packedWidth, ViceCrushEffect.img.packedHeight, this.scale * this.scale * Settings.scale, this.scale * Settings.scale * (this.duration + 1.0f), this.rotation - 180.0f);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


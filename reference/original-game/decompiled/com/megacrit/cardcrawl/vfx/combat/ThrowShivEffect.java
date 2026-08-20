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

public class ThrowShivEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float destY;
    private static final float DUR = 0.4f;
    private TextureAtlas.AtlasRegion img = ImageMaster.DAGGER_STREAK;
    private boolean playedSound = false;

    public ThrowShivEffect(float x, float y) {
        this.x = x - MathUtils.random(320.0f, 360.0f) - (float)this.img.packedWidth / 2.0f;
        this.destY = y;
        this.y = this.destY + MathUtils.random(-25.0f, 25.0f) * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.startingDuration = 0.4f;
        this.duration = 0.4f;
        this.scale = Settings.scale * MathUtils.random(0.5f, 2.0f);
        this.rotation = MathUtils.random(-30.0f, 30.0f);
        float darkness = MathUtils.random(0.5f, 0.8f);
        this.color = new Color(darkness, darkness, darkness, 1.0f);
    }

    private void playRandomSfX() {
        int roll = MathUtils.random(5);
        switch (roll) {
            case 0: {
                CardCrawlGame.sound.play("ATTACK_DAGGER_1");
                break;
            }
            case 1: {
                CardCrawlGame.sound.play("ATTACK_DAGGER_2");
                break;
            }
            case 2: {
                CardCrawlGame.sound.play("ATTACK_DAGGER_3");
                break;
            }
            case 3: {
                CardCrawlGame.sound.play("ATTACK_DAGGER_4");
                break;
            }
            case 4: {
                CardCrawlGame.sound.play("ATTACK_DAGGER_5");
                break;
            }
            default: {
                CardCrawlGame.sound.play("ATTACK_DAGGER_6");
            }
        }
    }

    @Override
    public void update() {
        if (!this.playedSound) {
            this.playRandomSfX();
            this.playedSound = true;
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.color.a = this.duration > 0.2f ? Interpolation.fade.apply(1.0f, 0.0f, (this.duration - 0.2f) * 5.0f) : Interpolation.fade.apply(0.0f, 1.0f, this.duration * 5.0f);
        this.scale = Interpolation.bounceIn.apply(Settings.scale * 0.5f, Settings.scale * 1.5f, this.duration / 0.4f);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth * 0.85f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale * 1.5f, this.rotation);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth * 0.85f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 0.75f, this.scale * 0.75f, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


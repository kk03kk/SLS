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
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class FlyingDaggerEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float destY;
    private float scaleMultiplier;
    private static final float DUR = 0.5f;
    private TextureAtlas.AtlasRegion img = ImageMaster.DAGGER_STREAK;
    private boolean playedSound = false;

    public FlyingDaggerEffect(float x, float y, float fAngle, boolean shouldFlip) {
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.destY = y;
        this.y = this.destY - (float)this.img.packedHeight / 2.0f;
        this.startingDuration = 0.5f;
        this.duration = 0.5f;
        this.scaleMultiplier = MathUtils.random(1.2f, 1.5f);
        this.scale = 0.25f * Settings.scale;
        this.rotation = shouldFlip ? fAngle - 180.0f : fAngle;
        this.color = Color.CHARTREUSE.cpy();
        this.color.a = 0.0f;
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
        Vector2 derp = new Vector2(MathUtils.cos((float)Math.PI / 180 * this.rotation), MathUtils.sin((float)Math.PI / 180 * this.rotation));
        this.x += derp.x * Gdx.graphics.getDeltaTime() * (3500.0f * this.scaleMultiplier) * Settings.scale;
        this.y += derp.y * Gdx.graphics.getDeltaTime() * (3500.0f * this.scaleMultiplier) * Settings.scale;
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.color.a = this.duration > 0.25f ? Interpolation.pow5In.apply(1.0f, 0.0f, (this.duration - 0.25f) * 4.0f) : Interpolation.fade.apply(0.0f, 1.0f, this.duration * 4.0f);
        this.scale += Gdx.graphics.getDeltaTime() * this.scaleMultiplier;
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale * 1.5f, this.rotation);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 0.75f, this.scale * 0.75f, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


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
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class FlameParticleEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img;
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float vY2;
    private float vS;
    private float startingDuration;
    private boolean flipX = MathUtils.randomBoolean();
    private float delayTimer = MathUtils.random(0.15f);

    public FlameParticleEffect(float x, float y) {
        this.setImg();
        this.duration = this.startingDuration = MathUtils.random(0.6f, 1.5f);
        float r = MathUtils.random(-13.0f, 13.0f) * MathUtils.random(-13.0f, 13.0f);
        this.x = x + r * Settings.scale - (float)this.img.packedWidth / 2.0f;
        this.y = y + MathUtils.random(-180.0f, 0.0f) * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.vX = MathUtils.random(-25.0f, 25.0f) * Settings.scale;
        r = MathUtils.random(3.0f, 30.0f);
        this.vY = r * r / this.startingDuration * Settings.scale;
        this.vY2 = MathUtils.random(-100.0f, 100.0f) * Settings.scale;
        this.vS = MathUtils.random(-0.5f, 0.5f) * Settings.scale;
        this.color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
        this.color.g -= MathUtils.random(0.5f);
        this.color.b -= this.color.g - MathUtils.random(0.5f);
        this.rotation = MathUtils.random(-10.0f, 10.0f);
        this.scale = Settings.scale * MathUtils.random(0.2f, 1.5f);
        this.renderBehind = MathUtils.randomBoolean(0.5f);
    }

    @Override
    public void update() {
        if (this.delayTimer > 0.0f) {
            this.delayTimer -= Gdx.graphics.getDeltaTime();
            return;
        }
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.vY += this.vY2 * Gdx.graphics.getDeltaTime();
        this.vY *= 59.0f * Gdx.graphics.getDeltaTime();
        this.scale += this.vS * Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else if (this.duration > this.startingDuration / 2.0f) {
            this.color.a = Interpolation.fade.apply(0.0f, 0.5f, (this.startingDuration - this.duration) / (this.startingDuration / 2.0f));
        } else if (this.duration < this.startingDuration / 2.0f) {
            this.color.a = Interpolation.fade.apply(0.0f, 0.5f, this.duration / (this.startingDuration / 2.0f));
        }
    }

    private void setImg() {
        int roll = MathUtils.random(2);
        this.img = roll == 0 ? ImageMaster.FLAME_1 : (roll == 1 ? ImageMaster.FLAME_2 : ImageMaster.FLAME_3);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        if (this.flipX && !this.img.isFlipX()) {
            this.img.flip(true, false);
        } else if (!this.flipX && this.img.isFlipX()) {
            this.img.flip(true, false);
        }
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


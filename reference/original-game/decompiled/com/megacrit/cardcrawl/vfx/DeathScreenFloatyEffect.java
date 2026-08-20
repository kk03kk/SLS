/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class DeathScreenFloatyEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img;
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float vX2;
    private float vY2;
    private float aV;

    public DeathScreenFloatyEffect() {
        this.startingDuration = this.duration = MathUtils.random(3.0f, 12.0f);
        int roll = MathUtils.random(5);
        this.img = roll == 0 ? ImageMaster.DEATH_VFX_1 : (roll == 1 ? ImageMaster.DEATH_VFX_2 : (roll == 2 ? ImageMaster.DEATH_VFX_3 : (roll == 3 ? ImageMaster.DEATH_VFX_4 : (roll == 4 ? ImageMaster.DEATH_VFX_5 : ImageMaster.DEATH_VFX_6))));
        this.x = MathUtils.random(0.0f, (float)Settings.WIDTH) - (float)this.img.packedWidth / 2.0f;
        this.y = MathUtils.random(0.0f, (float)Settings.HEIGHT) - (float)this.img.packedHeight / 2.0f;
        this.vX = MathUtils.random(-20.0f, 20.0f) * Settings.scale * this.scale;
        this.vY = MathUtils.random(-60.0f, 60.0f) * Settings.scale * this.scale;
        this.vX2 = MathUtils.random(-20.0f, 20.0f) * Settings.scale * this.scale;
        this.vY2 = MathUtils.random(-60.0f, 60.0f) * Settings.scale * this.scale;
        this.aV = MathUtils.random(-50.0f, 50.0f);
        float tmp = MathUtils.random(0.2f, 0.4f);
        this.color = new Color();
        this.color.r = tmp + MathUtils.random(0.0f, 0.2f);
        this.color.g = tmp;
        this.color.b = tmp + MathUtils.random(0.0f, 0.2f);
        this.renderBehind = MathUtils.randomBoolean(0.8f);
        this.scale = MathUtils.random(12.0f, 20.0f) * Settings.scale;
    }

    @Override
    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.vX += this.vX2 * Gdx.graphics.getDeltaTime();
        this.vY += this.vY2 * Gdx.graphics.getDeltaTime();
        this.rotation += this.aV * Gdx.graphics.getDeltaTime();
        if (this.startingDuration - this.duration < 1.5f) {
            this.color.a = Interpolation.fade.apply(0.0f, 0.3f, (this.startingDuration - this.duration) / 1.5f);
        } else if (this.duration < 1.5f) {
            this.color.a = Interpolation.fade.apply(0.3f, 0.0f, 1.0f - this.duration / 1.5f);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
    }

    @Override
    public void dispose() {
    }
}


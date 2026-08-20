/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class ShinySparkleEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float aV;
    private static final int W = 32;

    public ShinySparkleEffect() {
        this.duration = this.startingDuration = MathUtils.random(1.0f, 2.0f);
        this.scale = Settings.scale * MathUtils.random(0.4f, 1.0f);
        this.x = MathUtils.random(0, Settings.WIDTH);
        this.y = MathUtils.random(-100.0f, 550.0f) * Settings.scale + AbstractDungeon.floorY;
        this.vX = MathUtils.random(-24.0f, 24.0f) * Settings.scale;
        this.vY = MathUtils.random(-24.0f, 36.0f) * Settings.scale;
        float colorTmp = MathUtils.random(0.6f, 1.0f);
        this.color = new Color(colorTmp - 0.3f, colorTmp, colorTmp + MathUtils.random(-0.1f, 0.1f), 0.0f);
        this.color.a = 0.0f;
        this.aV = MathUtils.random(-120.0f, 120.0f);
    }

    @Override
    public void update() {
        this.rotation += this.aV * Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        if (this.duration > this.startingDuration / 2.0f) {
            float tmp = this.duration - this.startingDuration / 2.0f;
            this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.startingDuration / 2.0f - tmp) / 4.0f;
        } else {
            this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.duration / (this.startingDuration / 2.0f)) / 4.0f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(ImageMaster.WOBBLY_ORB_VFX, this.x - 16.0f, this.y - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, this.scale * MathUtils.random(1.0f, 1.2f), this.scale / 4.0f, 0.0f, 0, 0, 32, 32, false, false);
        sb.draw(ImageMaster.WOBBLY_ORB_VFX, this.x - 16.0f, this.y - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, this.scale * MathUtils.random(1.0f, 1.5f), this.scale / 4.0f, 90.0f, 0, 0, 32, 32, false, false);
    }

    @Override
    public void dispose() {
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class WobblyCircleEffect
extends AbstractGameEffect {
    private Vector2 pos;
    private float vX;
    private float vY;
    private float aV;
    private static final int W = 32;

    public WobblyCircleEffect() {
        this.duration = this.startingDuration = MathUtils.random(2.0f, 3.0f);
        this.scale = Settings.scale * MathUtils.random(0.2f, 0.5f);
        this.pos = new Vector2(MathUtils.random(0, Settings.WIDTH), MathUtils.random(-100.0f, 500.0f) * Settings.scale + AbstractDungeon.floorY);
        this.vX = MathUtils.random(-72.0f, 72.0f) * Settings.scale;
        this.vY = MathUtils.random(-24.0f, 24.0f) * Settings.scale;
        float colorTmp = MathUtils.random(0.7f, 1.0f);
        this.color = new Color(MathUtils.random(0.7f, 0.8f), colorTmp, colorTmp, 0.0f);
        this.color.a = 0.0f;
        this.aV = MathUtils.random(0.2f, 0.4f);
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.pos.add(this.vX * Gdx.graphics.getDeltaTime(), this.vY * Gdx.graphics.getDeltaTime());
        float dst = this.pos.dst(InputHelper.mX, InputHelper.mY);
        if (dst < 200.0f * Settings.scale) {
            this.pos.add((this.pos.x - (float)InputHelper.mX) * Gdx.graphics.getDeltaTime(), (this.pos.y - (float)InputHelper.mY) * Gdx.graphics.getDeltaTime());
        }
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        if (this.duration > this.startingDuration / 2.0f) {
            float tmp = this.duration - this.startingDuration / 2.0f;
            this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.startingDuration / 2.0f - tmp) * this.aV;
        } else {
            this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.duration / (this.startingDuration / 2.0f)) * this.aV;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(ImageMaster.WOBBLY_ORB_VFX, this.pos.x - 16.0f, this.pos.y - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, this.scale, this.scale, 0.0f, 0, 0, 32, 32, false, false);
    }

    @Override
    public void dispose() {
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class StaffFireEffect
extends AbstractGameEffect {
    private Texture img;
    private float x;
    private float y;
    private float vX;
    private float vY;
    private static final int W = 128;
    private boolean flippedX = MathUtils.randomBoolean();
    private static final float DUR = 0.7f;

    public StaffFireEffect(float x, float y) {
        this.img = this.getImg();
        this.x = x;
        this.y = y;
        this.vX = MathUtils.random(-50.0f, 50.0f) * Settings.scale;
        this.vY = MathUtils.random(20.0f, 140.0f) * Settings.scale;
        this.duration = 0.7f;
        this.color = Color.CHARTREUSE.cpy();
        this.color.a = 0.0f;
        this.scale = MathUtils.random(1.1f, 1.2f) * Settings.scale;
        this.renderBehind = true;
    }

    private Texture getImg() {
        if (MathUtils.randomBoolean()) {
            return ImageMaster.GHOST_ORB_1;
        }
        return ImageMaster.GHOST_ORB_2;
    }

    @Override
    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.color.a = this.duration * 0.75f;
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x - 64.0f, this.y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale * MathUtils.random(1.05f, 1.1f), this.scale, 0.0f, 0, 0, 128, 128, this.flippedX, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


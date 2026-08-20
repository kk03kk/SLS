/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class HbBlockBrokenEffect
extends AbstractGameEffect {
    private static final float WAIT_DUR = 0.4f;
    private static final float EFFECT_DUR = 0.7f;
    private static final int W = 64;
    private static final float DEST_X = -15.0f * Settings.scale;
    private static final float DEST_Y = -10.0f * Settings.scale;
    private static final float INITIAL_VX = 5.0f * Settings.scale;
    private static final float INITIAL_AV = 5.0f;
    private float x;
    private float y;
    private float offsetAngle;
    private float rotateSpeed = 55.0f;
    private float offsetX;
    private float offsetY;

    public HbBlockBrokenEffect(float x, float y) {
        this.color = new Color(0.6f, 0.93f, 0.98f, 1.0f);
        this.duration = 1.1f;
        this.x = x;
        this.y = y;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        if (this.duration < 0.7f) {
            this.offsetX = Interpolation.circleOut.apply(0.0f, DEST_X, 1.0f - this.duration / 0.7f);
            this.offsetY = Interpolation.fade.apply(0.0f, DEST_Y, 1.0f - this.duration / 0.7f);
            this.offsetAngle += Gdx.graphics.getDeltaTime() * this.rotateSpeed;
            this.color.a = Interpolation.fade.apply(1.0f, 0.0f, 1.0f - this.duration / 0.7f);
        } else {
            this.offsetX -= Gdx.graphics.getDeltaTime() * INITIAL_VX;
            this.offsetAngle += Gdx.graphics.getDeltaTime() * 5.0f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(ImageMaster.BLOCK_ICON_L, this.x - 32.0f + this.offsetX, this.y - 32.0f + this.offsetY, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, this.offsetAngle, 0, 0, 64, 64, false, false);
        sb.draw(ImageMaster.BLOCK_ICON_R, this.x - 32.0f - this.offsetX, this.y - 32.0f + this.offsetY, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, -this.offsetAngle, 0, 0, 64, 64, false, false);
    }

    @Override
    public void dispose() {
    }
}


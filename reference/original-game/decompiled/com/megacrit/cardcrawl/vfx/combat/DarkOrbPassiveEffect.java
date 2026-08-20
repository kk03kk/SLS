/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class DarkOrbPassiveEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float startingScale;
    private float rotationSpeed;
    private Texture img;
    private static final int W = 74;

    public DarkOrbPassiveEffect(float x, float y) {
        int roll = MathUtils.random(2);
        switch (roll) {
            case 0: {
                this.img = ImageMaster.DARK_ORB_PASSIVE_VFX_1;
                break;
            }
            case 1: {
                this.img = ImageMaster.DARK_ORB_PASSIVE_VFX_2;
                break;
            }
            default: {
                this.img = ImageMaster.DARK_ORB_PASSIVE_VFX_3;
            }
        }
        this.color = new Color(MathUtils.random(0.0f, 1.0f), 0.3f, MathUtils.random(0.7f, 1.0f), 0.01f);
        this.renderBehind = false;
        this.duration = 2.0f;
        this.startingDuration = 2.0f;
        this.x = x;
        this.y = y;
        this.rotation = MathUtils.random(360.0f);
        this.scale = this.startingScale = MathUtils.random(1.2f, 1.8f) * Settings.scale;
        this.rotationSpeed = MathUtils.random(100.0f, 360.0f);
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.rotation += Gdx.graphics.getDeltaTime() * this.rotationSpeed;
        this.color.a = this.duration > 1.0f ? Interpolation.fade.apply(1.0f, 0.0f, this.duration - 1.0f) : Interpolation.fade.apply(0.0f, 1.0f, this.duration);
        this.scale = Interpolation.swingOut.apply(0.01f, this.startingScale, this.duration / 2.0f) * Settings.scale;
        if (this.scale < 0.0f || this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x - 37.0f, this.y - 37.0f, 37.0f, 37.0f, 74.0f, 74.0f, this.scale, this.scale, this.rotation, 0, 0, 74, 74, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


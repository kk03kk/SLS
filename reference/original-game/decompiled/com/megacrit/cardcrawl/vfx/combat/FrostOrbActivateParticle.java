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

public class FrostOrbActivateParticle
extends AbstractGameEffect {
    private Texture img = null;
    public static final int W = 64;
    private float cX;
    private float cY;
    private float sX;
    private float sY;
    private float tX;
    private float tY;
    private float tRotation;

    public FrostOrbActivateParticle(int index, float x, float y) {
        this.cX = x;
        this.cY = y;
        this.sX = this.cX;
        this.sY = this.cY;
        this.rotation = MathUtils.random(-5.0f, 5.0f);
        switch (index) {
            case 0: {
                this.tX = this.sX;
                this.tY = this.sY + 5.0f * Settings.scale;
                this.img = ImageMaster.FROST_ACTIVATE_VFX_1;
                this.tRotation = MathUtils.random(-5.0f, 5.0f);
                break;
            }
            case 1: {
                this.tX = this.sX - 10.0f * Settings.scale;
                this.tY = this.sY - 5.0f * Settings.scale;
                this.img = ImageMaster.FROST_ACTIVATE_VFX_2;
                this.tRotation = this.rotation + MathUtils.random(-30.0f, 30.0f);
                break;
            }
            default: {
                this.tX = this.sX + 10.0f * Settings.scale;
                this.tY = this.sY - 5.0f * Settings.scale;
                this.tRotation = this.rotation - MathUtils.random(-30.0f, 30.0f);
                this.img = ImageMaster.FROST_ACTIVATE_VFX_3;
            }
        }
        this.renderBehind = false;
        this.color = new Color(0.5f, 0.95f, 1.0f, 0.9f);
        this.scale = 2.0f * Settings.scale;
        this.duration = this.startingDuration = 0.3f;
    }

    @Override
    public void update() {
        this.color.a = Interpolation.pow2Out.apply(0.2f, 0.9f, this.duration / this.startingDuration);
        if (this.color.a < 0.0f) {
            this.color.a = 0.0f;
        }
        this.cX = Interpolation.swingIn.apply(this.tX, this.sX, this.duration / this.startingDuration);
        this.cY = Interpolation.swingIn.apply(this.tY, this.sY, this.duration / this.startingDuration);
        this.rotation = Interpolation.swingIn.apply(this.tRotation, 0.0f, this.duration / this.startingDuration);
        this.scale = Interpolation.fade.apply(2.4f, 2.0f, this.duration / this.startingDuration) * Settings.scale;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.cX - 32.0f, this.cY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, this.rotation, 0, 0, 64, 64, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


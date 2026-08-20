/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class DebuffParticleEffect
extends AbstractGameEffect {
    private Texture img;
    private static int IMG_NUM = 0;
    private boolean spinClockwise;
    private float x;
    private float y;
    private float scale = 0.0f;

    public DebuffParticleEffect(float x, float y) {
        this.x = x + MathUtils.random(-36.0f, 36.0f) * Settings.scale;
        this.y = y + MathUtils.random(-36.0f, 36.0f) * Settings.scale;
        this.duration = 4.0f;
        this.rotation = MathUtils.random(360.0f);
        this.spinClockwise = MathUtils.randomBoolean();
        if (IMG_NUM == 0) {
            this.renderBehind = true;
            this.img = ImageMaster.DEBUFF_VFX_1;
            ++IMG_NUM;
        } else if (IMG_NUM == 1) {
            this.img = ImageMaster.DEBUFF_VFX_3;
            ++IMG_NUM;
        } else {
            this.img = ImageMaster.DEBUFF_VFX_2;
            IMG_NUM = 0;
        }
        this.color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    }

    @Override
    public void update() {
        this.rotation = this.spinClockwise ? (this.rotation += Gdx.graphics.getDeltaTime() * 120.0f) : (this.rotation -= Gdx.graphics.getDeltaTime() * 120.0f);
        if (this.duration > 3.0f) {
            this.color.a = Interpolation.fade.apply(0.0f, 1.0f, 1.0f - (this.duration - 3.0f));
        } else if (!(this.duration > 1.0f)) {
            this.color.a = Interpolation.fade.apply(1.0f, 0.0f, 1.0f - this.duration);
            this.scale = Interpolation.fade.apply(Settings.scale, 0.0f, 1.0f - this.duration);
        }
        if (this.duration > 2.0f) {
            this.scale = Interpolation.bounceOut.apply(0.0f, Settings.scale, 2.0f - (this.duration - 2.0f));
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x - 16.0f, this.y - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, this.scale, this.scale, this.rotation, 0, 0, 32, 32, false, false);
    }

    @Override
    public void dispose() {
    }
}


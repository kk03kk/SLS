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
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class GiantEyeEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img;
    private float x;
    private float y;

    public GiantEyeEffect(float setX, float setY, Color setColor) {
        this.duration = 1.0f;
        this.startingDuration = 1.0f;
        this.color = setColor.cpy();
        this.color.a = 0.0f;
        this.img = ImageMaster.EYE_ANIM_0;
        this.x = setX - (float)this.img.packedWidth / 2.0f;
        this.y = setY - (float)this.img.packedHeight / 2.0f;
    }

    @Override
    public void update() {
        this.color.a = 1.0f - this.duration < 0.1f ? Interpolation.fade.apply(0.0f, 0.9f, (1.0f - this.duration) * 10.0f) : Interpolation.pow2Out.apply(0.0f, 0.9f, this.duration);
        if (this.duration > this.startingDuration * 0.85f) {
            this.img = ImageMaster.EYE_ANIM_0;
        } else if (this.duration > this.startingDuration * 0.8f) {
            this.img = ImageMaster.EYE_ANIM_1;
        } else if (this.duration > this.startingDuration * 0.75f) {
            this.img = ImageMaster.EYE_ANIM_2;
        } else if (this.duration > this.startingDuration * 0.7f) {
            this.img = ImageMaster.EYE_ANIM_3;
        } else if (this.duration > this.startingDuration * 0.65f) {
            this.img = ImageMaster.EYE_ANIM_4;
        } else if (this.duration > this.startingDuration * 0.6f) {
            this.img = ImageMaster.EYE_ANIM_5;
        } else if (this.duration > this.startingDuration * 0.55f) {
            this.img = ImageMaster.EYE_ANIM_6;
        } else if (this.duration > this.startingDuration * 0.38f) {
            this.img = ImageMaster.EYE_ANIM_5;
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, (this.scale + MathUtils.random(-0.02f, 0.02f)) * 3.0f, (this.scale + MathUtils.random(-0.03f, 0.03f)) * 3.0f, this.rotation);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, (this.scale + MathUtils.random(-0.02f, 0.02f)) * 3.0f, (this.scale + MathUtils.random(-0.03f, 0.03f)) * 3.0f, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


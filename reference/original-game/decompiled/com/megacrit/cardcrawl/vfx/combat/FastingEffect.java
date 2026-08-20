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

public class FastingEffect
extends AbstractGameEffect {
    private static TextureAtlas.AtlasRegion img;
    private float x;
    private float y;

    public FastingEffect(float x, float y, Color c) {
        if (img == null) {
            img = ImageMaster.WHITE_RING;
        }
        this.startingDuration = 1.0f;
        this.duration = 1.0f;
        this.scale = 3.0f * Settings.scale;
        this.color = c.cpy();
        this.color.a = 0.0f;
        this.rotation = MathUtils.random(0.0f, 360.0f);
        this.x = x - (float)FastingEffect.img.packedWidth / 2.0f;
        this.y = y - (float)FastingEffect.img.packedHeight / 2.0f;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.rotation -= Gdx.graphics.getDeltaTime() * 205.0f;
        if (this.duration > 0.5f) {
            this.color.a = Interpolation.fade.apply(0.45f, 0.0f, (this.duration - 0.5f) * 2.0f);
        } else {
            this.color.a = Interpolation.fade.apply(0.0f, 0.45f, this.duration * 2.0f);
            this.scale = Interpolation.swingOut.apply(0.0f, 3.0f * Settings.scale, this.duration * 2.0f);
        }
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(img, this.x, this.y, (float)FastingEffect.img.packedWidth / 2.0f, (float)FastingEffect.img.packedHeight / 2.0f, FastingEffect.img.packedWidth, FastingEffect.img.packedHeight, this.scale + MathUtils.random(-0.05f, 0.05f), this.scale + MathUtils.random(-0.05f, 0.05f), this.rotation);
        sb.draw(img, this.x, this.y, (float)FastingEffect.img.packedWidth / 2.0f, (float)FastingEffect.img.packedHeight / 2.0f, FastingEffect.img.packedWidth, FastingEffect.img.packedHeight, this.scale + MathUtils.random(-0.05f, 0.05f), this.scale + MathUtils.random(-0.05f, 0.05f), this.rotation + 180.0f);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


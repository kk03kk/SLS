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

public class BuffParticleEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img;
    private static final float DURATION = 0.5f;
    private float x;
    private float y;
    private float vY;
    private float scale = 0.0f;

    public BuffParticleEffect(float x, float y) {
        this.x = x + MathUtils.random(-25.0f, 25.0f) * Settings.scale;
        this.y = y + MathUtils.random(-20.0f, 10.0f) * Settings.scale;
        this.duration = 0.5f;
        this.rotation = MathUtils.random(-5.0f, 5.0f);
        switch (MathUtils.random(2)) {
            case 0: {
                this.img = ImageMaster.vfxAtlas.findRegion("buffVFX1");
                break;
            }
            case 1: {
                this.img = ImageMaster.vfxAtlas.findRegion("buffVFX2");
                break;
            }
            default: {
                this.img = ImageMaster.vfxAtlas.findRegion("buffVFX3");
            }
        }
        this.renderBehind = MathUtils.randomBoolean();
        this.vY = MathUtils.random(30.0f, 50.0f) * Settings.scale;
        this.color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
        this.scale = MathUtils.random(1.0f, 1.5f) * Settings.scale;
    }

    @Override
    public void update() {
        this.scale += Gdx.graphics.getDeltaTime() / 2.0f;
        if (this.duration > 0.5f) {
            this.color.a = Interpolation.fade.apply(0.0f, 1.0f, 1.0f - (this.duration - 3.0f));
        } else if (this.duration < 0.5f) {
            this.color.a = Interpolation.fade.apply(1.0f, 0.0f, 1.0f - this.duration * 2.0f);
        }
        this.y += Gdx.graphics.getDeltaTime() * this.vY;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x - (float)this.img.packedWidth / 2.0f, this.y - (float)this.img.packedHeight / 2.0f, this.img.offsetX, this.img.offsetY, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


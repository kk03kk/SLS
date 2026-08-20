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

public class IntenseZoomParticle
extends AbstractGameEffect {
    private float x;
    private float y;
    private TextureAtlas.AtlasRegion img;
    private Color color;
    private float offsetX;
    private float flickerDuration = 0.0f;
    private float lengthX;
    private float lengthY;
    private boolean isBlack = false;

    public IntenseZoomParticle(float x, float y, boolean isBlack) {
        int i = MathUtils.random(2);
        this.img = i == 0 ? ImageMaster.CONE_2 : (i == 1 ? ImageMaster.CONE_4 : ImageMaster.CONE_5);
        this.duration = 1.5f;
        this.isBlack = isBlack;
        this.color = isBlack ? Color.BLACK.cpy() : Settings.GOLD_COLOR.cpy();
        this.x = x;
        this.y = y - (float)this.img.packedHeight / 2.0f;
        this.randomize();
    }

    @Override
    public void update() {
        this.flickerDuration -= Gdx.graphics.getDeltaTime();
        if (this.flickerDuration < 0.0f) {
            this.randomize();
            this.flickerDuration = MathUtils.random(0.0f, 0.05f);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    public void randomize() {
        this.rotation = MathUtils.random(360.0f);
        this.offsetX = MathUtils.random(200.0f, 600.0f) * Settings.scale * (2.0f - this.duration);
        this.lengthX = MathUtils.random(1.0f, 1.3f);
        this.lengthY = MathUtils.random(0.9f, 1.2f);
        this.color.a = this.isBlack ? MathUtils.random(0.5f, 1.0f) * Interpolation.pow2Out.apply(this.duration / 1.5f) : MathUtils.random(0.2f, 0.7f) * Interpolation.pow2Out.apply(this.duration / 1.5f);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.isBlack) {
            sb.setBlendFunction(770, 1);
        }
        sb.setColor(this.color);
        sb.draw(this.img, this.x + this.offsetX, this.y, -this.offsetX, (float)this.img.packedHeight / 2.0f, (float)this.img.packedWidth * this.lengthX, (float)this.img.packedHeight * this.lengthY, this.scale, this.scale, this.rotation);
        if (!this.isBlack) {
            sb.setBlendFunction(770, 771);
        }
    }

    @Override
    public void dispose() {
    }
}


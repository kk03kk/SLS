/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class StarBounceEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img = ImageMaster.TINY_STAR;
    private static final float DUR = 1.0f;
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float floor;
    private static final float GRAVITY = 3000.0f * Settings.scale;

    public StarBounceEffect(float x, float y) {
        this.duration = MathUtils.random(0.5f, 1.0f);
        this.x = x - (float)(this.img.packedWidth / 2);
        this.y = y - (float)(this.img.packedHeight / 2);
        this.color = new Color(MathUtils.random(0.8f, 1.0f), MathUtils.random(0.6f, 0.8f), MathUtils.random(0.0f, 0.6f), 0.0f);
        this.color.a = 0.0f;
        this.rotation = MathUtils.random(0.0f, 360.0f);
        this.scale = MathUtils.random(0.5f, 2.0f) * Settings.scale;
        this.vX = MathUtils.random(-900.0f, 900.0f) * Settings.scale;
        this.vY = MathUtils.random(500.0f, 1000.0f) * Settings.scale;
        this.floor = MathUtils.random(100.0f, 250.0f) * Settings.scale;
    }

    @Override
    public void update() {
        this.vY -= GRAVITY / this.scale * Gdx.graphics.getDeltaTime();
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        Vector2 test = new Vector2(this.vX, this.vY);
        this.rotation = test.angle();
        if (this.y < this.floor) {
            this.vY = -this.vY * 0.75f;
            this.y = this.floor + 0.1f;
            this.vX *= 1.1f;
        }
        this.color.a = 1.0f - this.duration < 0.1f ? Interpolation.fade.apply(0.0f, 1.0f, (1.0f - this.duration) * 10.0f) : Interpolation.pow2Out.apply(0.0f, 1.0f, this.duration);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.8f, 1.2f), this.scale * MathUtils.random(0.8f, 1.2f), this.rotation);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.8f, 1.2f), this.scale * MathUtils.random(0.8f, 1.2f), this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


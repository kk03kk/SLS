/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class CeilingDustCloudEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vY;
    private float vX;
    private float vYAccel;
    private float aV;
    private float startingAlpha;
    private TextureAtlas.AtlasRegion img;

    public CeilingDustCloudEffect(float x, float y) {
        if (this.img == null) {
            this.img = ImageMaster.vfxAtlas.findRegion("env/dustCloud");
        }
        this.x = x + MathUtils.random(-40.0f, 40.0f) * Settings.scale - (float)this.img.packedWidth / 2.0f;
        this.y = y - (float)this.img.packedHeight / 2.0f;
        float randY = MathUtils.random(-10.0f, 10.0f) * Settings.scale;
        y += randY;
        this.renderBehind = randY < 0.0f;
        this.vY = MathUtils.random(0.0f, 20.0f) * Settings.scale;
        this.vX = MathUtils.random(-30.0f, 30.0f) * Settings.scale;
        this.duration = MathUtils.random(3.0f, 7.0f);
        this.scale = Settings.scale * MathUtils.random(0.1f, 0.7f);
        this.rotation = MathUtils.random(0.0f, 360.0f);
        float c = MathUtils.random(0.1f, 0.3f);
        this.color = new Color(c + 0.1f, c, c, 0.0f);
        this.startingAlpha = this.color.a = MathUtils.random(0.1f, 0.2f);
        this.aV = MathUtils.random(-0.1f, 0.1f);
    }

    @Override
    public void update() {
        this.rotation += this.aV;
        this.y -= this.vY * Gdx.graphics.getDeltaTime();
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.vY += this.vYAccel * Gdx.graphics.getDeltaTime();
        this.vX *= 0.99f;
        this.scale += Gdx.graphics.getDeltaTime() * 0.2f;
        if (this.duration < 3.0f) {
            this.color.a = Interpolation.fade.apply(this.startingAlpha, 0.0f, 1.0f - this.duration / 3.0f);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
    }

    @Override
    public void dispose() {
    }
}


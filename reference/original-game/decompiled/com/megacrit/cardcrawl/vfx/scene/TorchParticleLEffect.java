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

public class TorchParticleLEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vY;
    private TextureAtlas.AtlasRegion img;
    public static boolean renderGreen = false;

    public TorchParticleLEffect(float x, float y) {
        this.startingDuration = this.duration = MathUtils.random(1.5f, 3.0f);
        this.img = this.getImg();
        this.x = x - (float)(this.img.packedWidth / 2) + MathUtils.random(-3.0f, 3.0f) * Settings.scale;
        this.y = y - (float)(this.img.packedHeight / 2);
        this.scale = Settings.scale * MathUtils.random(2.0f, 3.0f);
        this.vY = MathUtils.random(1.0f, 10.0f);
        this.vY *= this.vY * Settings.scale;
        this.rotation = MathUtils.random(-20.0f, 20.0f);
        this.color = !renderGreen ? new Color(MathUtils.random(0.6f, 1.0f), MathUtils.random(0.3f, 0.6f), MathUtils.random(0.0f, 0.3f), 0.01f) : new Color(MathUtils.random(0.1f, 0.3f), MathUtils.random(0.5f, 0.9f), MathUtils.random(0.1f, 0.3f), 0.01f);
        this.renderBehind = true;
    }

    private TextureAtlas.AtlasRegion getImg() {
        switch (MathUtils.random(0, 2)) {
            case 0: {
                return ImageMaster.TORCH_FIRE_1;
            }
            case 1: {
                return ImageMaster.TORCH_FIRE_2;
            }
        }
        return ImageMaster.TORCH_FIRE_3;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.color.a = Interpolation.fade.apply(0.0f, 0.75f, this.duration / this.startingDuration);
        this.y += this.vY * Gdx.graphics.getDeltaTime();
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


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
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class NemesisFireParticle
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vY;
    private TextureAtlas.AtlasRegion img;

    public NemesisFireParticle(float x, float y) {
        this.startingDuration = this.duration = MathUtils.random(0.5f, 1.0f);
        this.img = this.getImg();
        this.x = x - (float)(this.img.packedWidth / 2);
        this.y = y - (float)(this.img.packedHeight / 2);
        this.scale = Settings.scale * MathUtils.random(0.25f, 0.5f);
        this.vY = MathUtils.random(1.0f, 10.0f) * Settings.scale;
        this.vY *= this.vY;
        this.rotation = MathUtils.random(-20.0f, 20.0f);
        this.color = new Color(0.1f, 0.2f, 0.1f, 0.01f);
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
        this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.duration / this.startingDuration);
        this.y += this.vY * Gdx.graphics.getDeltaTime();
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


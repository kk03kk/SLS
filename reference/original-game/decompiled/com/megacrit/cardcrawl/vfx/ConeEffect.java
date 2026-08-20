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

public class ConeEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float aV;
    private TextureAtlas.AtlasRegion img;

    public ConeEffect() {
        switch (MathUtils.random(1, 6)) {
            case 1: {
                this.img = ImageMaster.CONE_3;
                break;
            }
            default: {
                this.img = MathUtils.randomBoolean() ? ImageMaster.CONE_1 : ImageMaster.CONE_2;
            }
        }
        this.x = (float)Settings.WIDTH / 2.0f;
        this.y = (float)Settings.HEIGHT / 2.0f - (float)this.img.packedHeight / 2.0f;
        this.startingDuration = this.duration = MathUtils.random(2.0f, 5.0f);
        this.rotation = MathUtils.random(360.0f);
        this.aV = MathUtils.random(-10.0f, 10.0f);
        this.aV *= 2.0f;
        this.color = new Color(1.0f, MathUtils.random(0.7f, 0.8f), 0.2f, 0.0f);
        this.scale = Settings.scale;
    }

    @Override
    public void update() {
        this.rotation += this.aV * Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else if (this.startingDuration - this.duration < 1.0f) {
            this.color.a = (this.startingDuration - this.duration) / 3.0f;
        } else if (this.duration < 1.0f) {
            this.color.a = Interpolation.fade.apply(0.0f, 0.33f, this.duration);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, 0.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 1.1f, this.scale * 1.1f, this.rotation);
    }

    @Override
    public void dispose() {
    }
}


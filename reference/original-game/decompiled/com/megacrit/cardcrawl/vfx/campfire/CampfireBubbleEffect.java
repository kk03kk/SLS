/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.campfire;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class CampfireBubbleEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float aV;
    private boolean flipX = MathUtils.randomBoolean();
    private boolean flipY = MathUtils.randomBoolean();
    private TextureAtlas.AtlasRegion img;

    public CampfireBubbleEffect(boolean isLarge) {
        this.startingDuration = this.duration = MathUtils.random(10.0f, 20.0f);
        switch (MathUtils.random(2)) {
            case 0: {
                this.img = ImageMaster.SMOKE_1;
                break;
            }
            case 1: {
                this.img = ImageMaster.SMOKE_2;
                break;
            }
            default: {
                this.img = ImageMaster.SMOKE_3;
            }
        }
        this.x = MathUtils.random(-300.0f, 300.0f) * Settings.scale - (float)this.img.packedWidth / 2.0f;
        this.y = isLarge ? MathUtils.random(-200.0f, 230.0f) * Settings.scale - (float)this.img.packedHeight / 2.0f : MathUtils.random(0.0f, 230.0f) * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.aV = MathUtils.random(-30.0f, 30.0f);
        this.rotation = MathUtils.random(0.0f, 360.0f);
        float tmp = MathUtils.random(0.8f, 1.0f);
        this.color = new Color();
        this.color.r = tmp;
        this.color.g = tmp - 0.03f;
        this.color.b = tmp - 0.07f;
        this.scale = MathUtils.random(6.0f, 9.0f) * Settings.scale;
    }

    @Override
    public void update() {
        this.rotation += this.aV * Gdx.graphics.getDeltaTime();
        if (this.startingDuration - this.duration < 3.0f) {
            this.color.a = Interpolation.fade.apply(0.0f, 0.5f, (this.startingDuration - this.duration) / 3.0f);
        } else if (this.duration < 3.0f) {
            this.color.a = Interpolation.fade.apply(0.5f, 0.0f, 1.0f - this.duration / 3.0f);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb, float srcX, float srcY) {
        sb.setColor(this.color);
        if (this.flipX && !this.img.isFlipX()) {
            this.img.flip(true, false);
        } else if (!this.flipX && this.img.isFlipX()) {
            this.img.flip(true, false);
        }
        if (this.flipY && !this.img.isFlipY()) {
            this.img.flip(false, true);
        } else if (!this.flipY && this.img.isFlipY()) {
            this.img.flip(false, true);
        }
        sb.draw(this.img, srcX + this.x, srcY + this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void render(SpriteBatch sb) {
    }
}


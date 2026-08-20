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

public class BottomFogEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float aV;
    private boolean flipX = MathUtils.randomBoolean();
    private boolean flipY = MathUtils.randomBoolean();
    private TextureAtlas.AtlasRegion img;

    public BottomFogEffect(boolean renderBehind) {
        this.startingDuration = this.duration = MathUtils.random(10.0f, 12.0f);
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
        this.x = MathUtils.random(-200.0f, 2120.0f) * Settings.scale - (float)this.img.packedWidth / 2.0f;
        this.y = (float)Settings.HEIGHT / 2.0f + MathUtils.random(60.0f, 410.0f) * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.vX = MathUtils.random(-200.0f, 200.0f) * Settings.scale;
        this.aV = MathUtils.random(-10.0f, 10.0f);
        this.renderBehind = renderBehind;
        float tmp = MathUtils.random(0.1f, 0.15f);
        this.color = new Color();
        this.color.r = tmp + MathUtils.random(0.1f);
        this.color.g = tmp;
        this.color.b = this.color.r + MathUtils.random(0.05f);
        this.scale = MathUtils.random(4.0f, 6.0f) * Settings.scale;
    }

    @Override
    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.rotation += this.aV * Gdx.graphics.getDeltaTime();
        if (this.startingDuration - this.duration < 5.0f) {
            this.color.a = Interpolation.fade.apply(0.0f, 0.3f, (this.startingDuration - this.duration) / 5.0f);
        } else if (this.duration < 5.0f) {
            this.color.a = Interpolation.fade.apply(0.3f, 0.0f, 1.0f - this.duration / 5.0f);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        this.scale += Gdx.graphics.getDeltaTime() / 3.0f;
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb, float srcX, float srcY) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public void render(SpriteBatch sb) {
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
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
    }
}


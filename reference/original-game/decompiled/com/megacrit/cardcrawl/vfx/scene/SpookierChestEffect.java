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
import com.megacrit.cardcrawl.rewards.chests.AbstractChest;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class SpookierChestEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float aV;
    private boolean flipX = MathUtils.randomBoolean();
    private boolean flipY = MathUtils.randomBoolean();
    private TextureAtlas.AtlasRegion img;

    public SpookierChestEffect() {
        this.startingDuration = this.duration = 3.0f;
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
        this.x = AbstractChest.CHEST_LOC_X + MathUtils.random(-30.0f, 30.0f) * Settings.scale - (float)this.img.packedWidth / 2.0f;
        this.y = AbstractChest.CHEST_LOC_Y - MathUtils.random(120.0f, 190.0f) * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.vX = MathUtils.random(-120.0f, 120.0f) * Settings.scale;
        this.vY = MathUtils.random(-50.0f, 100.0f) * Settings.scale;
        this.aV = MathUtils.random(-150.0f, 150.0f);
        float tmp = MathUtils.random(0.3f, 0.9f);
        this.color = new Color();
        this.color.r = tmp * MathUtils.random(0.7f, 1.0f);
        this.color.g = tmp * MathUtils.random(0.5f, 0.9f);
        this.color.b = tmp;
        this.renderBehind = true;
        this.scale = MathUtils.random(0.5f, 3.0f) * Settings.scale;
    }

    @Override
    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.rotation += this.aV * Gdx.graphics.getDeltaTime();
        if (this.startingDuration - this.duration < 1.0f) {
            this.color.a = Interpolation.fade.apply(0.0f, 0.3f, (this.startingDuration - this.duration) / 1.0f);
        } else if (this.duration < 2.0f) {
            this.color.a = Interpolation.fade.apply(0.3f, 0.0f, 1.0f - this.duration / 2.0f);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        this.scale += Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
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

    @Override
    public void dispose() {
    }
}


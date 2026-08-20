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

public class IroncladVictoryFlameEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float vY;
    private boolean flipX = MathUtils.randomBoolean();
    private TextureAtlas.AtlasRegion img;

    public IroncladVictoryFlameEffect() {
        this.startingDuration = this.duration = 1.0f;
        this.renderBehind = MathUtils.randomBoolean();
        switch (MathUtils.random(2)) {
            case 0: {
                this.img = ImageMaster.FLAME_1;
                break;
            }
            case 1: {
                this.img = ImageMaster.FLAME_2;
                break;
            }
            default: {
                this.img = ImageMaster.FLAME_3;
            }
        }
        this.x = MathUtils.random(600.0f, 1320.0f) * Settings.xScale - (float)this.img.packedWidth / 2.0f;
        this.y = -300.0f * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.vX = this.x > 960.0f * Settings.xScale ? MathUtils.random(0.0f, -120.0f) * Settings.xScale : MathUtils.random(120.0f, 0.0f) * Settings.xScale;
        this.vY = MathUtils.random(600.0f, 800.0f) * Settings.scale;
        this.color = new Color(MathUtils.random(0.4f, 0.8f), MathUtils.random(0.1f, 0.4f), MathUtils.random(0.4f, 0.9f), 0.8f);
        this.renderBehind = false;
        this.scale = MathUtils.random(6.0f, 7.0f) * Settings.scale;
    }

    @Override
    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.color.a = Interpolation.pow3Out.apply(0.0f, 0.8f, this.duration / this.startingDuration);
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
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
    }

    @Override
    public void dispose() {
    }
}


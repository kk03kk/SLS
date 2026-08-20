/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class PetalEffect
extends AbstractGameEffect {
    private float x = MathUtils.random(100.0f * Settings.scale, 1820.0f * Settings.scale);
    private float y = (float)Settings.HEIGHT + MathUtils.random(20.0f, 300.0f) * Settings.scale;
    private float vY;
    private float vX;
    private float scaleY;
    private int frame = MathUtils.random(8);
    private float animTimer = 0.05f;
    private static final int W = 32;

    public PetalEffect() {
        this.rotation = MathUtils.random(-10.0f, 10.0f);
        this.scale = MathUtils.random(1.0f, 2.5f);
        this.scaleY = MathUtils.random(1.0f, 1.2f);
        if (this.scale < 1.5f) {
            this.renderBehind = true;
        }
        this.vY = MathUtils.random(200.0f, 300.0f) * this.scale * Settings.scale;
        this.vX = MathUtils.random(-100.0f, 100.0f) * this.scale * Settings.scale;
        this.scale *= Settings.scale;
        if (MathUtils.randomBoolean()) {
            this.rotation += 180.0f;
        }
        this.color = new Color(1.0f, MathUtils.random(0.7f, 0.9f), MathUtils.random(0.7f, 0.9f), 1.0f);
        this.duration = 4.0f;
    }

    @Override
    public void update() {
        this.y -= this.vY * Gdx.graphics.getDeltaTime();
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.animTimer -= Gdx.graphics.getDeltaTime() / this.scale;
        if (this.animTimer < 0.0f) {
            this.animTimer += 0.05f;
            ++this.frame;
            if (this.frame > 11) {
                this.frame = 0;
            }
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else if (this.duration < 1.0f) {
            this.color.a = this.duration;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        switch (this.frame) {
            case 0: {
                this.renderImg(sb, ImageMaster.PETAL_VFX[0], false, false);
                break;
            }
            case 1: {
                this.renderImg(sb, ImageMaster.PETAL_VFX[1], false, false);
                break;
            }
            case 2: {
                this.renderImg(sb, ImageMaster.PETAL_VFX[2], false, false);
                break;
            }
            case 3: {
                this.renderImg(sb, ImageMaster.PETAL_VFX[3], false, false);
                break;
            }
            case 4: {
                this.renderImg(sb, ImageMaster.PETAL_VFX[2], true, true);
                break;
            }
            case 5: {
                this.renderImg(sb, ImageMaster.PETAL_VFX[1], true, true);
                break;
            }
            case 6: {
                this.renderImg(sb, ImageMaster.PETAL_VFX[0], true, true);
                break;
            }
            case 7: {
                this.renderImg(sb, ImageMaster.PETAL_VFX[1], true, true);
                break;
            }
            case 8: {
                this.renderImg(sb, ImageMaster.PETAL_VFX[2], true, true);
                break;
            }
            case 9: {
                this.renderImg(sb, ImageMaster.PETAL_VFX[3], true, true);
                break;
            }
            case 10: {
                this.renderImg(sb, ImageMaster.PETAL_VFX[2], false, false);
                break;
            }
            case 11: {
                this.renderImg(sb, ImageMaster.PETAL_VFX[1], false, false);
                break;
            }
        }
    }

    @Override
    public void dispose() {
    }

    private void renderImg(SpriteBatch sb, Texture img, boolean flipH, boolean flipV) {
        sb.setBlendFunction(770, 1);
        sb.draw(img, this.x, this.y, 16.0f, 16.0f, 32.0f, 32.0f, this.scale, this.scale * this.scaleY, this.rotation, 0, 0, 32, 32, flipH, flipV);
        sb.setBlendFunction(770, 771);
    }
}


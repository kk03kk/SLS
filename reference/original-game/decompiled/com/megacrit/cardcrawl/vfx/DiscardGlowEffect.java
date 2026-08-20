/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class DiscardGlowEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 0.2f;
    private float effectDuration;
    private float x;
    private float y;
    private float vY;
    private float rotator;
    private float scaleJitter = 1.0f;
    private static final float SCALE_JITTER_AMT = 0.1f;
    private Color shadowColor = Color.BLACK.cpy();
    private TextureAtlas.AtlasRegion img = this.getImg();
    private boolean isAdditive;

    public DiscardGlowEffect(boolean isAbove) {
        this.setPosition(isAbove);
        this.x -= (float)(this.img.packedWidth / 2);
        this.y -= (float)(this.img.packedHeight / 2);
        this.duration = this.effectDuration = MathUtils.random(0.4f, 0.9f);
        this.startingDuration = this.effectDuration;
        this.scaleJitter = MathUtils.random(this.scaleJitter - 0.1f, this.scaleJitter + 0.1f);
        this.vY = MathUtils.random(30.0f * Settings.scale, 60.0f * Settings.scale);
        this.color = Settings.DISCARD_COLOR.cpy();
        this.color.r -= MathUtils.random(0.0f, 0.1f);
        this.color.g += MathUtils.random(0.0f, 0.1f);
        this.color.b += MathUtils.random(0.0f, 0.1f);
        this.isAdditive = MathUtils.randomBoolean();
        this.rotator = MathUtils.random(-180.0f, 180.0f);
    }

    private TextureAtlas.AtlasRegion getImg() {
        int roll = MathUtils.random(0, 5);
        switch (roll) {
            case 0: {
                return ImageMaster.DECK_GLOW_1;
            }
            case 1: {
                return ImageMaster.DECK_GLOW_2;
            }
            case 2: {
                return ImageMaster.DECK_GLOW_3;
            }
            case 3: {
                return ImageMaster.DECK_GLOW_4;
            }
            case 4: {
                return ImageMaster.DECK_GLOW_5;
            }
        }
        return ImageMaster.DECK_GLOW_6;
    }

    private void setPosition(boolean isAbove) {
        int roll = MathUtils.random(0, 9);
        if (isAbove) {
            switch (roll) {
                case 0: {
                    this.x = 1886.0f * Settings.scale;
                    this.y = 86.0f * Settings.scale;
                    return;
                }
                case 1: {
                    this.x = 1883.0f * Settings.scale;
                    this.y = 80.0f * Settings.scale;
                    return;
                }
                case 2: {
                    this.x = 1881.0f * Settings.scale;
                    this.y = 67.0f * Settings.scale;
                    return;
                }
                case 3: {
                    this.x = 1876.0f * Settings.scale;
                    this.y = 54.0f * Settings.scale;
                    return;
                }
                case 4: {
                    this.x = 1873.0f * Settings.scale;
                    this.y = 45.0f * Settings.scale;
                    return;
                }
                case 5: {
                    this.x = 1865.0f * Settings.scale;
                    this.y = 36.0f * Settings.scale;
                    return;
                }
                case 6: {
                    this.x = 1849.0f * Settings.scale;
                    this.y = 32.0f * Settings.scale;
                    return;
                }
                case 7: {
                    this.x = 1841.0f * Settings.scale;
                    this.y = 36.0f * Settings.scale;
                    return;
                }
                case 8: {
                    this.x = 1830.0f * Settings.scale;
                    this.y = 36.0f * Settings.scale;
                    return;
                }
            }
            this.x = 1819.0f * Settings.scale;
            this.y = 43.0f * Settings.scale;
            return;
        }
        switch (roll) {
            case 0: {
                this.x = 1810.0f * Settings.scale;
                this.y = 84.0f * Settings.scale;
                return;
            }
            case 1: {
                this.x = 1820.0f * Settings.scale;
                this.y = 88.0f * Settings.scale;
                return;
            }
            case 2: {
                this.x = 1830.0f * Settings.scale;
                this.y = 94.0f * Settings.scale;
                return;
            }
            case 3: {
                this.x = 1834.0f * Settings.scale;
                this.y = 96.0f * Settings.scale;
                return;
            }
            case 4: {
                this.x = 1837.0f * Settings.scale;
                this.y = 96.0f * Settings.scale;
                return;
            }
            case 5: {
                this.x = 1841.0f * Settings.scale;
                this.y = 98.0f * Settings.scale;
                return;
            }
            case 6: {
                this.x = 1854.0f * Settings.scale;
                this.y = 99.0f * Settings.scale;
                return;
            }
            case 7: {
                this.x = 1859.0f * Settings.scale;
                this.y = 91.0f * Settings.scale;
                return;
            }
            case 8: {
                this.x = 1871.0f * Settings.scale;
                this.y = 87.0f * Settings.scale;
                return;
            }
        }
        this.x = 1877.0f * Settings.scale;
        this.y = 84.0f * Settings.scale;
    }

    @Override
    public void update() {
        this.rotation += this.rotator * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.1f) {
            this.scale = Settings.scale * this.duration / this.effectDuration * 2.0f + Settings.scale / 2.0f;
        }
        if (this.duration < 0.25f) {
            this.color.a = this.duration * 4.0f;
        }
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.shadowColor.a = this.color.a / 2.0f;
    }

    @Override
    public void render(SpriteBatch sb, float x2, float y2) {
        if (this.isAdditive) {
            sb.setBlendFunction(770, 1);
        }
        sb.setColor(this.color);
        sb.draw(this.img, this.x + x2, this.y + y2, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * this.scaleJitter, this.scale * this.scaleJitter, this.rotation);
        if (this.isAdditive) {
            sb.setBlendFunction(770, 771);
        }
    }

    @Override
    public void dispose() {
    }

    @Override
    public void render(SpriteBatch sb) {
    }
}


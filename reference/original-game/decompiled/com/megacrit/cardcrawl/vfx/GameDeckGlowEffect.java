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

public class GameDeckGlowEffect
extends AbstractGameEffect {
    private float effectDuration;
    private float x;
    private float y;
    private float vY;
    private float vX;
    private float rotator;
    private boolean flipY;
    private boolean flipX;
    private Color shadowColor = Color.BLACK.cpy();
    private TextureAtlas.AtlasRegion img;

    public GameDeckGlowEffect(boolean isAbove) {
        this.duration = this.effectDuration = MathUtils.random(2.0f, 5.0f);
        this.startingDuration = this.effectDuration;
        this.vY = MathUtils.random(10.0f * Settings.scale, 20.0f * Settings.scale);
        this.vX = MathUtils.random(10.0f * Settings.scale, 20.0f * Settings.scale);
        this.flipY = MathUtils.randomBoolean();
        this.flipX = MathUtils.randomBoolean();
        this.color = Settings.CREAM_COLOR.cpy();
        float darkness = MathUtils.random(0.1f, 0.4f);
        this.color.r -= darkness;
        this.color.g -= darkness;
        this.color.b -= darkness;
        this.img = this.getImg();
        this.x = MathUtils.random(35.0f, 85.0f) * Settings.scale - (float)(this.img.packedWidth / 2);
        this.y = MathUtils.random(35.0f, 85.0f) * Settings.scale - (float)(this.img.packedHeight / 2);
        this.scale = Settings.scale * 0.75f;
        this.rotator = MathUtils.random(-120.0f, 120.0f);
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

    @Override
    public void update() {
        this.rotation += this.rotator * Gdx.graphics.getDeltaTime();
        if (this.vY != 0.0f) {
            this.y = this.flipY ? (this.y += this.vY * Gdx.graphics.getDeltaTime()) : (this.y -= this.vY * Gdx.graphics.getDeltaTime());
            this.vY = MathUtils.lerp(this.vY, 0.0f, Gdx.graphics.getDeltaTime() / 4.0f);
            if (this.vY < 0.5f) {
                this.vY = 0.0f;
            }
        }
        if (this.vX != 0.0f) {
            this.x = this.flipX ? (this.x += this.vX * Gdx.graphics.getDeltaTime()) : (this.x -= this.vX * Gdx.graphics.getDeltaTime());
            this.vX = MathUtils.lerp(this.vX, 0.0f, Gdx.graphics.getDeltaTime() / 4.0f);
            if (this.vX < 0.5f) {
                this.vX = 0.0f;
            }
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        this.color.a = this.duration / this.effectDuration;
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.shadowColor.a = this.color.a / 2.0f;
    }

    @Override
    public void render(SpriteBatch sb, float x2, float y2) {
        if (this.img != null) {
            sb.setColor(this.color);
            sb.draw(this.img, this.x + x2, this.y + y2, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        }
    }

    @Override
    public void dispose() {
    }

    @Override
    public void render(SpriteBatch sb) {
    }
}


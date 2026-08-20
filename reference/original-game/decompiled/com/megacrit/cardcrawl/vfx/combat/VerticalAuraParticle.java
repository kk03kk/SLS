/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class VerticalAuraParticle
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vY;
    private static final float FADE_IN_TIME = 0.2f;
    private static final float FADE_OUT_TIME = 0.8f;
    private float fadeInTimer = 0.2f;
    private float fadeOutTimer = 0.8f;
    private float stallTimer;
    private TextureAtlas.AtlasRegion img = ImageMaster.VERTICAL_AURA;

    public VerticalAuraParticle(Color c, float x, float y) {
        this.color = c.cpy();
        this.randomizeColor(this.color, 0.1f);
        this.color.a = 0.0f;
        this.x = x + MathUtils.random(-200.0f, 200.0f) * Settings.scale - (float)this.img.packedWidth / 2.0f;
        this.y = y + MathUtils.random(-200.0f, 200.0f) * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.vY = MathUtils.random(-300.0f, 300.0f) * Settings.scale;
        this.stallTimer = MathUtils.random(0.0f, 0.2f);
        this.scale = MathUtils.random(0.6f, 1.7f) * Settings.scale;
        this.renderBehind = true;
    }

    @Override
    public void update() {
        if (this.stallTimer > 0.0f) {
            this.stallTimer -= Gdx.graphics.getDeltaTime();
            return;
        }
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        if (this.fadeInTimer != 0.0f) {
            this.fadeInTimer -= Gdx.graphics.getDeltaTime();
            if (this.fadeInTimer < 0.0f) {
                this.fadeInTimer = 0.0f;
            }
            this.color.a = Interpolation.fade.apply(0.5f, 0.0f, this.fadeInTimer / 0.2f);
        } else if (this.fadeOutTimer != 0.0f) {
            this.fadeOutTimer -= Gdx.graphics.getDeltaTime();
            if (this.fadeOutTimer < 0.0f) {
                this.fadeOutTimer = 0.0f;
            }
            this.color.a = Interpolation.fade.apply(0.0f, 0.5f, this.fadeOutTimer / 0.8f);
        } else {
            this.isDone = true;
        }
    }

    private void randomizeColor(Color c, float amt) {
        float r = c.r + MathUtils.random(-amt, amt);
        float g = c.g + MathUtils.random(-amt, amt);
        float b = c.b + MathUtils.random(-amt, amt);
        if (r > 1.0f) {
            r = 1.0f;
        } else if (r < 0.0f) {
            r = 0.0f;
        }
        if (g > 1.0f) {
            g = 1.0f;
        } else if (g < 0.0f) {
            g = 0.0f;
        }
        if (b > 1.0f) {
            b = 1.0f;
        } else if (b < 0.0f) {
            b = 0.0f;
        }
        c.r = r;
        c.g = g;
        c.b = b;
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


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class WindyParticleEffect
extends AbstractGameEffect {
    private float scaleY;
    private float x;
    private float y;
    private float vX;
    private float vY;

    public WindyParticleEffect(Color setColor, boolean reverse) {
        if (!reverse) {
            this.x = MathUtils.random(-400.0f, -100.0f) * Settings.scale - 128.0f;
            this.vX = MathUtils.random(1500.0f, 2500.0f) * Settings.scale;
        } else {
            this.x = (float)Settings.WIDTH + MathUtils.random(400.0f, 100.0f) * Settings.scale - 128.0f;
            this.vX = MathUtils.random(-1500.0f, -2500.0f) * Settings.scale;
        }
        this.y = MathUtils.random(0.15f, 0.85f) * (float)Settings.HEIGHT - 128.0f;
        this.vY = MathUtils.random(-100.0f, 100.0f) * Settings.scale;
        this.duration = 2.0f;
        this.scale = MathUtils.random(1.5f, 3.0f);
        this.vX *= this.scale;
        this.scale *= Settings.scale;
        this.scaleY = MathUtils.random(0.5f, 2.0f) * Settings.scale;
        this.color = setColor.cpy();
        this.color.a = MathUtils.random(0.5f, 1.0f);
        if (this.scaleY < 1.0f * Settings.scale) {
            this.renderBehind = true;
        }
    }

    public WindyParticleEffect() {
        this.x = MathUtils.random(-400.0f, -100.0f) * Settings.scale - 128.0f;
        this.y = MathUtils.random(0.15f, 0.85f) * (float)Settings.HEIGHT - 128.0f;
        this.vX = MathUtils.random(1500.0f, 2500.0f) * Settings.scale;
        this.vY = MathUtils.random(-100.0f, 100.0f) * Settings.scale;
        this.duration = 2.0f;
        this.scale = MathUtils.random(1.5f, 3.0f);
        this.vX *= this.scale;
        this.scale *= Settings.scale;
        this.scaleY = MathUtils.random(0.5f, 2.0f) * Settings.scale;
        this.color = new Color(0.9f, 0.9f, 1.0f, MathUtils.random(0.5f, 1.0f));
        if (this.scaleY < 1.0f * Settings.scale) {
            this.renderBehind = true;
        }
    }

    @Override
    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(ImageMaster.HORIZONTAL_LINE, this.x, this.y, 128.0f, 128.0f, 256.0f, 256.0f, this.scale * MathUtils.random(0.7f, 1.3f), this.scaleY * MathUtils.random(0.7f, 1.3f), this.rotation, 0, 0, 256, 256, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


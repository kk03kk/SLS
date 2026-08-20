/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class UnknownParticleEffect
extends AbstractGameEffect {
    private Texture img;
    private static final int RAW_W = 128;
    private static final float DURATION = 1.5f;
    private static int renderNum = 0;
    private float x;
    private float y;
    private float scale;
    private float targetScale;

    public UnknownParticleEffect(float x, float y) {
        this.duration = 1.5f;
        if (renderNum == 0) {
            this.targetScale = Settings.scale * 0.8f;
            this.rotation = 24.0f;
            this.x = x - 24.0f * Settings.scale;
            this.y = y - MathUtils.random(6.0f, 10.0f) * Settings.scale;
            this.color = MathUtils.randomBoolean() ? Color.GOLDENROD.cpy() : Color.GOLD.cpy();
            this.renderBehind = true;
        } else if (renderNum == 1) {
            this.targetScale = Settings.scale * 1.2f;
            this.rotation = 0.0f;
            this.x = x;
            this.y = y;
            this.color = Color.WHITE.cpy();
            this.renderBehind = false;
        } else {
            this.targetScale = Settings.scale * 0.8f;
            this.rotation = -24.0f;
            this.x = x + 24.0f * Settings.scale;
            this.y = y - MathUtils.random(6.0f, 10.0f) * Settings.scale;
            this.color = MathUtils.randomBoolean() ? Color.GOLDENROD.cpy() : Color.GOLD.cpy();
            this.renderBehind = true;
        }
        this.scale = 0.01f;
        if (++renderNum > 2) {
            renderNum = 0;
        }
        this.img = ImageMaster.INTENT_UNKNOWN_L;
    }

    @Override
    public void update() {
        if (this.duration > 0.5f) {
            this.scale = Interpolation.elasticOut.apply(0.01f, this.targetScale, 1.5f - this.duration);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else if (this.duration < 0.5f) {
            this.color.a = this.duration * 2.0f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x - 64.0f, this.y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
    }

    @Override
    public void dispose() {
    }
}


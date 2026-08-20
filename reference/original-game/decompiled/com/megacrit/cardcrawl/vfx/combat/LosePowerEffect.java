/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class LosePowerEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 1.0f;
    private float x;
    private float y;
    private float shake_offset;
    private float offset_y;
    private Texture img;
    private static final float IMG_WIDTH = 64.0f * Settings.scale;
    private float shakeVelo;
    private float popVelo;
    private static final float POP_VELO_START = 150.0f * Settings.scale;
    private static final float SHAKE_VELO_START = 200.0f * Settings.scale;
    private static final float SHAKE_DIST = 16.0f * Settings.scale;
    private static final float FALL_SPEED = 8.0f * Settings.scale;
    private static final float SHAKE_TAPER_SPEED = 30.0f * Settings.scale;
    private boolean shakeLeft = true;

    public LosePowerEffect(float x, float y, Texture img) {
        this.img = img;
        this.duration = 1.0f;
        this.startingDuration = 1.0f;
        this.x = x;
        this.y = y;
        this.shake_offset = 0.0f;
        this.offset_y = 0.0f;
        this.shakeVelo = SHAKE_VELO_START;
        this.popVelo = POP_VELO_START;
        this.color = Color.WHITE.cpy();
    }

    @Override
    public void update() {
        if (this.duration == this.startingDuration) {
            // empty if block
        }
        this.offset_y += this.popVelo * Gdx.graphics.getDeltaTime();
        this.popVelo -= FALL_SPEED;
        this.shakeVelo -= Gdx.graphics.getDeltaTime() * SHAKE_TAPER_SPEED;
        if (this.shakeLeft) {
            this.shake_offset -= this.shakeVelo * Gdx.graphics.getDeltaTime();
            if (this.shake_offset < -SHAKE_DIST) {
                this.shakeLeft = !this.shakeLeft;
            }
        } else {
            this.shake_offset += this.shakeVelo * Gdx.graphics.getDeltaTime();
            if (this.shake_offset > SHAKE_DIST) {
                boolean bl = this.shakeLeft = !this.shakeLeft;
            }
        }
        if (this.color.g > 0.3f) {
            this.color.g -= Gdx.graphics.getDeltaTime();
            this.color.b -= Gdx.graphics.getDeltaTime();
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration > 1.0f) {
            this.color.a = (1.5f - this.duration) * 2.0f;
        } else if (this.duration < 1.0f && this.duration > 0.33f) {
            this.color.a = 1.0f;
        } else if (this.duration < 0.33f && this.duration > 0.0f) {
            this.color.a = this.duration * 3.0f;
        } else if (this.duration < 0.0f) {
            this.isDone = true;
            this.color.a = 0.0f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.isDone) {
            sb.setColor(this.color);
            sb.draw(this.img, this.x - IMG_WIDTH * 0.25f + this.shake_offset, this.y - IMG_WIDTH * 0.25f + this.offset_y, IMG_WIDTH * 1.5f, IMG_WIDTH * 1.5f);
        }
    }

    @Override
    public void dispose() {
    }
}


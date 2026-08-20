/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class AnimatedSlashEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float sX;
    private float sY;
    private float tX;
    private float tY;
    private float scaleX;
    private float scaleY;
    private float targetScale;
    private Color color2;

    public AnimatedSlashEffect(float x, float y, float dX, float dY, float angle, Color color1, Color color2) {
        this(x, y, dX, dY, angle, 2.0f, color1, color2);
    }

    public AnimatedSlashEffect(float x, float y, float dX, float dY, float angle, float targetScale, Color color1, Color color2) {
        this.x = x - 64.0f - dX / 2.0f * Settings.scale;
        this.y = y - 64.0f - dY / 2.0f * Settings.scale;
        this.sX = this.x;
        this.sY = this.y;
        this.tX = this.x + dX / 2.0f * Settings.scale;
        this.tY = this.y + dY / 2.0f * Settings.scale;
        this.color = color1.cpy();
        this.color2 = color2.cpy();
        this.color.a = 0.0f;
        this.duration = this.startingDuration = 0.4f;
        this.targetScale = targetScale;
        this.scaleX = 0.01f;
        this.scaleY = 0.01f;
        this.rotation = -135.0f;
        this.rotation = angle;
    }

    @Override
    public void update() {
        if (this.duration > this.startingDuration / 2.0f) {
            this.color.a = Interpolation.exp10In.apply(0.8f, 0.0f, (this.duration - this.startingDuration / 2.0f) / (this.startingDuration / 2.0f));
            this.scaleY = this.scaleX = Interpolation.exp10In.apply(this.targetScale, 0.1f, (this.duration - this.startingDuration / 2.0f) / (this.startingDuration / 2.0f));
            this.x = Interpolation.fade.apply(this.tX, this.sX, (this.duration - this.startingDuration / 2.0f) / (this.startingDuration / 2.0f));
            this.y = Interpolation.fade.apply(this.tY, this.sY, (this.duration - this.startingDuration / 2.0f) / (this.startingDuration / 2.0f));
        } else {
            this.scaleX = Interpolation.pow2In.apply(0.5f, this.targetScale, this.duration / (this.startingDuration / 2.0f));
            this.color.a = Interpolation.pow5In.apply(0.0f, 0.8f, this.duration / (this.startingDuration / 2.0f));
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color2);
        sb.setBlendFunction(770, 1);
        sb.draw(ImageMaster.ANIMATED_SLASH_VFX, this.x, this.y, 64.0f, 64.0f, 128.0f, 128.0f, this.scaleX * 0.4f * MathUtils.random(0.95f, 1.05f) * Settings.scale, this.scaleY * 0.7f * MathUtils.random(0.95f, 1.05f) * Settings.scale, this.rotation, 0, 0, 128, 128, false, false);
        sb.setColor(this.color);
        sb.draw(ImageMaster.ANIMATED_SLASH_VFX, this.x, this.y, 64.0f, 64.0f, 128.0f, 128.0f, this.scaleX * 0.7f * MathUtils.random(0.95f, 1.05f) * Settings.scale, this.scaleY * MathUtils.random(0.95f, 1.05f) * Settings.scale, this.rotation, 0, 0, 128, 128, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


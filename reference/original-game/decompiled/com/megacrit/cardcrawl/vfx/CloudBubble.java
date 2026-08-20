/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.FloatyEffect;

public class CloudBubble {
    private static final int RAW_W = 128;
    private float x;
    private float y;
    private float fadeInTime;
    private float fadeInTimer;
    private float scale = 0.01f;
    private float targetScale;
    private boolean fadingIn = true;
    private FloatyEffect f_effect;
    private Color color;
    private Texture img;
    private boolean killed = false;
    private float killSpeed;

    public CloudBubble(float x, float y, float targetScale) {
        this.x = x;
        this.y = y;
        this.targetScale = targetScale * Settings.scale;
        this.fadeInTimer = this.fadeInTime = MathUtils.random(0.7f, 2.5f);
        this.f_effect = new FloatyEffect(this.targetScale * 3.0f, 1.0f);
        float darkness = MathUtils.random(0.8f, 0.9f);
        this.color = new Color(darkness, darkness - 0.04f, darkness - 0.05f, 1.0f);
        if (targetScale > 0.5f) {
            this.img = ImageMaster.LARGE_CLOUD;
        } else {
            this.img = ImageMaster.SMALL_CLOUD;
            this.targetScale *= 3.0f;
        }
        this.killSpeed = MathUtils.random(8.0f, 24.0f);
    }

    public void update() {
        this.f_effect.update();
        if (this.fadingIn) {
            this.fadeInTimer -= Gdx.graphics.getDeltaTime();
            if (this.fadeInTimer < 0.0f) {
                this.fadeInTimer = 0.0f;
                this.fadingIn = false;
            }
            this.scale = Interpolation.swingIn.apply(this.targetScale, 0.0f, this.fadeInTimer / this.fadeInTime);
        }
        if (this.killed) {
            this.color.a = MathUtils.lerp(this.color.a, 0.0f, Gdx.graphics.getDeltaTime() * this.killSpeed);
        }
    }

    public void kill() {
        this.killed = true;
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x - 64.0f + this.f_effect.x, this.y - 64.0f + this.f_effect.y, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, 0.0f, 0, 0, 128, 128, false, false);
    }
}


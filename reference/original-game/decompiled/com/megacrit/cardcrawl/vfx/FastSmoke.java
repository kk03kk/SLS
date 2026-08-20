/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;

public class FastSmoke {
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float rotation;
    private float fadeInTime;
    private float fadeInTimer;
    private float scale = 0.01f;
    private float targetScale = MathUtils.random(1.0f, 1.5f) * Settings.scale;
    private boolean fadingIn = true;
    private Color color;
    private TextureAtlas.AtlasRegion img;
    private boolean killed = false;
    private float killSpeed;
    private float decelerateY;

    public FastSmoke(float x, float y) {
        this.fadeInTimer = this.fadeInTime = MathUtils.random(1.0f, 1.5f);
        float darkness = MathUtils.random(0.4f, 0.9f);
        this.color = new Color(darkness + 0.1f, darkness + 0.1f, darkness + 0.05f, 1.0f);
        if (this.targetScale > 0.5f) {
            this.img = ImageMaster.EXHAUST_L;
        } else {
            this.img = ImageMaster.EXHAUST_S;
            this.vX /= 3.0f;
        }
        this.x = x + MathUtils.random(-75.0f, 75.0f) * Settings.scale - (float)this.img.packedWidth / 2.0f;
        this.y = y + MathUtils.random(-75.0f, 75.0f) * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.vY = MathUtils.random(50.0f * Settings.scale, 400.0f * Settings.scale);
        this.vX = MathUtils.random(-140.0f * Settings.scale, 140.0f * Settings.scale);
        this.rotation = MathUtils.random(360.0f);
        this.killSpeed = MathUtils.random(1.0f, 4.0f);
        this.decelerateY = MathUtils.random(1.0f * Settings.scale, 3.0f * Settings.scale);
    }

    public void update() {
        if (this.fadingIn) {
            this.fadeInTimer -= Gdx.graphics.getDeltaTime();
            if (this.fadeInTimer < 0.0f) {
                this.fadeInTimer = 0.0f;
                this.fadingIn = false;
            }
            this.scale = Interpolation.swingIn.apply(this.targetScale, 0.01f, this.fadeInTimer / this.fadeInTime);
        }
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.vY -= Gdx.graphics.getDeltaTime() * this.decelerateY;
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.rotation += this.vX * 2.0f * Gdx.graphics.getDeltaTime();
        if (this.vY < 0.0f) {
            this.vY = 0.0f;
        }
        if (this.killed) {
            this.color.a -= this.killSpeed * Gdx.graphics.getDeltaTime();
            if (this.color.a < 0.0f) {
                this.color.a = 0.0f;
            }
            this.scale += 5.0f * Gdx.graphics.getDeltaTime();
        }
    }

    public void kill() {
        this.killed = true;
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
    }
}


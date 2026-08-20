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
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class WaterSplashParticleEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img = ImageMaster.DECK_GLOW_1;
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float floor;

    public WaterSplashParticleEffect(float x, float y) {
        this.duration = MathUtils.random(0.5f, 1.0f);
        this.x = x - (float)(this.img.packedWidth / 2) + MathUtils.random(-10.0f, 10.0f) * Settings.scale;
        this.y = y - (float)(this.img.packedHeight / 2) - 40.0f * Settings.scale;
        this.color = new Color(1.0f, 0.2f, 0.1f, 0.0f);
        this.color.a = 0.0f;
        this.scale = MathUtils.random(1.5f, 3.5f) * Settings.scale;
        this.vX = MathUtils.random(-120.0f, 120.0f) * Settings.scale;
        this.vY = MathUtils.random(150.0f, 300.0f) * Settings.scale;
        this.floor = y - 40.0f * Settings.scale;
    }

    @Override
    public void update() {
        this.vY -= 1000.0f * Settings.scale * Gdx.graphics.getDeltaTime();
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        Vector2 test = new Vector2(this.vX, this.vY);
        this.rotation = test.angle() + 45.0f;
        this.scale -= Gdx.graphics.getDeltaTime() / 2.0f;
        if (this.y < this.floor && this.vY < 0.0f && this.duration > 0.2f) {
            this.duration = 0.2f;
        }
        this.color.a = this.duration < 0.2f ? Interpolation.fade.apply(0.0f, 1.0f, this.duration * 5.0f) : 1.0f;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale * 0.54f, this.rotation);
    }

    @Override
    public void dispose() {
    }
}


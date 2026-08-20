/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class DamageImpactBlurEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float startScale;
    private TextureAtlas.AtlasRegion img = ImageMaster.STRIKE_BLUR;

    public DamageImpactBlurEffect(float x, float y) {
        this.startingDuration = this.duration = MathUtils.random(0.5f, 0.75f);
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.y = y - (float)this.img.packedHeight / 2.0f;
        this.rotation = 0.0f;
        this.vX = MathUtils.random(-42000.0f * Settings.scale, 42000.0f * Settings.scale);
        this.vY = MathUtils.random(-42000.0f * Settings.scale, 42000.0f * Settings.scale);
        this.startScale = MathUtils.random(4.0f, 8.0f);
        this.renderBehind = true;
        float tmp = MathUtils.random(0.1f, 0.3f);
        this.color = new Color(tmp, tmp, tmp, 1.0f);
    }

    @Override
    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.vX *= 56.0f * Gdx.graphics.getDeltaTime();
        this.vY *= 56.0f * Gdx.graphics.getDeltaTime();
        this.scale = Settings.scale * (this.duration / this.startingDuration * 2.0f + this.startScale);
        this.color.a = this.duration;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
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


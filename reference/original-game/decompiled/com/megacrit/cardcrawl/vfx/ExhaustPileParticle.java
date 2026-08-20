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
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class ExhaustPileParticle
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float scale = 0.01f;
    private float targetScale;
    private static TextureAtlas.AtlasRegion img;

    public ExhaustPileParticle(float x, float y) {
        if (img == null) {
            img = ImageMaster.EXHAUST_L;
        }
        this.targetScale = MathUtils.random(0.5f, 0.7f) * Settings.scale;
        this.color = new Color();
        this.color.a = 0.0f;
        this.color.g = MathUtils.random(0.2f, 0.4f);
        this.color.r = this.color.g + 0.1f;
        this.color.b = this.color.r + 0.1f;
        this.x = x - (float)ExhaustPileParticle.img.packedWidth / 2.0f;
        this.y = y - (float)ExhaustPileParticle.img.packedHeight / 2.0f;
        this.rotation = MathUtils.random(360.0f);
        this.duration = this.startingDuration = 2.0f;
    }

    @Override
    public void update() {
        this.scale = Interpolation.bounceIn.apply(this.targetScale, 0.1f, this.duration / this.startingDuration);
        this.rotation += this.vX * this.startingDuration * Gdx.graphics.getDeltaTime();
        this.color.a = this.duration / this.startingDuration;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(img, this.x, this.y, (float)ExhaustPileParticle.img.packedWidth / 2.0f, (float)ExhaustPileParticle.img.packedHeight / 2.0f, ExhaustPileParticle.img.packedWidth, ExhaustPileParticle.img.packedHeight, this.scale, this.scale, this.rotation);
    }

    @Override
    public void dispose() {
    }
}


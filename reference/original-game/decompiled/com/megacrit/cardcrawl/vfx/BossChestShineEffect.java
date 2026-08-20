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

public class BossChestShineEffect
extends AbstractGameEffect {
    private float effectDuration;
    private float x;
    private float y;
    private float vY;
    private float alpha;
    private float targetScale;
    private TextureAtlas.AtlasRegion img = ImageMaster.ROOM_SHINE_2;

    public BossChestShineEffect(float x, float y) {
        this.duration = this.effectDuration = MathUtils.random(1.0f, 2.0f);
        this.startingDuration = this.effectDuration;
        this.x = x;
        this.y = y;
        this.vY = MathUtils.random(10.0f, 50.0f) * Settings.scale;
        this.alpha = MathUtils.random(0.7f, 1.0f);
        this.color = new Color(1.0f, MathUtils.random(0.4f, 0.9f), 1.0f, this.alpha);
        this.scale = 0.01f;
        this.targetScale = MathUtils.random(0.7f, 1.3f);
        this.rotation = MathUtils.random(-3.0f, 3.0f);
    }

    public BossChestShineEffect() {
        this.duration = this.effectDuration = MathUtils.random(1.0f, 3.0f);
        this.startingDuration = this.effectDuration;
        this.x = (float)Settings.WIDTH / 2.0f + MathUtils.random(-450.0f, 450.0f) * Settings.scale - (float)this.img.packedWidth / 2.0f;
        this.y = (float)Settings.HEIGHT / 2.0f + MathUtils.random(-200.0f, 250.0f) * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.vY = MathUtils.random(10.0f, 50.0f) * Settings.scale;
        this.alpha = MathUtils.random(0.7f, 1.0f);
        this.color = new Color(1.0f, MathUtils.random(0.4f, 0.9f), 1.0f, this.alpha);
        this.scale = 0.01f;
        this.targetScale = MathUtils.random(0.5f, 1.3f);
        this.rotation = MathUtils.random(-3.0f, 3.0f);
    }

    @Override
    public void update() {
        float t;
        if (this.vY != 0.0f) {
            this.y += this.vY * Gdx.graphics.getDeltaTime();
            MathUtils.lerp(this.vY, 0.0f, Gdx.graphics.getDeltaTime() * 10.0f);
            if (this.vY < 0.5f) {
                this.vY = 0.0f;
            }
        }
        if ((t = (this.effectDuration - this.duration) * 2.0f) > 1.0f) {
            t = 1.0f;
        }
        float tmp = Interpolation.bounceOut.apply(0.01f, this.targetScale, t);
        this.scale = tmp * tmp * Settings.scale;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        } else if (this.duration < this.effectDuration / 2.0f) {
            this.color.a = Interpolation.exp5In.apply(0.0f, this.alpha, this.duration / (this.effectDuration / 2.0f));
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.9f, 1.1f), this.scale * MathUtils.random(0.7f, 1.3f), this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


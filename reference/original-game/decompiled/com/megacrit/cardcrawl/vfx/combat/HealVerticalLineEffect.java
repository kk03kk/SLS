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

public class HealVerticalLineEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vY;
    private float staggerTimer;
    private TextureAtlas.AtlasRegion img = ImageMaster.STRIKE_LINE;
    private TextureAtlas.AtlasRegion img2 = ImageMaster.STRIKE_LINE_2;

    public HealVerticalLineEffect(float x, float y) {
        this.startingDuration = this.duration = MathUtils.random(0.6f, 1.3f);
        this.x = x;
        this.y = y;
        this.staggerTimer = MathUtils.random(0.0f, 0.5f);
        float tmp = MathUtils.random(5.0f, 20.0f);
        this.vY = tmp * tmp * Settings.scale;
        this.rotation = 90.0f;
        this.color = MathUtils.randomBoolean() ? Color.CHARTREUSE.cpy() : new Color(1.0f, 1.0f, 0.5f, 1.0f);
        this.color.a = 0.0f;
        this.renderBehind = MathUtils.randomBoolean(0.3f);
    }

    @Override
    public void update() {
        if (this.staggerTimer > 0.0f) {
            this.staggerTimer -= Gdx.graphics.getDeltaTime();
            return;
        }
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.scale = Settings.scale * this.duration / this.startingDuration;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration / this.startingDuration > 0.5f) {
            this.color.a = 1.0f - this.duration / this.startingDuration;
            this.color.a += MathUtils.random(0.0f, 0.2f);
        } else {
            this.color.a = this.duration / this.startingDuration;
            this.color.a += MathUtils.random(0.0f, 0.2f);
        }
        if (this.duration < 0.0f) {
            this.isDone = true;
            this.color.a = 0.0f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.staggerTimer > 0.0f) {
            return;
        }
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x - (float)this.img.packedWidth / 2.0f, this.y - (float)this.img.packedHeight / 2.0f, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.7f, 2.0f), this.scale * 0.8f, this.rotation + MathUtils.random(-2.0f, 2.0f));
        sb.setColor(new Color(1.0f, 1.0f, 0.7f, this.color.a));
        sb.draw(this.img2, this.x - (float)this.img2.packedWidth / 2.0f, this.y - (float)this.img2.packedHeight / 2.0f, (float)this.img2.packedWidth / 2.0f, (float)this.img2.packedHeight / 2.0f, this.img2.packedWidth, this.img2.packedHeight, this.scale * 1.5f, this.scale * MathUtils.random(1.2f, 2.4f), this.rotation);
        sb.draw(this.img2, this.x - (float)this.img2.packedWidth / 2.0f, this.y - (float)this.img2.packedHeight / 2.0f, (float)this.img2.packedWidth / 2.0f, (float)this.img2.packedHeight / 2.0f, this.img2.packedWidth, this.img2.packedHeight, this.scale * 1.5f, this.scale * MathUtils.random(1.2f, 2.4f), this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


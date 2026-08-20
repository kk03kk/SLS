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

public class AwakenedWingParticle
extends AbstractGameEffect {
    private float x;
    private float y;
    private float tScale;
    private TextureAtlas.AtlasRegion img;

    public AwakenedWingParticle() {
        this.startingDuration = this.duration = 2.0f;
        this.img = ImageMaster.THICK_3D_LINE_2;
        this.scale = 0.01f;
        this.rotation = MathUtils.random(25.0f, 85.0f);
        this.renderBehind = MathUtils.randomBoolean(0.2f);
        if (this.renderBehind) {
            this.tScale = MathUtils.random(0.8f, 1.2f);
        }
        this.color = new Color(0.3f, 0.3f, MathUtils.random(0.3f, 0.35f), MathUtils.random(0.5f, 0.9f));
        int roll = MathUtils.random(0, 2);
        if (roll == 0) {
            this.x = MathUtils.random(-340.0f, -170.0f) * Settings.scale;
            this.y = MathUtils.random(-20.0f, 20.0f) * Settings.scale;
            this.tScale = MathUtils.random(0.4f, 0.5f);
        } else if (roll == 1) {
            this.x = MathUtils.random(-220.0f, -20.0f) * Settings.scale;
            this.y = MathUtils.random(-40.0f, -10.0f) * Settings.scale;
            this.tScale = MathUtils.random(0.4f, 0.5f);
        } else {
            this.x = MathUtils.random(-270.0f, -60.0f) * Settings.scale;
            this.y = MathUtils.random(-30.0f, -0.0f) * Settings.scale;
            this.tScale = MathUtils.random(0.4f, 0.7f);
        }
        this.x += 155.0f * Settings.scale;
        this.y += 30.0f * Settings.scale;
        this.x -= (float)(this.img.packedWidth / 2);
        this.y -= (float)(this.img.packedHeight / 2);
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        if (this.duration > 1.0f) {
            this.scale = Interpolation.bounceIn.apply(this.tScale, 0.01f, this.duration - 1.0f) * Settings.scale;
        }
        if (this.duration < 0.2f) {
            this.color.a = Interpolation.fade.apply(0.0f, 0.5f, this.duration * 5.0f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void render(SpriteBatch sb, float x, float y) {
        float derp = MathUtils.random(3.0f, 5.0f);
        sb.setColor(new Color(0.4f, 1.0f, 1.0f, this.color.a / 2.0f));
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x + x, this.y + y, (float)this.img.packedWidth * 0.08f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(1.1f, 1.25f), this.scale, this.rotation + derp);
        sb.setBlendFunction(770, 771);
        sb.setColor(this.color);
        sb.draw(this.img, this.x + x, this.y + y, (float)this.img.packedWidth * 0.08f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation + derp);
        sb.setColor(new Color(0.0f, 0.0f, 0.0f, this.color.a / 5.0f));
        sb.draw(this.img, this.x + x, this.y + y, (float)this.img.packedWidth * 0.08f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 0.7f, this.scale * 0.7f, this.rotation + derp - 40.0f);
    }

    @Override
    public void dispose() {
    }
}


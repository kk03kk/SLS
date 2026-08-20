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

public class FlyingSpikeEffect
extends AbstractGameEffect {
    private static final float DURATION = 0.75f;
    private float x;
    private float y;
    private float vX;
    private float vY;
    private TextureAtlas.AtlasRegion img = ImageMaster.THICK_3D_LINE;

    public FlyingSpikeEffect(float x, float y, float startingRotation, float vX, float vY, Color color) {
        this.duration = 0.75f;
        this.rotation = startingRotation;
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.y = y - (float)this.img.packedHeight / 2.0f;
        this.vX = vX;
        this.vY = vY;
        this.color = new Color(color.r, color.g, color.b, 0.0f);
        this.renderBehind = true;
        this.scale = 0.01f;
        this.rotation += MathUtils.random(-5.0f, 5.0f);
    }

    @Override
    public void update() {
        this.scale = this.duration * 2.0f * Settings.scale;
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.duration -= Gdx.graphics.getDeltaTime();
        this.color.a = this.duration > 0.5f ? (0.75f - this.duration) * 2.0f : this.duration;
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


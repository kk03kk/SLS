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

public class EmptyStanceParticleEffect
extends AbstractGameEffect {
    private float rotationSpeed;
    private Vector2 pos;
    private Vector2 pos2;
    private Vector2 pos3;
    private static TextureAtlas.AtlasRegion img;

    public EmptyStanceParticleEffect(float x, float y) {
        if (img == null) {
            img = ImageMaster.STRIKE_BLUR;
        }
        this.duration = this.startingDuration = 0.6f;
        this.pos = new Vector2(x, y);
        this.rotationSpeed = MathUtils.random(120.0f, 150.0f);
        this.rotation = MathUtils.random(360.0f);
        this.scale = MathUtils.random(0.7f, 2.5f) * Settings.scale;
        this.color = new Color(MathUtils.random(0.2f, 0.4f), MathUtils.random(0.6f, 0.8f), 1.0f, 0.0f);
        this.renderBehind = MathUtils.randomBoolean(0.8f);
    }

    @Override
    public void update() {
        this.pos2 = new Vector2(MathUtils.cosDeg(this.rotation), MathUtils.sinDeg(this.rotation));
        this.pos2.nor();
        this.pos2.x *= 10.0f;
        this.pos2.y *= 10.0f;
        this.pos3 = this.pos.sub(this.pos2);
        this.rotation += Gdx.graphics.getDeltaTime() * this.rotationSpeed;
        this.duration -= Gdx.graphics.getDeltaTime();
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.fade.apply(1.0f, 0.0f, (this.duration - this.startingDuration / 2.0f) * 2.0f) : Interpolation.fade.apply(0.0f, 1.0f, this.duration * 2.0f);
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.pos3 != null) {
            sb.setBlendFunction(770, 1);
            sb.setColor(this.color);
            sb.draw(img, this.pos3.x - (float)EmptyStanceParticleEffect.img.packedWidth / 2.0f, this.pos3.y - (float)EmptyStanceParticleEffect.img.packedHeight / 2.0f, (float)EmptyStanceParticleEffect.img.packedWidth / 2.0f, (float)EmptyStanceParticleEffect.img.packedHeight / 2.0f, EmptyStanceParticleEffect.img.packedWidth, EmptyStanceParticleEffect.img.packedHeight, this.scale + MathUtils.random(-0.08f, 0.08f), this.scale + MathUtils.random(-0.08f, 0.08f), this.rotation);
            sb.setBlendFunction(770, 771);
        }
    }

    @Override
    public void dispose() {
    }
}


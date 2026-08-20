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

public class LightFlareParticleEffect
extends AbstractGameEffect {
    private Vector2 pos = new Vector2();
    private float speed;
    private float speedStart;
    private float speedTarget;
    private float waveIntensity;
    private float waveSpeed;
    private TextureAtlas.AtlasRegion img = ImageMaster.STRIKE_BLUR;

    public LightFlareParticleEffect(float x, float y, Color color) {
        this.startingDuration = this.duration = MathUtils.random(0.5f, 1.1f);
        this.pos.x = x - (float)this.img.packedWidth / 2.0f;
        this.pos.y = y - (float)this.img.packedHeight / 2.0f;
        this.speedStart = this.speed = MathUtils.random(200.0f, 300.0f) * Settings.scale;
        this.speedTarget = MathUtils.random(20.0f, 30.0f) * Settings.scale;
        this.color = color.cpy();
        this.color.a = 0.0f;
        this.renderBehind = true;
        this.rotation = MathUtils.random(360.0f);
        this.waveIntensity = MathUtils.random(5.0f, 10.0f);
        this.waveSpeed = MathUtils.random(-20.0f, 20.0f);
        this.speedTarget = MathUtils.random(0.1f, 0.5f);
        this.scale = MathUtils.random(0.2f, 1.0f) * Settings.scale;
    }

    @Override
    public void update() {
        Vector2 tmp = new Vector2(MathUtils.cosDeg(this.rotation), MathUtils.sinDeg(this.rotation));
        tmp.x *= this.speed * Gdx.graphics.getDeltaTime();
        tmp.y *= this.speed * Gdx.graphics.getDeltaTime();
        this.speed = Interpolation.pow2OutInverse.apply(this.speedStart, this.speedTarget, 1.0f - this.duration / this.startingDuration);
        this.pos.x += tmp.x;
        this.pos.y += tmp.y;
        this.rotation += MathUtils.cos(this.duration * this.waveSpeed) * this.waveIntensity;
        this.color.a = this.duration < 0.5f ? Interpolation.fade.apply(0.0f, 1.0f, this.duration * 2.0f) : 1.0f;
        super.update();
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(new Color(this.color.r, this.color.g, this.color.b, this.color.a / 4.0f));
        sb.draw(this.img, this.pos.x, this.pos.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 4.0f, this.scale * 4.0f, this.rotation);
        sb.setColor(this.color);
        sb.draw(this.img, this.pos.x, this.pos.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


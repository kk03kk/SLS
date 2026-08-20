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

public class SmallLaserEffect
extends AbstractGameEffect {
    private float sX;
    private float sY;
    private float dX;
    private float dY;
    private float dst;
    private static final float DUR = 0.5f;
    private static TextureAtlas.AtlasRegion img;

    public SmallLaserEffect(float sX, float sY, float dX, float dY) {
        if (img == null) {
            img = ImageMaster.vfxAtlas.findRegion("combat/laserThin");
        }
        this.sX = sX;
        this.sY = sY;
        this.dX = dX;
        this.dY = dY;
        this.dst = Vector2.dst(this.sX, this.sY, this.dX, this.dY) / Settings.scale;
        this.color = Color.CYAN.cpy();
        this.duration = 0.5f;
        this.startingDuration = 0.5f;
        this.rotation = MathUtils.atan2(dX - sX, dY - sY);
        this.rotation *= 57.295776f;
        this.rotation = -this.rotation + 90.0f;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.pow2In.apply(1.0f, 0.0f, (this.duration - 0.25f) * 4.0f) : Interpolation.bounceIn.apply(0.0f, 1.0f, this.duration * 4.0f);
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(img, this.sX, this.sY - (float)SmallLaserEffect.img.packedHeight / 2.0f + 10.0f * Settings.scale, 0.0f, (float)SmallLaserEffect.img.packedHeight / 2.0f, this.dst, 50.0f, this.scale + MathUtils.random(-0.01f, 0.01f), this.scale, this.rotation);
        sb.setColor(new Color(0.3f, 0.3f, 1.0f, this.color.a));
        sb.draw(img, this.sX, this.sY - (float)SmallLaserEffect.img.packedHeight / 2.0f, 0.0f, (float)SmallLaserEffect.img.packedHeight / 2.0f, this.dst, MathUtils.random(50.0f, 90.0f), this.scale + MathUtils.random(-0.02f, 0.02f), this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


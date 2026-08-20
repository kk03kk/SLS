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

public class ThirdEyeParticleEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float vY;
    private TextureAtlas.AtlasRegion img;

    public ThirdEyeParticleEffect(float x, float y, float vX, float vY) {
        int roll = MathUtils.random(2);
        this.img = roll == 0 ? ImageMaster.FLAME_1 : (roll == 1 ? ImageMaster.FLAME_3 : ImageMaster.FLAME_3);
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.y = y - (float)this.img.packedHeight / 2.0f;
        this.vX = vX * Settings.scale;
        this.vY = vY * Settings.scale;
        this.rotation = new Vector2(vX, vY).angle() - 90.0f;
        this.scale = 3.0f * Settings.scale;
        this.color = Color.VIOLET.cpy();
        this.color.a = 0.0f;
        this.duration = this.startingDuration = 0.6f;
        this.renderBehind = true;
    }

    @Override
    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        if (this.duration > this.startingDuration / 2.0f) {
            this.color.a = Interpolation.pow2Out.apply(0.7f, 0.0f, (this.duration - this.startingDuration / 2.0f) / (this.startingDuration / 2.0f));
        } else {
            this.color.a = Interpolation.fade.apply(0.0f, 0.5f, this.duration / (this.startingDuration / 2.0f));
            this.scale = Interpolation.fade.apply(0.01f, 3.0f, this.duration / (this.startingDuration / 2.0f)) * Settings.scale;
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale + MathUtils.random(-0.05f, 0.05f), this.scale + MathUtils.random(-0.05f, 0.05f), this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


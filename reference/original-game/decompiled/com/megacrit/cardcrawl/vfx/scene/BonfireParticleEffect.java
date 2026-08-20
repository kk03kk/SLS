/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class BonfireParticleEffect
extends AbstractGameEffect {
    private float effectDuration;
    private float x;
    private float y;
    private float vY;
    private float vX;
    private float rotator;
    private TextureAtlas.AtlasRegion img = this.getImg();

    public BonfireParticleEffect(boolean isAbove, boolean isBlue) {
        this.x = 170.0f * Settings.scale + MathUtils.random(-25.0f, 25.0f) * Settings.scale;
        this.y = 44.0f * Settings.scale;
        this.duration = this.effectDuration = MathUtils.random(1.0f, 2.0f);
        this.startingDuration = this.effectDuration;
        this.vY = MathUtils.random(0.0f, 200.0f) * Settings.scale;
        this.vX = MathUtils.random(-30.0f, 30.0f) * Settings.scale;
        this.rotation = MathUtils.random(-10.0f, 10.0f);
        this.scale = MathUtils.random(0.8f, 2.5f);
        this.vY /= this.scale;
        this.vX /= this.scale * 2.0f;
        int roll = MathUtils.random(2);
        this.color = !isBlue ? (roll == 0 ? Color.ORANGE.cpy() : (roll == 1 ? Color.GOLDENROD.cpy() : Color.CORAL.cpy())) : (roll == 0 ? Color.CYAN.cpy() : (roll == 1 ? Color.SKY.cpy() : Color.TEAL.cpy()));
        this.rotator = MathUtils.random(-10.0f, 10.0f);
    }

    private TextureAtlas.AtlasRegion getImg() {
        switch (MathUtils.random(0, 2)) {
            case 0: {
                return ImageMaster.TORCH_FIRE_1;
            }
            case 1: {
                return ImageMaster.TORCH_FIRE_2;
            }
        }
        return ImageMaster.TORCH_FIRE_3;
    }

    @Override
    public void update() {
        this.rotation += this.rotator * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.vX *= 0.995f;
        this.duration -= Gdx.graphics.getDeltaTime();
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.exp10In.apply(0.4f, 0.0f, (this.duration - this.startingDuration / 2.0f) / (this.startingDuration / 2.0f)) : Interpolation.pow2In.apply(0.0f, 0.4f, this.duration / (this.startingDuration / 2.0f));
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb, float x2, float y2) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x + x2, this.y + y2, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * Settings.scale, this.scale * Settings.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void render(SpriteBatch sb) {
    }
}


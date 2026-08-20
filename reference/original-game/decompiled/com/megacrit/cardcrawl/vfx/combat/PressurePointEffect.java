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
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class PressurePointEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float endX;
    private float endY;
    private float scaleMultiplier;
    private TextureAtlas.AtlasRegion img = ImageMaster.DAGGER_STREAK;

    public PressurePointEffect(float setX, float setY) {
        this.endX = (setX -= 120.0f * Settings.scale) - (float)this.img.packedWidth / 2.0f;
        this.endY = (setY -= -80.0f * Settings.scale) - (float)this.img.packedHeight / 2.0f;
        this.x = this.endX + MathUtils.random(-550.0f, -450.0f) * Settings.scale;
        this.y = this.endY + MathUtils.random(380.0f, 320.0f) * Settings.scale;
        this.startingDuration = 0.3f;
        this.duration = 0.3f;
        this.scaleMultiplier = MathUtils.random(0.05f, 0.2f);
        this.rotation = 150.0f;
        this.color = Color.VIOLET.cpy();
        this.color.a = 0.0f;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.x = Interpolation.swingIn.apply(this.endX, this.x, this.duration * 3.33f);
        this.y = Interpolation.swingIn.apply(this.endY, this.y, this.duration * 3.33f);
        if (this.duration < 0.0f) {
            this.isDone = true;
            this.duration = 0.0f;
        }
        this.color.a = 1.0f - this.duration;
        this.scale = this.duration * Settings.scale + this.scaleMultiplier;
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale * 1.5f, this.rotation);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 0.75f, this.scale * 0.75f, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


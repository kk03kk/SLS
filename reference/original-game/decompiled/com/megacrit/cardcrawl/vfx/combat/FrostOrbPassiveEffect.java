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

public class FrostOrbPassiveEffect
extends AbstractGameEffect {
    private float effectDuration;
    private float x;
    private float y;
    private float vY;
    private float alpha;
    private TextureAtlas.AtlasRegion img = ImageMaster.ROOM_SHINE_1;

    public FrostOrbPassiveEffect(float x, float y) {
        this.duration = this.effectDuration = MathUtils.random(0.4f, 0.8f);
        this.startingDuration = this.effectDuration;
        float offset = MathUtils.random(-34.0f, 34.0f) * Settings.scale;
        this.x = x + offset - (float)this.img.packedWidth / 2.0f;
        if (offset > 0.0f) {
            this.renderBehind = true;
        }
        this.y = y + MathUtils.random(-28.0f, 20.0f) * Settings.scale - (float)this.img.packedWidth / 2.0f;
        this.vY = MathUtils.random(2.0f, 20.0f) * Settings.scale;
        this.alpha = MathUtils.random(0.5f, 1.0f);
        this.color = new Color(MathUtils.random(0.6f, 0.9f), 1.0f, 1.0f, this.alpha);
        this.scale = MathUtils.random(0.5f, 1.2f) * Settings.scale;
    }

    @Override
    public void update() {
        if (this.vY != 0.0f) {
            this.y += this.vY * Gdx.graphics.getDeltaTime();
            MathUtils.lerp(this.vY, 0.0f, Gdx.graphics.getDeltaTime() * 10.0f);
            if (this.vY < 0.5f) {
                this.vY = 0.0f;
            }
        }
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
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.75f, 1.25f), this.scale * MathUtils.random(0.75f, 1.25f), this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


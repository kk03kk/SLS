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

public class AwakenedEyeParticle
extends AbstractGameEffect {
    private float x;
    private float y;
    private TextureAtlas.AtlasRegion img;

    public AwakenedEyeParticle(float x, float y) {
        this.startingDuration = this.duration = MathUtils.random(0.5f, 1.0f);
        this.img = ImageMaster.ROOM_SHINE_2;
        this.x = x - (float)(this.img.packedWidth / 2);
        this.y = y - (float)(this.img.packedHeight / 2);
        this.scale = Settings.scale * MathUtils.random(0.5f, 1.0f);
        this.rotation = 0.0f;
        this.color = new Color(MathUtils.random(0.2f, 0.4f), MathUtils.random(0.8f, 1.0f), MathUtils.random(0.8f, 1.0f), 0.01f);
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.color.a = Interpolation.fade.apply(0.0f, 0.5f, this.duration / this.startingDuration);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(6.0f, 12.0f), this.scale * MathUtils.random(0.7f, 0.8f), this.rotation + MathUtils.random(-1.0f, 1.0f));
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.2f, 0.5f), this.scale * MathUtils.random(2.0f, 3.0f), this.rotation + MathUtils.random(-1.0f, 1.0f));
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


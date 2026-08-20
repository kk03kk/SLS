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
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class ShineSparkleEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float oX;
    private float oY;
    private float dur_div2;
    private Hitbox hb = null;
    private TextureAtlas.AtlasRegion img;

    public ShineSparkleEffect(float x, float y) {
        this(null);
        this.x = x;
        this.y = y;
    }

    public ShineSparkleEffect(Hitbox hb) {
        this.hb = hb;
        this.img = ImageMaster.ROOM_SHINE_2;
        this.duration = MathUtils.random(0.9f, 1.2f);
        this.scale = MathUtils.random(0.8f, 1.2f) * Settings.scale;
        this.dur_div2 = this.duration / 2.0f;
        this.color = new Color(1.0f, MathUtils.random(0.7f, 1.0f), 0.4f, 0.0f);
        this.oX += MathUtils.random(-50.0f, 50.0f) * Settings.scale;
        this.oY += MathUtils.random(-50.0f, 50.0f) * Settings.scale;
        this.oX -= (float)this.img.packedWidth / 2.0f;
        this.oY -= (float)this.img.packedHeight / 2.0f;
        this.renderBehind = MathUtils.randomBoolean(0.2f + (this.scale - 0.5f));
        this.rotation = MathUtils.random(-5.0f, 5.0f);
    }

    @Override
    public void update() {
        this.color.a = this.duration > this.dur_div2 ? Interpolation.pow3In.apply(0.6f, 0.0f, (this.duration - this.dur_div2) / this.dur_div2) : Interpolation.pow3In.apply(0.0f, 0.6f, this.duration / this.dur_div2);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        if (this.hb == null) {
            sb.draw(this.img, this.x + this.oX, this.y + this.oY, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale * MathUtils.random(0.6f, 1.4f), this.rotation);
        } else {
            sb.draw(this.img, this.hb.cX + this.oX, this.hb.cY + this.oY, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale * MathUtils.random(0.6f, 1.4f), this.rotation);
        }
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


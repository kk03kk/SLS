/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class RefreshEnergyEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 0.4f;
    private float scale = Settings.scale / 1.2f;
    private Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private TextureAtlas.AtlasRegion img = ImageMaster.WHITE_RING;
    private float x;
    private float y;

    public RefreshEnergyEffect() {
        this.x = 198.0f * Settings.scale - (float)this.img.packedWidth / 2.0f;
        this.y = 190.0f * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.duration = 0.4f;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.scale *= 1.0f + Gdx.graphics.getDeltaTime() * 2.5f;
        this.color.a = Interpolation.fade.apply(0.0f, 0.75f, this.duration / 0.4f);
        if (this.color.a < 0.0f) {
            this.color.a = 0.0f;
        }
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 1.5f, this.scale * 1.5f, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


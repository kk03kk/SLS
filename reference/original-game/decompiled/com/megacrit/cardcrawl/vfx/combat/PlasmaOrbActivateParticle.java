/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class PlasmaOrbActivateParticle
extends AbstractGameEffect {
    private float effectDuration;
    private float x;
    private float y;
    private float sX;
    private float sY;
    private float tX;
    private float tY;
    private TextureAtlas.AtlasRegion img = ImageMaster.GLOW_SPARK_2;

    public PlasmaOrbActivateParticle(float x, float y) {
        this.duration = this.effectDuration = 0.5f;
        this.startingDuration = this.effectDuration;
        this.sX = x + MathUtils.random(-100.0f, 100.0f) * Settings.scale;
        this.sY = y + MathUtils.random(-30.0f, 30.0f) * Settings.scale;
        this.tX = x;
        this.tY = y;
        this.x = x;
        this.y = y;
        int tmp = MathUtils.random(2);
        this.color = tmp == 0 ? Settings.LIGHT_YELLOW_COLOR.cpy() : (tmp == 1 ? Color.CYAN.cpy() : Color.SALMON.cpy());
        this.scale = MathUtils.random(0.6f, 1.8f) * Settings.scale;
        this.renderBehind = true;
    }

    @Override
    public void update() {
        this.x = Interpolation.swing.apply(this.sX, this.tX, this.duration);
        this.y = Interpolation.swing.apply(this.sY, this.tY, this.duration);
        super.update();
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x - (float)this.img.packedWidth / 2.0f, this.y - (float)this.img.packedWidth / 2.0f, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.7f, 1.4f), this.scale * MathUtils.random(0.7f, 1.4f), this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class UpgradeHammerImprintEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img = ImageMaster.UPGRADE_HAMMER_IMPACT;
    private static final float DUR = 0.7f;
    private float x;
    private float y;
    private float hammerGlowScale;
    private Color shineColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);

    public UpgradeHammerImprintEffect(float x, float y) {
        this.x = x - (float)(this.img.packedWidth / 2);
        this.y = y - (float)(this.img.packedHeight / 2);
        this.color = Color.WHITE.cpy();
        this.color.a = 0.7f;
        this.duration = 0.7f;
        this.scale = Settings.scale / MathUtils.random(1.0f, 1.5f);
        this.rotation = MathUtils.random(0.0f, 360.0f);
        this.hammerGlowScale = 1.0f - this.duration;
        this.hammerGlowScale *= this.hammerGlowScale;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.color.a = this.duration;
        this.hammerGlowScale = 1.7f - this.duration;
        this.hammerGlowScale *= this.hammerGlowScale * this.hammerGlowScale;
        this.scale += Gdx.graphics.getDeltaTime() / 20.0f;
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x + MathUtils.random(-2.0f, 2.0f) * Settings.scale, this.y + MathUtils.random(-2.0f, 2.0f) * Settings.scale, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        this.shineColor.a = this.color.a / 10.0f;
        sb.setColor(this.shineColor);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.hammerGlowScale, this.hammerGlowScale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


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

public class AdditiveSlashImpactEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float targetScale;
    private static TextureAtlas.AtlasRegion img;

    public AdditiveSlashImpactEffect(float x, float y, Color color) {
        if (img == null) {
            img = ImageMaster.vfxAtlas.findRegion("ui/impactLineThick");
        }
        this.x = x - (float)AdditiveSlashImpactEffect.img.packedWidth / 2.0f;
        this.y = y - (float)AdditiveSlashImpactEffect.img.packedHeight / 2.0f;
        this.color = color;
        this.duration = 0.4f;
        this.scale = 0.01f;
        this.targetScale = MathUtils.random(3.0f, 5.0f);
        this.rotation = MathUtils.random(360.0f);
    }

    @Override
    public void update() {
        if (this.duration > 0.2f) {
            this.color.a = Interpolation.fade.apply(0.0f, 0.8f, (this.duration - 0.2f) * 5.0f);
            this.scale = Interpolation.fade.apply(0.01f, this.targetScale, (this.duration - 0.2f) * 5.0f) * Settings.scale;
        } else {
            this.color.a = Interpolation.fade.apply(0.0f, 0.8f, this.duration * 5.0f);
            this.scale = Interpolation.fade.apply(0.01f, this.targetScale, this.duration * 5.0f) * Settings.scale;
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
        sb.draw(img, this.x, this.y, (float)AdditiveSlashImpactEffect.img.packedWidth / 2.0f, (float)AdditiveSlashImpactEffect.img.packedHeight / 2.0f, AdditiveSlashImpactEffect.img.packedWidth, AdditiveSlashImpactEffect.img.packedHeight, this.scale / 3.0f, this.scale, this.rotation);
        sb.draw(img, this.x, this.y, (float)AdditiveSlashImpactEffect.img.packedWidth / 2.0f, (float)AdditiveSlashImpactEffect.img.packedHeight / 2.0f, AdditiveSlashImpactEffect.img.packedWidth, AdditiveSlashImpactEffect.img.packedHeight, this.scale / 6.0f, this.scale * 0.5f, this.rotation + 90.0f);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


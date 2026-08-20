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

public class LightFlareMEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    public static TextureAtlas.AtlasRegion[] imgs = new TextureAtlas.AtlasRegion[2];
    private TextureAtlas.AtlasRegion img;
    public static boolean renderGreen = false;

    public LightFlareMEffect(float x, float y) {
        if (imgs[0] == null) {
            LightFlareMEffect.imgs[0] = ImageMaster.vfxAtlas.findRegion("env/lightFlare1");
            LightFlareMEffect.imgs[1] = ImageMaster.vfxAtlas.findRegion("env/lightFlare2");
        }
        this.startingDuration = this.duration = MathUtils.random(2.0f, 3.0f);
        this.img = imgs[MathUtils.random(imgs.length - 1)];
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.y = y - (float)this.img.packedHeight / 2.0f;
        this.scale = Settings.scale * MathUtils.random(4.0f, 5.0f);
        this.rotation = MathUtils.random(360.0f);
        this.color = !renderGreen ? new Color(MathUtils.random(0.6f, 1.0f), MathUtils.random(0.4f, 0.7f), MathUtils.random(0.2f, 0.3f), 0.01f) : new Color(MathUtils.random(0.1f, 0.3f), MathUtils.random(0.5f, 0.9f), MathUtils.random(0.1f, 0.3f), 0.01f);
        this.renderBehind = true;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.color.a = this.startingDuration - this.duration < 1.0f ? Interpolation.fade.apply(0.2f, 0.0f, this.duration / this.startingDuration) : Interpolation.fade.apply(0.0f, 0.2f, this.duration / this.startingDuration);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


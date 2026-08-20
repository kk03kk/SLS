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

public class EventBgParticle
extends AbstractGameEffect {
    private float x;
    private float y;
    private float aV;
    private float offsetX;
    private static TextureAtlas.AtlasRegion img;
    private static TextureAtlas.AtlasRegion img2;
    private TextureAtlas.AtlasRegion useImg;

    public EventBgParticle() {
        if (img == null) {
            img = ImageMaster.vfxAtlas.findRegion("eventBgParticle1");
            img2 = ImageMaster.vfxAtlas.findRegion("eventBgParticle2");
        }
        this.useImg = MathUtils.randomBoolean() ? img : img2;
        this.startingDuration = this.duration = 20.0f;
        this.x = (float)Settings.WIDTH / 2.0f - (float)(this.useImg.packedWidth / 2);
        this.y = (float)Settings.HEIGHT / 2.0f - (float)(this.useImg.packedHeight / 2);
        this.scale = Settings.scale * MathUtils.random(0.3f, 3.0f);
        this.rotation = MathUtils.random(360.0f);
        this.offsetX = MathUtils.random(600.0f, 670.0f);
        this.aV = MathUtils.random(0.01f, 7.0f) + this.offsetX / 300.0f;
        this.offsetX *= Settings.scale;
        this.scale += this.offsetX / 900.0f;
        float g = MathUtils.random(0.05f, 0.1f);
        this.color = new Color(0.0f, g, g, 0.1f);
        this.renderBehind = true;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.rotation += Gdx.graphics.getDeltaTime() * this.aV;
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        if (this.duration > 16.0f) {
            this.color.a = Interpolation.fade.apply(0.3f, 0.0f, (this.duration - 16.0f) / 4.0f);
        } else if (this.duration < 4.0f) {
            this.color.a = Interpolation.fade.apply(0.0f, 0.3f, this.duration / 4.0f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.useImg, this.x - this.offsetX, this.y, (float)this.useImg.packedWidth / 2.0f + this.offsetX, (float)this.useImg.packedHeight / 2.0f, this.useImg.packedWidth, this.useImg.packedHeight, this.scale, this.scale, this.rotation);
    }

    @Override
    public void dispose() {
    }
}


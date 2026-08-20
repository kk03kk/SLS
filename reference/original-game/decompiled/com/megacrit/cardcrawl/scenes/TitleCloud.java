/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;

public class TitleCloud {
    private TextureAtlas.AtlasRegion region;
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float sliderJiggle;

    public TitleCloud(TextureAtlas.AtlasRegion region, float vX, float x) {
        this.region = region;
        this.vX = vX;
        this.x = x;
        this.y = (float)Settings.HEIGHT - 1100.0f * Settings.scale + MathUtils.random(-50.0f, 50.0f) * Settings.scale;
        this.vY = MathUtils.random(-vX / 10.0f, vX / 10.0f) * Settings.scale;
        this.sliderJiggle = MathUtils.random(-4.0f, 4.0f);
    }

    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        if (this.vX > 0.0f && this.x > 1920.0f * Settings.xScale) {
            this.x = -1920.0f * Settings.xScale;
            this.vX = MathUtils.random(10.0f, 50.0f) * Settings.xScale;
            this.y = (float)Settings.HEIGHT - 1100.0f * Settings.scale + MathUtils.random(-50.0f, 50.0f) * Settings.scale;
            this.vY = MathUtils.random(-this.vX / 5.0f, this.vX / 5.0f) * Settings.scale;
        } else if (this.x < -1920.0f * Settings.xScale) {
            this.x = 1920.0f * Settings.xScale;
            this.vX = MathUtils.random(-50.0f, -10.0f) * Settings.xScale;
            this.y = (float)Settings.HEIGHT - 1100.0f * Settings.scale + MathUtils.random(-50.0f, 50.0f) * Settings.scale;
            this.vY = MathUtils.random(-this.vX / 5.0f, this.vX / 5.0f) * Settings.scale;
        }
    }

    public void render(SpriteBatch sb, float slider) {
        this.renderRegion(sb, this.region, this.x, (-55.0f + this.sliderJiggle) * Settings.scale * slider + this.y);
    }

    private void renderRegion(SpriteBatch sb, TextureAtlas.AtlasRegion region, float x, float y) {
        sb.draw(region.getTexture(), region.offsetX * Settings.scale + x, region.offsetY * Settings.scale + y, 0.0f, 0.0f, region.packedWidth, region.packedHeight, Settings.scale, Settings.scale, 0.0f, region.getRegionX(), region.getRegionY(), region.getRegionWidth(), region.getRegionHeight(), false, false);
    }
}


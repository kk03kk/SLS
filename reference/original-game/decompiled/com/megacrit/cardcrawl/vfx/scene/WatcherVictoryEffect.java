/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class WatcherVictoryEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private static TextureAtlas.AtlasRegion img;

    public WatcherVictoryEffect() {
        this.renderBehind = true;
        img = ImageMaster.EYE_ANIM_0;
        this.x = (float)Settings.WIDTH / 2.0f - (float)WatcherVictoryEffect.img.packedWidth / 2.0f;
        this.y = (float)Settings.HEIGHT / 2.0f - (float)WatcherVictoryEffect.img.packedHeight / 2.0f;
        this.scale = 1.5f * Settings.scale;
        this.color = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public void update() {
        this.color.a = MathHelper.slowColorLerpSnap(this.color.a, 1.0f);
        this.duration += Gdx.graphics.getDeltaTime();
        this.rotation += 5.0f * Gdx.graphics.getDeltaTime();
    }

    private void renderHelper(SpriteBatch sb, float offsetX, float offsetY, float rotation, Color color, float scaleMod) {
        sb.setColor(color);
        sb.draw(this.getImg(this.rotation + rotation + (offsetX *= Settings.scale) / 100.0f), this.x, this.y, (float)WatcherVictoryEffect.img.packedWidth / 2.0f - offsetX, (float)WatcherVictoryEffect.img.packedHeight / 2.0f - (offsetY *= Settings.scale), WatcherVictoryEffect.img.packedWidth, WatcherVictoryEffect.img.packedHeight, this.scale, this.scale * 2.0f, this.rotation + rotation);
    }

    private TextureAtlas.AtlasRegion getImg(float input) {
        if ((input %= 10.0f) < 0.5f) {
            return ImageMaster.EYE_ANIM_1;
        }
        if (input < 1.2f) {
            return ImageMaster.EYE_ANIM_2;
        }
        if (input < 2.0f) {
            return ImageMaster.EYE_ANIM_3;
        }
        if (input < 3.0f) {
            return ImageMaster.EYE_ANIM_4;
        }
        if (input < 4.2f) {
            return ImageMaster.EYE_ANIM_5;
        }
        if (input < 6.0f) {
            return ImageMaster.EYE_ANIM_6;
        }
        if (input < 7.5f) {
            return ImageMaster.EYE_ANIM_5;
        }
        if (input < 8.5f) {
            return ImageMaster.EYE_ANIM_4;
        }
        if (input < 9.3f) {
            return ImageMaster.EYE_ANIM_3;
        }
        return ImageMaster.EYE_ANIM_2;
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        float angle = 0.0f;
        for (int i = 0; i < 24; ++i) {
            this.color.r = 0.9f;
            this.color.g = 0.46f + (float)i * 0.01f;
            this.color.b = 0.3f + (float)(12 - i) * 0.05f;
            this.renderHelper(sb, -760.0f, -760.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, -630.0f, -630.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, -510.0f, -510.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, -400.0f, -400.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, -300.0f, -300.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, -220.0f, -220.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, -170.0f, -170.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, -130.0f, -130.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, -100.0f, -100.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, 760.0f, -760.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, 630.0f, -630.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, 510.0f, -510.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, 400.0f, -400.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, 300.0f, -300.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, 220.0f, -220.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, 170.0f, -170.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, 130.0f, -130.0f, angle, this.color, 1.0f);
            this.renderHelper(sb, 100.0f, -100.0f, angle, this.color, 1.0f);
            angle += 15.0f;
        }
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


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
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class CleaveEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private static final float FADE_IN_TIME = 0.05f;
    private static final float FADE_OUT_TIME = 0.4f;
    private float fadeInTimer = 0.05f;
    private float fadeOutTimer = 0.4f;
    private float stallTimer;
    private TextureAtlas.AtlasRegion img = ImageMaster.vfxAtlas.findRegion("combat/cleave");

    public CleaveEffect(boolean usedByMonster) {
        this.color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
        this.x = (float)Settings.WIDTH * 0.3f - (float)this.img.packedWidth / 2.0f;
        this.y = AbstractDungeon.floorY + 100.0f * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.vX = 100.0f * Settings.scale;
        this.stallTimer = MathUtils.random(0.0f, 0.2f);
        this.scale = 1.2f * Settings.scale;
        this.rotation = MathUtils.random(-5.0f, 1.0f);
    }

    public CleaveEffect() {
        this.color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
        this.x = (float)Settings.WIDTH * 0.7f - (float)this.img.packedWidth / 2.0f;
        this.y = AbstractDungeon.floorY + 100.0f * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.vX = 100.0f * Settings.scale;
        this.stallTimer = MathUtils.random(0.0f, 0.2f);
        this.scale = 1.2f * Settings.scale;
        this.rotation = MathUtils.random(-5.0f, 1.0f);
    }

    @Override
    public void update() {
        if (this.stallTimer > 0.0f) {
            this.stallTimer -= Gdx.graphics.getDeltaTime();
            return;
        }
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.rotation += MathUtils.random(-0.5f, 0.5f);
        this.scale += 0.005f * Settings.scale;
        if (this.fadeInTimer != 0.0f) {
            this.fadeInTimer -= Gdx.graphics.getDeltaTime();
            if (this.fadeInTimer < 0.0f) {
                this.fadeInTimer = 0.0f;
            }
            this.color.a = Interpolation.fade.apply(1.0f, 0.0f, this.fadeInTimer / 0.05f);
        } else if (this.fadeOutTimer != 0.0f) {
            this.fadeOutTimer -= Gdx.graphics.getDeltaTime();
            if (this.fadeOutTimer < 0.0f) {
                this.fadeOutTimer = 0.0f;
            }
            this.color.a = Interpolation.pow2.apply(0.0f, 1.0f, this.fadeOutTimer / 0.4f);
        } else {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


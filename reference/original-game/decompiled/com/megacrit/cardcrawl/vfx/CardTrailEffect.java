/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Pool;
import com.megacrit.cardcrawl.cards.Soul;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class CardTrailEffect
extends AbstractGameEffect
implements Pool.Poolable {
    private static final float EFFECT_DUR = 0.5f;
    private static final float DUR_DIV_2 = 0.25f;
    private static TextureAtlas.AtlasRegion img = null;
    private static final int W = 12;
    private static final int W_DIV_2 = 6;
    private static final float SCALE_MULTI = Settings.scale * 22.0f;
    private float x;
    private float y;

    public CardTrailEffect() {
        if (img == null) {
            img = ImageMaster.vfxAtlas.findRegion("combat/blurDot2");
        }
        this.renderBehind = false;
    }

    public void init(float x, float y) {
        this.duration = 0.5f;
        this.startingDuration = 0.5f;
        this.x = x - 6.0f;
        this.y = y - 6.0f;
        this.color = AbstractDungeon.player.getCardTrailColor();
        this.scale = 0.01f;
        this.isDone = false;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        this.scale = this.duration < 0.25f ? this.duration * SCALE_MULTI : (this.duration - 0.25f) * SCALE_MULTI;
        if (this.duration < 0.0f) {
            this.isDone = true;
            Soul.trailEffectPool.free(this);
        } else {
            this.color.a = Interpolation.fade.apply(0.0f, 0.18f, this.duration / 0.5f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(img, this.x, this.y, 6.0f, 6.0f, 12.0f, 12.0f, this.scale, this.scale, 0.0f);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }

    @Override
    public void reset() {
    }
}


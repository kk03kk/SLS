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
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.BorderLongFlashEffect;

public class LaserBeamEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private static final float DUR = 2.0f;
    private static TextureAtlas.AtlasRegion img;
    private boolean playedSfx = false;

    public LaserBeamEffect(float x, float y) {
        if (img == null) {
            img = ImageMaster.vfxAtlas.findRegion("combat/laserThick");
        }
        this.x = x;
        this.y = y;
        this.color = Color.CYAN.cpy();
        this.duration = 2.0f;
        this.startingDuration = 2.0f;
    }

    @Override
    public void update() {
        if (!this.playedSfx) {
            AbstractDungeon.effectsQueue.add(new BorderLongFlashEffect(Color.SKY));
            this.playedSfx = true;
            CardCrawlGame.sound.play("ATTACK_MAGIC_BEAM");
            CardCrawlGame.screenShake.rumble(2.0f);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.pow2In.apply(1.0f, 0.0f, this.duration - 1.0f) : Interpolation.pow2Out.apply(0.0f, 1.0f, this.duration);
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(new Color(0.5f, 0.7f, 1.0f, this.color.a));
        sb.draw(img, this.x, this.y - (float)(LaserBeamEffect.img.packedHeight / 2), 0.0f, (float)LaserBeamEffect.img.packedHeight / 2.0f, LaserBeamEffect.img.packedWidth, LaserBeamEffect.img.packedHeight, this.scale * 2.0f + MathUtils.random(-0.05f, 0.05f), this.scale * 1.5f + MathUtils.random(-0.1f, 0.1f), MathUtils.random(186.0f, 189.0f));
        sb.draw(img, this.x, this.y - (float)(LaserBeamEffect.img.packedHeight / 2), 0.0f, (float)LaserBeamEffect.img.packedHeight / 2.0f, LaserBeamEffect.img.packedWidth, LaserBeamEffect.img.packedHeight, this.scale * 2.0f + MathUtils.random(-0.05f, 0.05f), this.scale * 1.5f + MathUtils.random(-0.1f, 0.1f), MathUtils.random(186.0f, 189.0f));
        sb.setColor(this.color);
        sb.draw(img, this.x, this.y - (float)(LaserBeamEffect.img.packedHeight / 2), 0.0f, (float)LaserBeamEffect.img.packedHeight / 2.0f, LaserBeamEffect.img.packedWidth, LaserBeamEffect.img.packedHeight, this.scale * 2.0f, this.scale / 2.0f, MathUtils.random(187.0f, 188.0f));
        sb.draw(img, this.x, this.y - (float)(LaserBeamEffect.img.packedHeight / 2), 0.0f, (float)LaserBeamEffect.img.packedHeight / 2.0f, LaserBeamEffect.img.packedWidth, LaserBeamEffect.img.packedHeight, this.scale * 2.0f, this.scale / 2.0f, MathUtils.random(187.0f, 188.0f));
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


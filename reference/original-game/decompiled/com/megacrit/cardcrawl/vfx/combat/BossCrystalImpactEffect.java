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
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class BossCrystalImpactEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img;
    private float x;
    private float y;
    private static final float DUR = 0.5f;

    public BossCrystalImpactEffect(float x, float y) {
        CardCrawlGame.sound.playA("HEART_BEAT", MathUtils.random(0.0f, 0.6f));
        this.img = ImageMaster.CRYSTAL_IMPACT;
        this.x = x - (float)(this.img.packedWidth / 2);
        this.y = y - (float)(this.img.packedHeight / 2);
        this.color = Color.BLACK.cpy();
        this.duration = 0.5f;
        this.scale = 0.01f;
        CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, false);
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
            this.duration = 0.0f;
        }
        this.color.a = Interpolation.pow3Out.apply(0.0f, 1.0f, this.duration / 2.0f);
        this.scale += Gdx.graphics.getDeltaTime() * 8.0f;
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(new Color(1.0f, 0.5f, 1.0f, this.color.a));
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.8f, 1.2f), this.scale * MathUtils.random(0.8f, 1.2f), this.rotation);
        sb.setBlendFunction(770, 771);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.8f, 1.2f), this.scale * MathUtils.random(0.8f, 1.2f), this.rotation);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.8f, 1.2f), this.scale * MathUtils.random(0.8f, 1.2f), this.rotation);
    }

    @Override
    public void dispose() {
    }
}


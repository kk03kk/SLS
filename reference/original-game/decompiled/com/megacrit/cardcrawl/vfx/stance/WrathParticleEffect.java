/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.stance;

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

public class WrathParticleEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vY;
    private float dur_div2;
    private TextureAtlas.AtlasRegion img = ImageMaster.GLOW_SPARK;

    public WrathParticleEffect() {
        this.duration = MathUtils.random(1.3f, 1.8f);
        this.scale = MathUtils.random(0.6f, 1.0f) * Settings.scale;
        this.dur_div2 = this.duration / 2.0f;
        this.color = new Color(MathUtils.random(0.5f, 1.0f), 0.0f, MathUtils.random(0.0f, 0.2f), 0.0f);
        this.x = AbstractDungeon.player.hb.cX + MathUtils.random(-AbstractDungeon.player.hb.width / 2.0f - 30.0f * Settings.scale, AbstractDungeon.player.hb.width / 2.0f + 30.0f * Settings.scale);
        this.y = AbstractDungeon.player.hb.cY + MathUtils.random(-AbstractDungeon.player.hb.height / 2.0f - -10.0f * Settings.scale, AbstractDungeon.player.hb.height / 2.0f - 10.0f * Settings.scale);
        this.x -= (float)this.img.packedWidth / 2.0f;
        this.y -= (float)this.img.packedHeight / 2.0f;
        this.renderBehind = MathUtils.randomBoolean(0.2f + (this.scale - 0.5f));
        this.rotation = MathUtils.random(-8.0f, 8.0f);
    }

    @Override
    public void update() {
        this.color.a = this.duration > this.dur_div2 ? Interpolation.fade.apply(1.0f, 0.0f, (this.duration - this.dur_div2) / this.dur_div2) : Interpolation.fade.apply(0.0f, 1.0f, this.duration / this.dur_div2);
        this.vY += Gdx.graphics.getDeltaTime() * 40.0f * Settings.scale;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x, this.y + this.vY, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 0.8f, (0.1f + (this.dur_div2 * 2.0f - this.duration) * 2.0f * this.scale) * Settings.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


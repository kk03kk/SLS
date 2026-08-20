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

public class WrathStanceChangeParticle
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img = ImageMaster.STRIKE_LINE;
    private float x;
    private float y;
    private float delayTimer;

    public WrathStanceChangeParticle(float playerX) {
        this.duration = this.startingDuration = 1.0f;
        this.color = new Color(1.0f, MathUtils.random(0.1f, 0.3f), 0.1f, 0.0f);
        this.x = MathUtils.random(-30.0f, 30.0f) * Settings.scale - (float)this.img.packedWidth / 2.0f;
        this.y = (float)Settings.HEIGHT / 2.0f + MathUtils.random(-150.0f, 150.0f) * Settings.scale - (float)this.img.packedHeight / 2.0f;
        this.scale = MathUtils.random(2.2f, 2.5f) * Settings.scale;
        this.delayTimer = MathUtils.random(0.5f);
        this.rotation = MathUtils.random(89.0f, 91.0f);
        this.renderBehind = MathUtils.randomBoolean(0.9f);
    }

    @Override
    public void update() {
        if (this.delayTimer > 0.0f) {
            this.delayTimer -= Gdx.graphics.getDeltaTime();
            return;
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
            return;
        }
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.pow3In.apply(0.6f, 0.0f, (this.duration - this.startingDuration / 2.0f) / (this.startingDuration / 2.0f)) : Interpolation.fade.apply(0.0f, 0.6f, this.duration / (this.startingDuration / 2.0f));
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.delayTimer > 0.0f) {
            return;
        }
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, AbstractDungeon.player.hb.cX + this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(2.9f, 3.1f), this.scale * MathUtils.random(0.95f, 1.05f), this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


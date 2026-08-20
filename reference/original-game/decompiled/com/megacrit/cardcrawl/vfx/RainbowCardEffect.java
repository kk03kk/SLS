/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class RainbowCardEffect
extends AbstractGameEffect {
    float x;
    float y;
    private TextureAtlas.AtlasRegion img;

    public RainbowCardEffect() {
        if (this.img == null) {
            this.img = ImageMaster.CRYSTAL_IMPACT;
        }
        this.x = AbstractDungeon.player.hb.cX - (float)this.img.packedWidth / 2.0f;
        this.y = AbstractDungeon.player.hb.cY - (float)this.img.packedHeight / 2.0f;
        this.duration = this.startingDuration = 1.5f;
        this.scale = Settings.scale;
        this.color = Color.CYAN.cpy();
        this.color.a = 0.0f;
        this.renderBehind = true;
    }

    @Override
    public void update() {
        if (this.duration == this.startingDuration) {
            CardCrawlGame.sound.playA("HEAL_3", 0.5f);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.fade.apply(1.0f, 0.01f, this.duration - this.startingDuration / 2.0f) * Settings.scale : Interpolation.fade.apply(0.01f, 1.0f, this.duration / (this.startingDuration / 2.0f)) * Settings.scale;
        this.scale = Interpolation.elasticIn.apply(4.0f, 0.01f, this.duration / this.startingDuration) * Settings.scale;
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(new Color(1.0f, 0.2f, 0.2f, this.color.a));
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 1.15f, this.scale * 1.15f, 0.0f);
        sb.setColor(new Color(1.0f, 1.0f, 0.2f, this.color.a));
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, 0.0f);
        sb.setColor(new Color(0.2f, 1.0f, 0.2f, this.color.a));
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 0.85f, this.scale * 0.85f, 0.0f);
        sb.setColor(new Color(0.2f, 0.7f, 1.0f, this.color.a));
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 0.7f, this.scale * 0.7f, 0.0f);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


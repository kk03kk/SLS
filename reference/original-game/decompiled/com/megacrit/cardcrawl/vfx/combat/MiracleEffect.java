/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

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

public class MiracleEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private TextureAtlas.AtlasRegion img;
    private Color altColor;
    private String sfxUrl = "HEAL_3";

    public MiracleEffect(Color setColor, Color altColor, String setSfx) {
        if (this.img == null) {
            this.img = ImageMaster.CRYSTAL_IMPACT;
        }
        this.x = AbstractDungeon.player.hb.cX - (float)this.img.packedWidth / 2.0f;
        this.y = AbstractDungeon.player.hb.cY - (float)this.img.packedHeight / 2.0f;
        this.duration = this.startingDuration = 0.7f;
        this.scale = Settings.scale;
        this.altColor = altColor;
        this.color = setColor.cpy();
        this.color.a = 0.0f;
        this.renderBehind = false;
        this.sfxUrl = setSfx;
    }

    public MiracleEffect() {
        if (this.img == null) {
            this.img = ImageMaster.CRYSTAL_IMPACT;
        }
        this.x = AbstractDungeon.player.hb.cX - (float)this.img.packedWidth / 2.0f;
        this.y = AbstractDungeon.player.hb.cY - (float)this.img.packedHeight / 2.0f;
        this.duration = this.startingDuration = 0.7f;
        this.scale = Settings.scale;
        this.altColor = new Color(1.0f, 0.6f, 0.2f, 0.0f);
        this.color = Settings.GOLD_COLOR.cpy();
        this.color.a = 0.0f;
        this.renderBehind = false;
    }

    @Override
    public void update() {
        if (this.duration == this.startingDuration) {
            CardCrawlGame.sound.playA(this.sfxUrl, 0.5f);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.fade.apply(1.0f, 0.01f, this.duration - this.startingDuration / 2.0f) * Settings.scale : Interpolation.fade.apply(0.01f, 1.0f, this.duration / (this.startingDuration / 2.0f)) * Settings.scale;
        this.scale = Interpolation.pow5In.apply(2.4f, 0.3f, this.duration / this.startingDuration) * Settings.scale;
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        this.altColor.a = this.color.a;
        sb.setColor(this.altColor);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 1.1f, this.scale * 1.1f, 0.0f);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * 0.9f, this.scale * 0.9f, 0.0f);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


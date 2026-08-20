/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class SpotlightPlayerEffect
extends AbstractGameEffect {
    public SpotlightPlayerEffect() {
        this.duration = 3.0f;
        this.color = new Color(1.0f, 1.0f, 0.8f, 0.5f);
    }

    @Override
    public void update() {
        if (this.duration == 3.0f) {
            CardCrawlGame.sound.playA("INTIMIDATE", -0.6f);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        this.color.a = this.duration > 1.5f ? Interpolation.pow5In.apply(0.5f, 0.0f, (this.duration - 1.5f) / 1.5f) : Interpolation.exp10In.apply(0.0f, 0.5f, this.duration / 1.5f);
        if (this.duration < 0.0f) {
            this.color.a = 0.0f;
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(ImageMaster.SPOTLIGHT_VFX, 0.0f, 0.0f, AbstractDungeon.player.drawX + AbstractDungeon.player.hb_w * 2.0f, (float)Settings.HEIGHT);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


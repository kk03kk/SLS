/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class HeartMegaDebuffEffect
extends AbstractGameEffect {
    public HeartMegaDebuffEffect() {
        this.duration = this.startingDuration = 4.0f;
        this.color = new Color(0.9f, 0.7f, 1.0f, 0.0f);
        this.renderBehind = true;
    }

    @Override
    public void update() {
        if (this.duration == this.startingDuration) {
            CardCrawlGame.sound.playA("GHOST_ORB_IGNITE_1", -0.6f);
        }
        this.color.a = this.duration > this.startingDuration / 2.0f ? Interpolation.bounceIn.apply(1.0f, 0.0f, (this.duration - this.startingDuration / 2.0f) / (this.startingDuration / 2.0f)) : Interpolation.bounceOut.apply(this.duration * (this.startingDuration / 2.0f));
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(new Color(0.0f, 0.0f, 0.0f, this.color.a * 0.8f));
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        sb.setColor(this.color);
        sb.draw(ImageMaster.BORDER_GLOW_2, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
    }

    @Override
    public void dispose() {
    }
}


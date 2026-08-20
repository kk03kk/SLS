/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class FlashIntentParticle
extends AbstractGameEffect {
    private static final float DURATION = 1.0f;
    private static final float START_SCALE = 5.0f * Settings.scale;
    private float scale = 0.01f;
    private static int W;
    private Texture img;
    private float x;
    private float y;
    private Color shineColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);

    public FlashIntentParticle(Texture img, AbstractMonster m) {
        this.duration = 1.0f;
        this.img = img;
        W = img.getWidth();
        this.x = m.intentHb.cX - (float)W / 2.0f;
        this.y = m.intentHb.cY - (float)W / 2.0f;
        this.renderBehind = true;
    }

    @Override
    public void update() {
        this.scale = Interpolation.fade.apply(START_SCALE, 0.01f, this.duration);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb, float x, float y) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        this.shineColor.a = this.duration / 2.0f;
        sb.setColor(this.shineColor);
        sb.draw(this.img, this.x, this.y, (float)W / 2.0f, (float)W / 2.0f, W, W, this.scale, this.scale, 0.0f, 0, 0, W, W, false, false);
        sb.setBlendFunction(770, 771);
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class DarkOrbActivateParticle
extends AbstractGameEffect {
    private float x;
    private float y;
    private float scaleY;
    private float aV;
    private static final int W = 140;
    private boolean flipHorizontal;
    private boolean flipVertical;

    public DarkOrbActivateParticle(float x, float y) {
        this.x = x;
        this.y = y;
        this.renderBehind = true;
        this.duration = 0.25f;
        this.startingDuration = 0.25f;
        this.color = new Color(MathUtils.random(0.5f, 1.0f), MathUtils.random(0.6f, 1.0f), 1.0f, 0.5f);
        this.scale = MathUtils.random(1.0f, 2.0f) * Settings.scale;
        this.rotation = MathUtils.random(-8.0f, 8.0f);
        this.flipHorizontal = MathUtils.randomBoolean();
        this.flipVertical = MathUtils.randomBoolean();
        this.scale = Settings.scale;
        this.scaleY = 2.0f * Settings.scale;
        this.aV = MathUtils.random(-100.0f, 100.0f);
    }

    @Override
    public void update() {
        this.rotation += Gdx.graphics.getDeltaTime() * this.aV;
        this.scale = Interpolation.pow4Out.apply(5.0f, 1.0f, this.duration * 4.0f) * Settings.scale;
        this.scaleY = Interpolation.bounceOut.apply(0.2f, 2.0f, this.duration * 4.0f) * Settings.scale;
        this.color.a = Interpolation.pow5Out.apply(0.01f, 0.5f, this.duration * 4.0f);
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(ImageMaster.DARK_ORB_ACTIVATE_VFX, this.x - 70.0f, this.y - 70.0f, 70.0f, 70.0f, 140.0f, 140.0f, this.scale, this.scaleY, this.rotation, 0, 0, 140, 140, this.flipHorizontal, this.flipVertical);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


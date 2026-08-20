/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class DoorFlashEffect
extends AbstractGameEffect {
    private Texture img;
    private float yOffset = 0.0f;

    public DoorFlashEffect(Texture img, boolean eventVersion) {
        this.img = img;
        this.duration = this.startingDuration = 1.3f;
        this.color = Color.WHITE.cpy();
        this.scale = Settings.scale * 2.0f;
        this.yOffset = eventVersion ? -48.0f * Settings.scale : 0.0f;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.duration = 0.0f;
            this.isDone = true;
        }
        this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.duration / this.startingDuration);
        this.scale = Interpolation.swingIn.apply(0.95f, 1.3f, this.duration / this.startingDuration) * Settings.scale;
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.setBlendFunction(770, 1);
        sb.draw(this.img, (float)Settings.WIDTH / 2.0f - 960.0f, (float)Settings.HEIGHT / 2.0f - 600.0f + this.yOffset, 960.0f, 600.0f, 1920.0f, 1200.0f, this.scale, this.scale, 0.0f, 0, 0, 1920, 1200, false, false);
        sb.draw(this.img, (float)Settings.WIDTH / 2.0f - 960.0f, (float)Settings.HEIGHT / 2.0f - 600.0f + this.yOffset, 960.0f, 600.0f, 1920.0f, 1200.0f, this.scale * 1.1f, this.scale * 1.1f, 0.0f, 0, 0, 1920, 1200, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
        this.img.dispose();
    }
}


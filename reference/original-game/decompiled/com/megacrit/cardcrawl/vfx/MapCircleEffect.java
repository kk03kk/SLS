/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class MapCircleEffect
extends AbstractGameEffect {
    public static Texture img;
    private float x;
    private float y;
    public static final int W = 192;

    public MapCircleEffect(float x, float y, float angle) {
        img = ImageMaster.MAP_CIRCLE_1;
        this.x = x;
        this.y = y;
        this.scale = Settings.scale;
        this.duration = 1.2f;
        this.startingDuration = 1.2f;
        this.scale = 3.0f * Settings.scale;
        this.rotation = angle;
    }

    @Override
    public void update() {
        if (Settings.FAST_MODE) {
            this.duration -= Gdx.graphics.getDeltaTime();
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 1.0f) {
            img = ImageMaster.MAP_CIRCLE_5;
        } else if (this.duration < 1.05f) {
            img = ImageMaster.MAP_CIRCLE_4;
        } else if (this.duration < 1.1f) {
            img = ImageMaster.MAP_CIRCLE_3;
        } else if (this.duration < 1.15f) {
            img = ImageMaster.MAP_CIRCLE_2;
        }
        this.scale = MathHelper.scaleLerpSnap(this.scale, 1.5f * Settings.scale);
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(new Color(0.09f, 0.13f, 0.17f, 1.0f));
        sb.draw(img, this.x - 96.0f, this.y - 96.0f, 96.0f, 96.0f, 192.0f, 192.0f, this.scale, this.scale, this.rotation, 0, 0, 192, 192, false, false);
    }

    @Override
    public void dispose() {
    }
}


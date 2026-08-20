/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class FlameAnimationEffect
extends AbstractGameEffect {
    public Texture img = null;
    private static final int W = 256;
    private static final float DUR = 0.5f;
    private static boolean alternator = true;
    private boolean flipped = false;
    private Hitbox nodeHb;
    private float offsetX;
    private float offsetY;

    public FlameAnimationEffect(Hitbox hb) {
        this.nodeHb = hb;
        this.startingDuration = 0.5f;
        this.duration = 0.5f;
        this.scale = MathUtils.random(0.9f, 1.3f) * Settings.scale;
        this.rotation = MathUtils.random(-30.0f, 30.0f);
        this.offsetX = MathUtils.random(0.0f, 8.0f) * Settings.scale;
        this.offsetY = MathUtils.random(-3.0f, 14.0f) * Settings.scale;
        this.flipped = alternator = !alternator;
        if (!alternator) {
            this.offsetX = -this.offsetX;
        }
        this.color = new Color(0.34f, 0.34f, 0.34f, 1.0f);
        this.color = this.color.cpy();
        this.img = ImageMaster.FLAME_ANIM_1;
    }

    @Override
    public void update() {
        this.color.a = this.duration / 0.5f;
        if (this.duration < 0.1f) {
            this.img = null;
        } else if (this.duration < 0.0f) {
            this.img = ImageMaster.FLAME_ANIM_6;
        } else if (this.duration < 0.1f) {
            this.img = ImageMaster.FLAME_ANIM_5;
        } else if (this.duration < 0.2f) {
            this.img = ImageMaster.FLAME_ANIM_4;
        } else if (this.duration < 0.3f) {
            this.img = ImageMaster.FLAME_ANIM_3;
        } else if (this.duration < 0.4f) {
            this.img = ImageMaster.FLAME_ANIM_2;
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    public void render(SpriteBatch sb, float s) {
        sb.setColor(this.color);
        if (this.img != null) {
            sb.draw(this.img, this.nodeHb.cX - 128.0f + this.offsetX, this.nodeHb.cY - 128.0f + this.offsetY, 128.0f, 128.0f, 256.0f, 256.0f, s * this.scale, s * this.scale, this.rotation, 0, 0, 256, 256, this.flipped, false);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}


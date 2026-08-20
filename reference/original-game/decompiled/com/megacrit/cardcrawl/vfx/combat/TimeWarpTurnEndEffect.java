/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class TimeWarpTurnEndEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private static TextureAtlas.AtlasRegion img = null;

    public TimeWarpTurnEndEffect() {
        if (img == null) {
            img = AbstractPower.atlas.findRegion("128/time");
        }
        this.duration = this.startingDuration = 2.0f;
        this.scale = Settings.scale * 3.0f;
        this.x = (float)Settings.WIDTH * 0.5f - (float)TimeWarpTurnEndEffect.img.packedWidth / 2.0f;
        this.y = (float)TimeWarpTurnEndEffect.img.packedHeight / 2.0f;
        this.color = Color.WHITE.cpy();
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        if (this.duration < 1.0f) {
            this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.duration);
        } else {
            this.y = Interpolation.swingIn.apply((float)Settings.HEIGHT * 0.7f - (float)TimeWarpTurnEndEffect.img.packedHeight / 2.0f, (float)(-TimeWarpTurnEndEffect.img.packedHeight) / 2.0f, this.duration - 1.0f);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(img, this.x, this.y, (float)TimeWarpTurnEndEffect.img.packedWidth / 2.0f, (float)TimeWarpTurnEndEffect.img.packedHeight / 2.0f, TimeWarpTurnEndEffect.img.packedWidth, TimeWarpTurnEndEffect.img.packedHeight, this.scale, this.scale, this.duration * 360.0f);
    }

    @Override
    public void dispose() {
    }
}


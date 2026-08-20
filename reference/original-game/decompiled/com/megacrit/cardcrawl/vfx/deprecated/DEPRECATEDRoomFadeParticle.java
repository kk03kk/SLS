/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.deprecated;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class DEPRECATEDRoomFadeParticle
extends AbstractGameEffect {
    private float x;
    private float y;
    private TextureAtlas.AtlasRegion img;
    private static final float DUR = 1.0f;

    public DEPRECATEDRoomFadeParticle(float y) {
        this.y = y;
        this.x = (float)Settings.WIDTH + (float)this.img.packedWidth * 1.5f;
        y -= (float)(this.img.packedHeight / 2);
        this.duration = 1.0f;
        this.startingDuration = 1.0f;
        this.color = AbstractDungeon.fadeColor.cpy();
        this.color.a = 1.0f;
        this.scale *= 2.0f;
    }

    @Override
    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        this.x = Interpolation.pow2Out.apply(0.0f - (float)this.img.packedWidth * 1.5f, (float)Settings.WIDTH + (float)this.img.packedWidth * 1.5f, this.duration / 1.0f);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
    }

    @Override
    public void dispose() {
    }
}


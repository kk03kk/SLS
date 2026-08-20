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
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class LightBulbEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float startY;
    private float dstY;
    private TextureAtlas.AtlasRegion img = AbstractPower.atlas.findRegion("128/curiosity");

    public LightBulbEffect(Hitbox hb) {
        this.x = hb.cX - (float)this.img.packedHeight / 2.0f;
        this.y = hb.cY + hb.height / 2.0f - (float)this.img.packedHeight / 2.0f;
        this.startY = this.y - 50.0f * Settings.scale;
        this.dstY = this.y + 70.0f * Settings.scale;
        this.duration = this.startingDuration = 0.8f;
        this.color = Color.WHITE.cpy();
        this.color.a = 0.0f;
    }

    @Override
    public void update() {
        this.y = Interpolation.swingIn.apply(this.dstY, this.startY, this.duration * (1.0f / this.startingDuration));
        this.duration -= Gdx.graphics.getDeltaTime();
        this.color.a = this.duration < this.startingDuration * 0.8f ? Interpolation.fade.apply(0.0f, 1.0f, this.duration * (1.0f / this.startingDuration / 0.5f)) : MathHelper.fadeLerpSnap(this.color.a, 0.0f);
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale, this.scale, this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


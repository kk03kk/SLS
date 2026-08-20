/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class FlashPowerEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private Texture img;
    private TextureAtlas.AtlasRegion region128;
    private static final int W = 32;
    private float scale = Settings.scale;

    public FlashPowerEffect(AbstractPower power) {
        if (!power.owner.isDeadOrEscaped()) {
            this.x = power.owner.hb.cX;
            this.y = power.owner.hb.cY;
        }
        this.img = power.img;
        this.region128 = power.region128;
        if (this.img == null) {
            this.x -= (float)(this.region128.packedWidth / 2);
            this.y -= (float)(this.region128.packedHeight / 2);
        }
        this.duration = 0.7f;
        this.startingDuration = 0.7f;
        this.color = Color.WHITE.cpy();
        this.renderBehind = false;
    }

    @Override
    public void update() {
        super.update();
        this.scale = Interpolation.exp5In.apply(Settings.scale, Settings.scale * 0.3f, this.duration / this.startingDuration);
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        if (this.img != null) {
            sb.draw(this.img, this.x - 16.0f, this.y - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, this.scale * 12.0f, this.scale * 12.0f, 0.0f, 0, 0, 32, 32, false, false);
            sb.draw(this.img, this.x - 16.0f, this.y - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, this.scale * 10.0f, this.scale * 10.0f, 0.0f, 0, 0, 32, 32, false, false);
            sb.draw(this.img, this.x - 16.0f, this.y - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, this.scale * 8.0f, this.scale * 8.0f, 0.0f, 0, 0, 32, 32, false, false);
            sb.draw(this.img, this.x - 16.0f, this.y - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, this.scale * 7.0f, this.scale * 7.0f, 0.0f, 0, 0, 32, 32, false, false);
        } else {
            sb.draw(this.region128, this.x, this.y, (float)this.region128.packedWidth / 2.0f, (float)this.region128.packedHeight / 2.0f, this.region128.packedWidth, this.region128.packedHeight, this.scale * 3.0f, this.scale * 3.0f, 0.0f);
        }
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


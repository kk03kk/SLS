/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class PowerIconShowEffect
extends AbstractGameEffect {
    private static final float DUR = 2.2f;
    private float x;
    private float y;
    private float offsetY;
    private Texture img;
    private static final float STARTING_OFFSET_Y = 130.0f * Settings.scale;
    private static final float TARGET_OFFSET_Y = 170.0f * Settings.scale;
    private static final float LERP_RATE = 5.0f;
    private static final int W = 32;
    private Color shineColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);

    public PowerIconShowEffect(AbstractPower power) {
        if (!power.owner.isDeadOrEscaped()) {
            this.x = power.owner.hb.cX;
            this.y = power.owner.hb.cY + power.owner.hb.height / 2.0f;
        }
        this.img = power.img;
        this.duration = 2.2f;
        this.startingDuration = 2.2f;
        this.offsetY = STARTING_OFFSET_Y;
        this.color = Color.WHITE.cpy();
        this.renderBehind = true;
    }

    @Override
    public void update() {
        super.update();
        this.offsetY = MathUtils.lerp(this.offsetY, TARGET_OFFSET_Y, Gdx.graphics.getDeltaTime() * 5.0f);
        this.y += Gdx.graphics.getDeltaTime() * 12.0f * Settings.scale;
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.img != null) {
            this.shineColor.a = this.color.a / 2.0f;
            sb.setColor(this.shineColor);
            sb.setBlendFunction(770, 1);
            sb.draw(this.img, this.x - 16.0f, this.y - 16.0f + this.offsetY, 16.0f, 16.0f, 32.0f, 32.0f, Settings.scale * 2.5f, Settings.scale * 2.5f, 0.0f, 0, 0, 32, 32, false, false);
            sb.setBlendFunction(770, 771);
            sb.setColor(this.color);
            sb.draw(this.img, this.x - 16.0f, this.y - 16.0f + this.offsetY, 16.0f, 16.0f, 32.0f, 32.0f, Settings.scale * 2.0f, Settings.scale * 2.0f, 0.0f, 0, 0, 32, 32, false, false);
        }
    }

    @Override
    public void dispose() {
    }
}


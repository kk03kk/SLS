/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class IceShatterEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float vY;
    private Texture img = MathUtils.randomBoolean() ? ImageMaster.FROST_ACTIVATE_VFX_1 : ImageMaster.FROST_ACTIVATE_VFX_2;

    public IceShatterEffect(float x, float y) {
        this.x = x;
        this.y = y;
        this.vX = MathUtils.random(-300.0f, 300.0f) * Settings.scale;
        this.vY = MathUtils.random(-900.0f, 200.0f) * Settings.scale;
        this.duration = 0.5f;
        this.scale = MathUtils.random(0.75f, 1.25f) * Settings.scale;
        this.color = new Color(0.5f, 0.8f, 1.0f, MathUtils.random(0.9f, 1.0f));
        Vector2 derp = new Vector2(this.vX, this.vY);
        this.rotation = derp.angle() - 45.0f;
    }

    @Override
    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y -= this.vY * Gdx.graphics.getDeltaTime();
        this.rotation += Gdx.graphics.getDeltaTime() * this.vX;
        this.vY += 2000.0f * Settings.scale * Gdx.graphics.getDeltaTime();
        this.color.a = this.duration * 2.0f;
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, this.rotation, 0, 0, 64, 64, false, false);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


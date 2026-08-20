/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class EmpowerCircleEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float vY;
    private TextureAtlas.AtlasRegion img;

    public EmpowerCircleEffect(float x, float y) {
        this.startingDuration = this.duration = MathUtils.random(0.8f, 3.2f);
        this.img = MathUtils.randomBoolean() ? ImageMaster.POWER_UP_1 : ImageMaster.POWER_UP_2;
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.y = y - (float)this.img.packedHeight / 2.0f;
        this.vX = MathUtils.random(-6000.0f * Settings.scale, 6000.0f * Settings.scale);
        this.vY = MathUtils.random(-6000.0f * Settings.scale, 6000.0f * Settings.scale);
        this.rotation = new Vector2(this.vX, this.vY).angle();
        this.color = MathUtils.randomBoolean() ? Settings.CREAM_COLOR.cpy() : Color.SLATE.cpy();
        this.renderBehind = true;
    }

    @Override
    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.vX = MathHelper.fadeLerpSnap(this.vX, 0.0f);
        this.vY = MathHelper.fadeLerpSnap(this.vY, 0.0f);
        this.scale = Settings.scale * this.duration / this.startingDuration;
        super.update();
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.isDone) {
            sb.setColor(this.color);
            sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(0.9f, 1.1f), this.scale * MathUtils.random(0.9f, 1.1f), this.rotation);
        }
    }

    @Override
    public void dispose() {
    }
}


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
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

public class BlockImpactLineEffect
extends AbstractGameEffect {
    private static final float EFFECT_DUR = 0.5f;
    private float x;
    private float y;
    private Vector2 speedVector;
    private float speed;
    private TextureAtlas.AtlasRegion img = MathUtils.randomBoolean() ? ImageMaster.STRIKE_LINE : ImageMaster.STRIKE_LINE_2;

    public BlockImpactLineEffect(float x, float y) {
        this.duration = 0.5f;
        this.startingDuration = 0.5f;
        this.x = x - (float)this.img.packedWidth / 2.0f;
        this.y = y - (float)this.img.packedHeight / 2.0f;
        this.speed = MathUtils.random(20.0f * Settings.scale, 40.0f * Settings.scale);
        this.speedVector = new Vector2(MathUtils.random(-1.0f, 1.0f), MathUtils.random(-1.0f, 1.0f));
        this.speedVector.nor();
        this.speedVector.angle();
        this.rotation = this.speedVector.angle();
        this.speedVector.x *= this.speed;
        this.speedVector.y *= this.speed;
        this.color = MathUtils.randomBoolean() ? Color.LIGHT_GRAY.cpy() : Color.CYAN.cpy();
    }

    @Override
    public void update() {
        this.speed -= Gdx.graphics.getDeltaTime() * 60.0f;
        this.speedVector.nor();
        this.speedVector.x *= this.speed;
        this.speedVector.y *= this.speed;
        this.x += this.speedVector.x * Gdx.graphics.getDeltaTime() * 60.0f;
        this.y += this.speedVector.y * Gdx.graphics.getDeltaTime() * 60.0f;
        this.scale = Settings.scale * this.duration / 0.5f;
        super.update();
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


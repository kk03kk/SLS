/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import java.util.ArrayList;

public class FireFlyEffect
extends AbstractGameEffect {
    private float x;
    private float y;
    private float vX;
    private float vY;
    private float aX;
    private float waveDst;
    private float baseAlpha;
    private float trailTimer = 0.0f;
    private static final float TRAIL_TIME = 0.04f;
    private static final int TRAIL_MAX_AMT = 30;
    private TextureAtlas.AtlasRegion img;
    private Color setColor;
    private ArrayList<Vector2> prevPositions = new ArrayList();

    public FireFlyEffect(Color setColor) {
        this.duration = this.startingDuration = MathUtils.random(6.0f, 14.0f);
        this.setColor = setColor;
        this.img = ImageMaster.STRIKE_BLUR;
        this.x = (float)MathUtils.random(0, Settings.WIDTH) - (float)this.img.packedWidth / 2.0f;
        this.y = MathUtils.random(-100.0f, 400.0f) * Settings.scale + AbstractDungeon.floorY - (float)this.img.packedHeight / 2.0f;
        this.vX = MathUtils.random(18.0f, 90.0f) * Settings.scale;
        this.aX = MathUtils.random(-5.0f, 5.0f) * Settings.scale;
        this.waveDst = this.vX * MathUtils.random(0.03f, 0.07f);
        this.scale = Settings.scale * this.vX / 60.0f;
        if (MathUtils.randomBoolean()) {
            this.vX = -this.vX;
        }
        this.vY = MathUtils.random(-36.0f, 36.0f) * Settings.scale;
        this.color = setColor.cpy();
        this.baseAlpha = 0.25f;
        this.color.a = 0.0f;
    }

    @Override
    public void update() {
        this.trailTimer -= Gdx.graphics.getDeltaTime();
        if (this.trailTimer < 0.0f) {
            this.trailTimer = 0.04f;
            this.prevPositions.add(new Vector2(this.x, this.y));
            if (this.prevPositions.size() > 30) {
                this.prevPositions.remove(0);
            }
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.vX += this.aX * Gdx.graphics.getDeltaTime();
        if (!this.prevPositions.isEmpty() && (this.prevPositions.get((int)0).x < 0.0f || this.prevPositions.get((int)0).x > (float)Settings.WIDTH)) {
            this.isDone = true;
        }
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        this.y += MathUtils.sin(this.duration * this.waveDst) * this.waveDst / 4.0f * Gdx.graphics.getDeltaTime() * 60.0f;
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
        if (this.duration > this.startingDuration / 2.0f) {
            float tmp = this.duration - this.startingDuration / 2.0f;
            this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.startingDuration / 2.0f - tmp) * this.baseAlpha;
        } else {
            this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.duration / (this.startingDuration / 2.0f)) * this.baseAlpha;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        this.setColor.a = this.color.a;
        for (int i = this.prevPositions.size() - 1; i > 0; --i) {
            this.setColor.a *= 0.95f;
            sb.setColor(this.setColor);
            sb.draw(this.img, this.prevPositions.get((int)i).x, this.prevPositions.get((int)i).y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * (float)(i + 5) / (float)this.prevPositions.size(), this.scale * (float)(i + 5) / (float)this.prevPositions.size(), this.rotation);
        }
        sb.setColor(this.color);
        sb.draw(this.img, this.x, this.y, (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, this.scale * MathUtils.random(2.5f, 3.0f), this.scale * MathUtils.random(2.5f, 3.0f), this.rotation);
        sb.setBlendFunction(770, 771);
    }

    @Override
    public void dispose() {
    }
}


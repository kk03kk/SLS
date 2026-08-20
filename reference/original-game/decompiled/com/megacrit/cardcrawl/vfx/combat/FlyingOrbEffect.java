/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.CatmullRomSpline;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import java.util.ArrayList;

public class FlyingOrbEffect
extends AbstractGameEffect {
    private TextureAtlas.AtlasRegion img;
    private CatmullRomSpline<Vector2> crs = new CatmullRomSpline();
    private ArrayList<Vector2> controlPoints = new ArrayList();
    private static final int TRAIL_ACCURACY = 60;
    private Vector2[] points = new Vector2[60];
    private Vector2 pos;
    private Vector2 target;
    private float currentSpeed = 0.0f;
    private static final float START_VELOCITY = 100.0f * Settings.scale;
    private static final float MAX_VELOCITY = 5000.0f * Settings.scale;
    private static final float VELOCITY_RAMP_RATE = 2000.0f * Settings.scale;
    private static final float DST_THRESHOLD = 36.0f * Settings.scale;
    private static final float HOME_IN_THRESHOLD = 36.0f * Settings.scale;
    private float rotation;
    private boolean rotateClockwise = true;
    private boolean stopRotating = false;
    private float rotationRate;

    public FlyingOrbEffect(float x, float y) {
        this.img = ImageMaster.GLOW_SPARK_2;
        this.pos = new Vector2(x, y);
        this.target = new Vector2(AbstractDungeon.player.hb.cX - DST_THRESHOLD / 3.0f - 100.0f * Settings.scale, AbstractDungeon.player.hb.cY + MathUtils.random(-50.0f, 50.0f) * Settings.scale);
        this.crs.controlPoints = new Vector2[1];
        this.rotateClockwise = MathUtils.randomBoolean();
        this.rotation = MathUtils.random(0, 359);
        this.controlPoints.clear();
        this.rotationRate = MathUtils.random(300.0f, 350.0f) * Settings.scale;
        this.currentSpeed = START_VELOCITY * MathUtils.random(0.2f, 1.0f);
        this.color = new Color(1.0f, 0.15f, 0.2f, 0.4f);
        this.duration = 1.3f;
    }

    @Override
    public void update() {
        this.updateMovement();
    }

    private void updateMovement() {
        Vector2 tmp = new Vector2(this.pos.x - this.target.x, this.pos.y - this.target.y);
        tmp.nor();
        float targetAngle = tmp.angle();
        this.rotationRate += Gdx.graphics.getDeltaTime() * 700.0f;
        if (!this.stopRotating) {
            if (this.rotateClockwise) {
                this.rotation += Gdx.graphics.getDeltaTime() * this.rotationRate;
            } else {
                this.rotation -= Gdx.graphics.getDeltaTime() * this.rotationRate;
                if (this.rotation < 0.0f) {
                    this.rotation += 360.0f;
                }
            }
            this.rotation %= 360.0f;
            if (!this.stopRotating) {
                if (this.target.dst(this.pos) < HOME_IN_THRESHOLD) {
                    this.rotation = targetAngle;
                    this.stopRotating = true;
                } else if (Math.abs(this.rotation - targetAngle) < Gdx.graphics.getDeltaTime() * this.rotationRate) {
                    this.rotation = targetAngle;
                    this.stopRotating = true;
                }
            }
        }
        tmp.setAngle(this.rotation);
        tmp.x *= Gdx.graphics.getDeltaTime() * this.currentSpeed;
        tmp.y *= Gdx.graphics.getDeltaTime() * this.currentSpeed;
        this.pos.sub(tmp);
        this.currentSpeed = this.stopRotating ? (this.currentSpeed += Gdx.graphics.getDeltaTime() * VELOCITY_RAMP_RATE * 3.0f) : (this.currentSpeed += Gdx.graphics.getDeltaTime() * VELOCITY_RAMP_RATE * 1.5f);
        if (this.currentSpeed > MAX_VELOCITY) {
            this.currentSpeed = MAX_VELOCITY;
        }
        if (this.target.x < (float)Settings.WIDTH / 2.0f && this.pos.x < 0.0f || this.target.x > (float)Settings.WIDTH / 2.0f && this.pos.x > (float)Settings.WIDTH || this.target.dst(this.pos) < DST_THRESHOLD) {
            this.isDone = true;
            return;
        }
        if (!this.controlPoints.isEmpty()) {
            if (!this.controlPoints.get(0).equals(this.pos)) {
                this.controlPoints.add(this.pos.cpy());
            }
        } else {
            this.controlPoints.add(this.pos.cpy());
        }
        if (this.controlPoints.size() > 3) {
            Vector2[] vec2Array = new Vector2[]{};
            this.crs.set(this.controlPoints.toArray(vec2Array), false);
            for (int i = 0; i < 60; ++i) {
                this.points[i] = new Vector2();
                this.crs.valueAt(this.points[i], (float)i / 59.0f);
            }
        }
        if (this.controlPoints.size() > 10) {
            this.controlPoints.remove(0);
        }
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < 0.0f) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!this.isDone) {
            sb.setBlendFunction(770, 1);
            sb.setColor(this.color);
            float scale = Settings.scale * 1.5f;
            for (int i = this.points.length - 1; i > 0; --i) {
                if (this.points[i] == null) continue;
                sb.draw(this.img, this.points[i].x - (float)(this.img.packedWidth / 2), this.points[i].y - (float)(this.img.packedHeight / 2), (float)this.img.packedWidth / 2.0f, (float)this.img.packedHeight / 2.0f, this.img.packedWidth, this.img.packedHeight, scale, scale, this.rotation);
                scale *= 0.975f;
            }
            sb.setBlendFunction(770, 771);
        }
    }

    @Override
    public void dispose() {
    }
}


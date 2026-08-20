/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;

public class FloatyEffect {
    public float x;
    public float y;
    public float vX;
    public float vY;
    private float scale;
    private float minV;
    private float maxV;
    private float threshold;
    private static final float SPEED_MIN = 0.4f;
    private static final float SPEED_MAX = 3.0f;
    private static final float EDGE_THRESHOLD = 0.95f;
    private float speedScale;

    public FloatyEffect(float distanceScale, float speedScale) {
        this.scale = distanceScale;
        this.speedScale = speedScale;
        this.minV = 0.4f * this.scale;
        this.maxV = 3.0f * this.scale;
        this.threshold = 0.95f * distanceScale;
        this.vX = MathUtils.random(-this.maxV * speedScale, this.maxV * speedScale);
        this.vY = MathUtils.random(-this.maxV * speedScale, this.maxV * speedScale);
    }

    public void update() {
        this.x += this.vX * Gdx.graphics.getDeltaTime();
        this.y += this.vY * Gdx.graphics.getDeltaTime();
        if (this.y > this.threshold) {
            this.vY = -MathUtils.random(this.minV * this.speedScale, this.maxV * this.speedScale);
        } else if (this.y < -this.threshold) {
            this.vY = MathUtils.random(this.minV * this.speedScale, this.maxV * this.speedScale);
        }
        if (this.x > this.threshold) {
            this.vX = -MathUtils.random(this.minV * this.speedScale, this.maxV * this.speedScale);
        } else if (this.x < -this.threshold) {
            this.vX = MathUtils.random(this.minV * this.speedScale, this.maxV * this.speedScale);
        }
    }
}


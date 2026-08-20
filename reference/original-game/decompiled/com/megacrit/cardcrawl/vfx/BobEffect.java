/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.Settings;

public class BobEffect {
    public float y = 0.0f;
    public float speed;
    public float dist;
    private float timer = MathUtils.random(0.0f, 359.0f);
    private static final float DEFAULT_DIST = 5.0f * Settings.scale;
    private static final float DEFAULT_SPEED = 4.0f;

    public BobEffect() {
        this(DEFAULT_DIST, 4.0f);
    }

    public BobEffect(float speed) {
        this(DEFAULT_DIST, speed);
    }

    public BobEffect(float dist, float speed) {
        this.speed = speed;
        this.dist = dist;
    }

    public void update() {
        this.y = MathUtils.sin(this.timer) * this.dist;
        this.timer += Gdx.graphics.getDeltaTime() * this.speed;
    }
}


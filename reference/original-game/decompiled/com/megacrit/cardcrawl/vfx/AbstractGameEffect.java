/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.megacrit.cardcrawl.core.Settings;

public abstract class AbstractGameEffect
implements Disposable {
    public float duration;
    public float startingDuration;
    protected Color color;
    public boolean isDone = false;
    protected float scale = Settings.scale;
    protected float rotation = 0.0f;
    public boolean renderBehind = false;

    public void update() {
        this.duration -= Gdx.graphics.getDeltaTime();
        if (this.duration < this.startingDuration / 2.0f) {
            this.color.a = this.duration / (this.startingDuration / 2.0f);
        }
        if (this.duration < 0.0f) {
            this.isDone = true;
            this.color.a = 0.0f;
        }
    }

    public abstract void render(SpriteBatch var1);

    public void render(SpriteBatch sb, float x, float y) {
    }

    @Override
    public abstract void dispose();
}


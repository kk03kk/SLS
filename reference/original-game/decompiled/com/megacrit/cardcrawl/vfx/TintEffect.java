/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.vfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

public class TintEffect {
    public Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private Color targetColor = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private float lerpSpeed = 3.0f;

    public void changeColor(Color targetColor, float lerpSpeed) {
        this.targetColor = targetColor;
        this.lerpSpeed = lerpSpeed;
    }

    public void changeColor(Color targetColor) {
        this.changeColor(targetColor, 3.0f);
    }

    public void fadeOut() {
        this.targetColor.set(this.color.r, this.color.g, this.color.b, 0.0f);
        this.lerpSpeed = 3.0f;
    }

    public void update() {
        this.color.lerp(this.targetColor, Gdx.graphics.getDeltaTime() * this.lerpSpeed);
    }
}


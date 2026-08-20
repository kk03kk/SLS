/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Interpolation;

public class SoundInfo {
    public String name;
    public long id;
    public boolean isDone = false;
    private static final float FADE_OUT_DURATION = 5.0f;
    private float fadeDuration = 5.0f;
    public float volumeMultiplier = 1.0f;

    public SoundInfo(String name, long id) {
        this.name = name;
        this.id = id;
    }

    public void update() {
        if (this.fadeDuration != 0.0f) {
            this.fadeDuration -= Gdx.graphics.getDeltaTime();
            this.volumeMultiplier = Interpolation.fade.apply(1.0f, 0.0f, 1.0f - this.fadeDuration / 5.0f);
            if (this.fadeDuration < 0.0f) {
                this.isDone = true;
                this.fadeDuration = 0.0f;
            }
        }
    }
}


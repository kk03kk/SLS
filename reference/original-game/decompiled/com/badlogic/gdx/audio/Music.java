/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.audio;

import com.badlogic.gdx.utils.Disposable;

public interface Music
extends Disposable {
    public void play();

    public void pause();

    public void stop();

    public boolean isPlaying();

    public void setLooping(boolean var1);

    public boolean isLooping();

    public void setVolume(float var1);

    public float getVolume();

    public void setPan(float var1, float var2);

    public void setPosition(float var1);

    public float getPosition();

    @Override
    public void dispose();

    public void setOnCompletionListener(OnCompletionListener var1);

    public static interface OnCompletionListener {
        public void onCompletion(Music var1);
    }
}


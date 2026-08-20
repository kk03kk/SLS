/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.audio;

import com.badlogic.gdx.audio.Music;

public class MockMusic
implements Music {
    @Override
    public void play() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public void setLooping(boolean isLooping) {
    }

    @Override
    public boolean isLooping() {
        return false;
    }

    @Override
    public void setVolume(float volume) {
    }

    @Override
    public float getVolume() {
        return 0.0f;
    }

    @Override
    public void setPan(float pan, float volume) {
    }

    @Override
    public void setPosition(float position) {
    }

    @Override
    public float getPosition() {
        return 0.0f;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void setOnCompletionListener(Music.OnCompletionListener listener) {
    }
}


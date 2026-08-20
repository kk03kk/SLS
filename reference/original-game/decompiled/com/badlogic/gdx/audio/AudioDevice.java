/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.audio;

import com.badlogic.gdx.utils.Disposable;

public interface AudioDevice
extends Disposable {
    public boolean isMono();

    public void writeSamples(short[] var1, int var2, int var3);

    public void writeSamples(float[] var1, int var2, int var3);

    public int getLatency();

    @Override
    public void dispose();

    public void setVolume(float var1);
}


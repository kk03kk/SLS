/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.audio;

import com.badlogic.gdx.utils.Disposable;

public interface AudioRecorder
extends Disposable {
    public void read(short[] var1, int var2, int var3);

    @Override
    public void dispose();
}


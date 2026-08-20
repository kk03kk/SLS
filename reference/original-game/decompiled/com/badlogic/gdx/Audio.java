/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx;

import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.audio.AudioRecorder;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;

public interface Audio {
    public AudioDevice newAudioDevice(int var1, boolean var2);

    public AudioRecorder newAudioRecorder(int var1, boolean var2);

    public Sound newSound(FileHandle var1);

    public Music newMusic(FileHandle var1);
}


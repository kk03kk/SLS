/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl.audio;

import com.badlogic.gdx.audio.AudioRecorder;
import com.badlogic.gdx.utils.GdxRuntimeException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;

public class JavaSoundAudioRecorder
implements AudioRecorder {
    private TargetDataLine line;
    private byte[] buffer = new byte[4096];

    public JavaSoundAudioRecorder(int samplingRate, boolean isMono) {
        try {
            AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, samplingRate, 16, isMono ? 1 : 2, isMono ? 2 : 4, samplingRate, false);
            this.line = AudioSystem.getTargetDataLine(format);
            this.line.open(format, this.buffer.length);
            this.line.start();
        }
        catch (Exception ex) {
            throw new GdxRuntimeException("Error creating JavaSoundAudioRecorder.", ex);
        }
    }

    @Override
    public void read(short[] samples, int offset, int numSamples) {
        if (this.buffer.length < numSamples * 2) {
            this.buffer = new byte[numSamples * 2];
        }
        int toRead = numSamples * 2;
        for (int read = 0; read != toRead; read += this.line.read(this.buffer, read, toRead - read)) {
        }
        int i = 0;
        int j = 0;
        while (i < numSamples * 2) {
            samples[offset + j] = (short)(this.buffer[i + 1] << 8 | this.buffer[i] & 0xFF);
            i += 2;
            ++j;
        }
    }

    @Override
    public void dispose() {
        this.line.close();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl.audio;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

public abstract class OpenALMusic
implements Music {
    private static final int bufferSize = 40960;
    private static final int bufferCount = 3;
    private static final int bytesPerSample = 2;
    private static final byte[] tempBytes = new byte[40960];
    private static final ByteBuffer tempBuffer = BufferUtils.createByteBuffer(40960);
    private final OpenALAudio audio;
    private IntBuffer buffers;
    private int sourceID = -1;
    private int format;
    private int sampleRate;
    private boolean isLooping;
    private boolean isPlaying;
    private float volume = 1.0f;
    private float pan = 0.0f;
    private float renderedSeconds;
    private float secondsPerBuffer;
    protected final FileHandle file;
    protected int bufferOverhead = 0;
    private Music.OnCompletionListener onCompletionListener;

    public OpenALMusic(OpenALAudio audio, FileHandle file) {
        this.audio = audio;
        this.file = file;
        this.onCompletionListener = null;
    }

    protected void setup(int channels, int sampleRate) {
        this.format = channels > 1 ? 4355 : 4353;
        this.sampleRate = sampleRate;
        this.secondsPerBuffer = (float)(40960 - this.bufferOverhead) / (float)(2 * channels * sampleRate);
    }

    @Override
    public void play() {
        if (this.audio.noDevice) {
            return;
        }
        if (this.sourceID == -1) {
            int bufferID;
            this.sourceID = this.audio.obtainSource(true);
            if (this.sourceID == -1) {
                return;
            }
            this.audio.music.add(this);
            if (this.buffers == null) {
                this.buffers = BufferUtils.createIntBuffer(3);
                AL10.alGenBuffers(this.buffers);
                int errorCode = AL10.alGetError();
                if (errorCode != 0) {
                    throw new GdxRuntimeException("Unable to allocate audio buffers. AL Error: " + errorCode);
                }
            }
            AL10.alSourcei(this.sourceID, 4103, 0);
            this.setPan(this.pan, this.volume);
            boolean filled = false;
            for (int i = 0; i < 3 && this.fill(bufferID = this.buffers.get(i)); ++i) {
                filled = true;
                AL10.alSourceQueueBuffers(this.sourceID, bufferID);
            }
            if (!filled && this.onCompletionListener != null) {
                this.onCompletionListener.onCompletion(this);
            }
            if (AL10.alGetError() != 0) {
                this.stop();
                return;
            }
        }
        if (!this.isPlaying) {
            AL10.alSourcePlay(this.sourceID);
            this.isPlaying = true;
        }
    }

    @Override
    public void stop() {
        if (this.audio.noDevice) {
            return;
        }
        if (this.sourceID == -1) {
            return;
        }
        this.audio.music.removeValue(this, true);
        this.reset();
        this.audio.freeSource(this.sourceID);
        this.sourceID = -1;
        this.renderedSeconds = 0.0f;
        this.isPlaying = false;
    }

    @Override
    public void pause() {
        if (this.audio.noDevice) {
            return;
        }
        if (this.sourceID != -1) {
            AL10.alSourcePause(this.sourceID);
        }
        this.isPlaying = false;
    }

    @Override
    public boolean isPlaying() {
        if (this.audio.noDevice) {
            return false;
        }
        if (this.sourceID == -1) {
            return false;
        }
        return this.isPlaying;
    }

    @Override
    public void setLooping(boolean isLooping) {
        this.isLooping = isLooping;
    }

    @Override
    public boolean isLooping() {
        return this.isLooping;
    }

    @Override
    public void setVolume(float volume) {
        this.volume = volume;
        if (this.audio.noDevice) {
            return;
        }
        if (this.sourceID != -1) {
            AL10.alSourcef(this.sourceID, 4106, volume);
        }
    }

    @Override
    public float getVolume() {
        return this.volume;
    }

    @Override
    public void setPan(float pan, float volume) {
        this.volume = volume;
        this.pan = pan;
        if (this.audio.noDevice) {
            return;
        }
        if (this.sourceID == -1) {
            return;
        }
        AL10.alSource3f(this.sourceID, 4100, MathUtils.cos((pan - 1.0f) * (float)Math.PI / 2.0f), 0.0f, MathUtils.sin((pan + 1.0f) * (float)Math.PI / 2.0f));
        AL10.alSourcef(this.sourceID, 4106, volume);
    }

    @Override
    public void setPosition(float position) {
        int bufferID;
        if (this.audio.noDevice) {
            return;
        }
        if (this.sourceID == -1) {
            return;
        }
        boolean wasPlaying = this.isPlaying;
        this.isPlaying = false;
        AL10.alSourceStop(this.sourceID);
        AL10.alSourceUnqueueBuffers(this.sourceID, this.buffers);
        this.renderedSeconds += this.secondsPerBuffer * 3.0f;
        if (position <= this.renderedSeconds) {
            this.reset();
            this.renderedSeconds = 0.0f;
        }
        while (this.renderedSeconds < position - this.secondsPerBuffer && this.read(tempBytes) > 0) {
            this.renderedSeconds += this.secondsPerBuffer;
        }
        boolean filled = false;
        for (int i = 0; i < 3 && this.fill(bufferID = this.buffers.get(i)); ++i) {
            filled = true;
            AL10.alSourceQueueBuffers(this.sourceID, bufferID);
        }
        if (!filled) {
            this.stop();
            if (this.onCompletionListener != null) {
                this.onCompletionListener.onCompletion(this);
            }
        }
        AL10.alSourcef(this.sourceID, 4132, position - this.renderedSeconds);
        if (wasPlaying) {
            AL10.alSourcePlay(this.sourceID);
            this.isPlaying = true;
        }
    }

    @Override
    public float getPosition() {
        if (this.audio.noDevice) {
            return 0.0f;
        }
        if (this.sourceID == -1) {
            return 0.0f;
        }
        return this.renderedSeconds + AL10.alGetSourcef(this.sourceID, 4132);
    }

    public abstract int read(byte[] var1);

    public abstract void reset();

    protected void loop() {
        this.reset();
    }

    public int getChannels() {
        return this.format == 4355 ? 2 : 1;
    }

    public int getRate() {
        return this.sampleRate;
    }

    public void update() {
        int bufferID;
        if (this.audio.noDevice) {
            return;
        }
        if (this.sourceID == -1) {
            return;
        }
        boolean end = false;
        int buffers = AL10.alGetSourcei(this.sourceID, 4118);
        while (buffers-- > 0 && (bufferID = AL10.alSourceUnqueueBuffers(this.sourceID)) != 40963) {
            this.renderedSeconds += this.secondsPerBuffer;
            if (end) continue;
            if (this.fill(bufferID)) {
                AL10.alSourceQueueBuffers(this.sourceID, bufferID);
                continue;
            }
            end = true;
        }
        if (end && AL10.alGetSourcei(this.sourceID, 4117) == 0) {
            this.stop();
            if (this.onCompletionListener != null) {
                this.onCompletionListener.onCompletion(this);
            }
        }
        if (this.isPlaying && AL10.alGetSourcei(this.sourceID, 4112) != 4114) {
            AL10.alSourcePlay(this.sourceID);
        }
    }

    private boolean fill(int bufferID) {
        tempBuffer.clear();
        int length = this.read(tempBytes);
        if (length <= 0) {
            if (this.isLooping) {
                this.loop();
                this.renderedSeconds = 0.0f;
                length = this.read(tempBytes);
                if (length <= 0) {
                    return false;
                }
            } else {
                return false;
            }
        }
        tempBuffer.put(tempBytes, 0, length).flip();
        AL10.alBufferData(bufferID, this.format, tempBuffer, this.sampleRate);
        return true;
    }

    @Override
    public void dispose() {
        this.stop();
        if (this.audio.noDevice) {
            return;
        }
        if (this.buffers == null) {
            return;
        }
        AL10.alDeleteBuffers(this.buffers);
        this.buffers = null;
        this.onCompletionListener = null;
    }

    @Override
    public void setOnCompletionListener(Music.OnCompletionListener listener) {
        this.onCompletionListener = listener;
    }

    public int getSourceId() {
        return this.sourceID;
    }
}


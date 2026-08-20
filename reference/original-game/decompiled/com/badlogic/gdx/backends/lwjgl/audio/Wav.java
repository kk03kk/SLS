/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl.audio;

import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALMusic;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALSound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;

public class Wav {

    private static class WavInputStream
    extends FilterInputStream {
        int channels;
        int sampleRate;
        int dataRemaining;

        WavInputStream(FileHandle file) {
            super(file.read());
            try {
                if (this.read() != 82 || this.read() != 73 || this.read() != 70 || this.read() != 70) {
                    throw new GdxRuntimeException("RIFF header not found: " + file);
                }
                this.skipFully(4);
                if (this.read() != 87 || this.read() != 65 || this.read() != 86 || this.read() != 69) {
                    throw new GdxRuntimeException("Invalid wave file header: " + file);
                }
                int fmtChunkLength = this.seekToChunk('f', 'm', 't', ' ');
                int type = this.read() & 0xFF | (this.read() & 0xFF) << 8;
                if (type != 1) {
                    throw new GdxRuntimeException("WAV files must be PCM: " + type);
                }
                this.channels = this.read() & 0xFF | (this.read() & 0xFF) << 8;
                if (this.channels != 1 && this.channels != 2) {
                    throw new GdxRuntimeException("WAV files must have 1 or 2 channels: " + this.channels);
                }
                this.sampleRate = this.read() & 0xFF | (this.read() & 0xFF) << 8 | (this.read() & 0xFF) << 16 | (this.read() & 0xFF) << 24;
                this.skipFully(6);
                int bitsPerSample = this.read() & 0xFF | (this.read() & 0xFF) << 8;
                if (bitsPerSample != 16) {
                    throw new GdxRuntimeException("WAV files must have 16 bits per sample: " + bitsPerSample);
                }
                this.skipFully(fmtChunkLength - 16);
                this.dataRemaining = this.seekToChunk('d', 'a', 't', 'a');
            }
            catch (Throwable ex) {
                StreamUtils.closeQuietly(this);
                throw new GdxRuntimeException("Error reading WAV file: " + file, ex);
            }
        }

        private int seekToChunk(char c1, char c2, char c3, char c4) throws IOException {
            while (true) {
                boolean found = this.read() == c1;
                found &= this.read() == c2;
                found &= this.read() == c3;
                found &= this.read() == c4;
                int chunkLength = this.read() & 0xFF | (this.read() & 0xFF) << 8 | (this.read() & 0xFF) << 16 | (this.read() & 0xFF) << 24;
                if (chunkLength == -1) {
                    throw new IOException("Chunk not found: " + c1 + c2 + c3 + c4);
                }
                if (found) {
                    return chunkLength;
                }
                this.skipFully(chunkLength);
            }
        }

        private void skipFully(int count) throws IOException {
            while (count > 0) {
                long skipped = this.in.skip(count);
                if (skipped <= 0L) {
                    throw new EOFException("Unable to skip.");
                }
                count = (int)((long)count - skipped);
            }
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            if (this.dataRemaining == 0) {
                return -1;
            }
            int length = Math.min(super.read(buffer), this.dataRemaining);
            if (length == -1) {
                return -1;
            }
            this.dataRemaining -= length;
            return length;
        }
    }

    public static class Sound
    extends OpenALSound {
        public Sound(OpenALAudio audio, FileHandle file) {
            super(audio);
            if (audio.noDevice) {
                return;
            }
            WavInputStream input = null;
            try {
                input = new WavInputStream(file);
                this.setup(StreamUtils.copyStreamToByteArray(input, input.dataRemaining), input.channels, input.sampleRate);
            }
            catch (IOException ex) {
                try {
                    throw new GdxRuntimeException("Error reading WAV file: " + file, ex);
                }
                catch (Throwable throwable) {
                    StreamUtils.closeQuietly(input);
                    throw throwable;
                }
            }
            StreamUtils.closeQuietly(input);
        }
    }

    public static class Music
    extends OpenALMusic {
        private WavInputStream input;

        public Music(OpenALAudio audio, FileHandle file) {
            super(audio, file);
            this.input = new WavInputStream(file);
            if (audio.noDevice) {
                return;
            }
            this.setup(this.input.channels, this.input.sampleRate);
        }

        @Override
        public int read(byte[] buffer) {
            if (this.input == null) {
                this.input = new WavInputStream(this.file);
                this.setup(this.input.channels, this.input.sampleRate);
            }
            try {
                return this.input.read(buffer);
            }
            catch (IOException ex) {
                throw new GdxRuntimeException("Error reading WAV file: " + this.file, ex);
            }
        }

        @Override
        public void reset() {
            StreamUtils.closeQuietly(this.input);
            this.input = null;
        }
    }
}


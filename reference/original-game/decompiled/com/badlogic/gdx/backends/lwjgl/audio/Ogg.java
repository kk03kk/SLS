/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl.audio;

import com.badlogic.gdx.backends.lwjgl.audio.OggInputStream;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALMusic;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALSound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.StreamUtils;
import java.io.ByteArrayOutputStream;

public class Ogg {

    public static class Sound
    extends OpenALSound {
        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        public Sound(OpenALAudio audio, FileHandle file) {
            super(audio);
            if (audio.noDevice) {
                return;
            }
            OggInputStream input = null;
            try {
                int length;
                input = new OggInputStream(file.read());
                ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
                byte[] buffer = new byte[2048];
                while (!input.atEnd() && (length = input.read(buffer)) != -1) {
                    output.write(buffer, 0, length);
                }
                this.setup(output.toByteArray(), input.getChannels(), input.getSampleRate());
            }
            catch (Throwable throwable) {
                StreamUtils.closeQuietly(input);
                throw throwable;
            }
            StreamUtils.closeQuietly(input);
        }
    }

    public static class Music
    extends OpenALMusic {
        private OggInputStream input;
        private OggInputStream previousInput;

        public Music(OpenALAudio audio, FileHandle file) {
            super(audio, file);
            if (audio.noDevice) {
                return;
            }
            this.input = new OggInputStream(file.read());
            this.setup(this.input.getChannels(), this.input.getSampleRate());
        }

        @Override
        public int read(byte[] buffer) {
            if (this.input == null) {
                this.input = new OggInputStream(this.file.read(), this.previousInput);
                this.setup(this.input.getChannels(), this.input.getSampleRate());
                this.previousInput = null;
            }
            return this.input.read(buffer);
        }

        @Override
        public void reset() {
            StreamUtils.closeQuietly(this.input);
            this.previousInput = null;
            this.input = null;
        }

        @Override
        protected void loop() {
            StreamUtils.closeQuietly(this.input);
            this.previousInput = this.input;
            this.input = null;
        }
    }
}


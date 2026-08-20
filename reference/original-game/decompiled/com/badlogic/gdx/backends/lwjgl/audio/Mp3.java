/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl.audio;

import com.badlogic.gdx.backends.lwjgl.audio.OpenALAudio;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALMusic;
import com.badlogic.gdx.backends.lwjgl.audio.OpenALSound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.ByteArrayOutputStream;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.MP3Decoder;
import javazoom.jl.decoder.OutputBuffer;

public class Mp3 {

    public static class Sound
    extends OpenALSound {
        public Sound(OpenALAudio audio, FileHandle file) {
            super(audio);
            if (audio.noDevice) {
                return;
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream(4096);
            Bitstream bitstream = new Bitstream(file.read());
            MP3Decoder decoder = new MP3Decoder();
            try {
                Header header;
                OutputBuffer outputBuffer = null;
                int sampleRate = -1;
                int channels = -1;
                while ((header = bitstream.readFrame()) != null) {
                    if (outputBuffer == null) {
                        channels = header.mode() == 3 ? 1 : 2;
                        outputBuffer = new OutputBuffer(channels, false);
                        decoder.setOutputBuffer(outputBuffer);
                        sampleRate = header.getSampleRate();
                    }
                    try {
                        decoder.decodeFrame(header, bitstream);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                    bitstream.closeFrame();
                    output.write(outputBuffer.getBuffer(), 0, outputBuffer.reset());
                }
                bitstream.close();
                this.setup(output.toByteArray(), channels, sampleRate);
            }
            catch (Throwable ex) {
                throw new GdxRuntimeException("Error reading audio data.", ex);
            }
        }
    }

    public static class Music
    extends OpenALMusic {
        private Bitstream bitstream;
        private OutputBuffer outputBuffer;
        private MP3Decoder decoder;

        public Music(OpenALAudio audio, FileHandle file) {
            super(audio, file);
            if (audio.noDevice) {
                return;
            }
            this.bitstream = new Bitstream(file.read());
            this.decoder = new MP3Decoder();
            this.bufferOverhead = 4096;
            try {
                Header header = this.bitstream.readFrame();
                if (header == null) {
                    throw new GdxRuntimeException("Empty MP3");
                }
                int channels = header.mode() == 3 ? 1 : 2;
                this.outputBuffer = new OutputBuffer(channels, false);
                this.decoder.setOutputBuffer(this.outputBuffer);
                this.setup(channels, header.getSampleRate());
            }
            catch (BitstreamException e) {
                throw new GdxRuntimeException("error while preloading mp3", e);
            }
        }

        @Override
        public int read(byte[] buffer) {
            try {
                Header header;
                int totalLength;
                int length;
                boolean setup;
                boolean bl = setup = this.bitstream == null;
                if (setup) {
                    this.bitstream = new Bitstream(this.file.read());
                    this.decoder = new MP3Decoder();
                }
                int minRequiredLength = buffer.length - 4608;
                for (totalLength = 0; totalLength <= minRequiredLength && (header = this.bitstream.readFrame()) != null; totalLength += length) {
                    if (setup) {
                        int channels = header.mode() == 3 ? 1 : 2;
                        this.outputBuffer = new OutputBuffer(channels, false);
                        this.decoder.setOutputBuffer(this.outputBuffer);
                        this.setup(channels, header.getSampleRate());
                        setup = false;
                    }
                    try {
                        this.decoder.decodeFrame(header, this.bitstream);
                    }
                    catch (Exception channels) {
                        // empty catch block
                    }
                    this.bitstream.closeFrame();
                    length = this.outputBuffer.reset();
                    System.arraycopy(this.outputBuffer.getBuffer(), 0, buffer, totalLength, length);
                }
                return totalLength;
            }
            catch (Throwable ex) {
                this.reset();
                throw new GdxRuntimeException("Error reading audio data.", ex);
            }
        }

        @Override
        public void reset() {
            if (this.bitstream == null) {
                return;
            }
            try {
                this.bitstream.close();
            }
            catch (BitstreamException bitstreamException) {
                // empty catch block
            }
            this.bitstream = null;
        }
    }
}


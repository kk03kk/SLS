/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.StreamUtils;
import com.jcraft.jogg.Packet;
import com.jcraft.jogg.Page;
import com.jcraft.jogg.StreamState;
import com.jcraft.jogg.SyncState;
import com.jcraft.jorbis.Block;
import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.DspState;
import com.jcraft.jorbis.Info;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.BufferUtils;

public class OggInputStream
extends InputStream {
    private static final int BUFFER_SIZE = 512;
    private int convsize = 2048;
    private byte[] convbuffer;
    private InputStream input;
    private Info oggInfo = new Info();
    private boolean endOfStream;
    private SyncState syncState = new SyncState();
    private StreamState streamState = new StreamState();
    private Page page = new Page();
    private Packet packet = new Packet();
    private Comment comment = new Comment();
    private DspState dspState = new DspState();
    private Block vorbisBlock = new Block(this.dspState);
    byte[] buffer;
    int bytes = 0;
    boolean bigEndian = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
    boolean endOfBitStream = true;
    boolean inited = false;
    private int readIndex;
    private ByteBuffer pcmBuffer;
    private int total;

    public OggInputStream(InputStream input) {
        this(input, null);
    }

    OggInputStream(InputStream input, OggInputStream previousStream) {
        if (previousStream == null) {
            this.convbuffer = new byte[this.convsize];
            this.pcmBuffer = BufferUtils.createByteBuffer(2048000);
        } else {
            this.convbuffer = previousStream.convbuffer;
            this.pcmBuffer = previousStream.pcmBuffer;
        }
        this.input = input;
        try {
            this.total = input.available();
        }
        catch (IOException ex) {
            throw new GdxRuntimeException(ex);
        }
        this.init();
    }

    public int getLength() {
        return this.total;
    }

    public int getChannels() {
        return this.oggInfo.channels;
    }

    public int getSampleRate() {
        return this.oggInfo.rate;
    }

    private void init() {
        this.initVorbis();
        this.readPCM();
    }

    @Override
    public int available() {
        return this.endOfStream ? 0 : 1;
    }

    private void initVorbis() {
        this.syncState.init();
    }

    private boolean getPageAndPacket() {
        int index = this.syncState.buffer(512);
        if (index == -1) {
            return false;
        }
        this.buffer = this.syncState.data;
        if (this.buffer == null) {
            this.endOfStream = true;
            return false;
        }
        try {
            this.bytes = this.input.read(this.buffer, index, 512);
        }
        catch (Exception e) {
            throw new GdxRuntimeException("Failure reading Vorbis.", e);
        }
        this.syncState.wrote(this.bytes);
        if (this.syncState.pageout(this.page) != 1) {
            if (this.bytes < 512) {
                return false;
            }
            throw new GdxRuntimeException("Input does not appear to be an Ogg bitstream.");
        }
        this.streamState.init(this.page.serialno());
        this.oggInfo.init();
        this.comment.init();
        if (this.streamState.pagein(this.page) < 0) {
            throw new GdxRuntimeException("Error reading first page of Ogg bitstream.");
        }
        if (this.streamState.packetout(this.packet) != 1) {
            throw new GdxRuntimeException("Error reading initial header packet.");
        }
        if (this.oggInfo.synthesis_headerin(this.comment, this.packet) < 0) {
            throw new GdxRuntimeException("Ogg bitstream does not contain Vorbis audio data.");
        }
        int i = 0;
        while (i < 2) {
            int result;
            while (i < 2 && (result = this.syncState.pageout(this.page)) != 0) {
                if (result != 1) continue;
                this.streamState.pagein(this.page);
                while (i < 2 && (result = this.streamState.packetout(this.packet)) != 0) {
                    if (result == -1) {
                        throw new GdxRuntimeException("Corrupt secondary header.");
                    }
                    this.oggInfo.synthesis_headerin(this.comment, this.packet);
                    ++i;
                }
            }
            index = this.syncState.buffer(512);
            if (index == -1) {
                return false;
            }
            this.buffer = this.syncState.data;
            try {
                this.bytes = this.input.read(this.buffer, index, 512);
            }
            catch (Exception e) {
                throw new GdxRuntimeException("Failed to read Vorbis.", e);
            }
            if (this.bytes == 0 && i < 2) {
                throw new GdxRuntimeException("End of file before finding all Vorbis headers.");
            }
            this.syncState.wrote(this.bytes);
        }
        this.convsize = 512 / this.oggInfo.channels;
        this.dspState.synthesis_init(this.oggInfo);
        this.vorbisBlock.init(this.dspState);
        return true;
    }

    private void readPCM() {
        boolean wrote = false;
        while (true) {
            if (this.endOfBitStream) {
                if (!this.getPageAndPacket()) break;
                this.endOfBitStream = false;
            }
            if (!this.inited) {
                this.inited = true;
                return;
            }
            float[][][] _pcm = new float[1][][];
            int[] _index = new int[this.oggInfo.channels];
            while (!this.endOfBitStream) {
                int result;
                while (!this.endOfBitStream && (result = this.syncState.pageout(this.page)) != 0) {
                    if (result == -1) {
                        Gdx.app.log("gdx-audio", "Error reading OGG: Corrupt or missing data in bitstream.");
                        continue;
                    }
                    this.streamState.pagein(this.page);
                    while ((result = this.streamState.packetout(this.packet)) != 0) {
                        int samples;
                        if (result == -1) continue;
                        if (this.vorbisBlock.synthesis(this.packet) == 0) {
                            this.dspState.synthesis_blockin(this.vorbisBlock);
                        }
                        while ((samples = this.dspState.synthesis_pcmout(_pcm, _index)) > 0) {
                            float[][] pcm = _pcm[0];
                            int bout = samples < this.convsize ? samples : this.convsize;
                            for (int i = 0; i < this.oggInfo.channels; ++i) {
                                int ptr = i * 2;
                                int mono = _index[i];
                                for (int j = 0; j < bout; ++j) {
                                    int val = (int)((double)pcm[i][mono + j] * 32767.0);
                                    if (val > Short.MAX_VALUE) {
                                        val = Short.MAX_VALUE;
                                    }
                                    if (val < Short.MIN_VALUE) {
                                        val = Short.MIN_VALUE;
                                    }
                                    if (val < 0) {
                                        val |= 0x8000;
                                    }
                                    if (this.bigEndian) {
                                        this.convbuffer[ptr] = (byte)(val >>> 8);
                                        this.convbuffer[ptr + 1] = (byte)val;
                                    } else {
                                        this.convbuffer[ptr] = (byte)val;
                                        this.convbuffer[ptr + 1] = (byte)(val >>> 8);
                                    }
                                    ptr += 2 * this.oggInfo.channels;
                                }
                            }
                            int bytesToWrite = 2 * this.oggInfo.channels * bout;
                            if (bytesToWrite > this.pcmBuffer.remaining()) {
                                throw new GdxRuntimeException("Ogg block too big to be buffered: " + bytesToWrite + " :: " + this.pcmBuffer.remaining());
                            }
                            this.pcmBuffer.put(this.convbuffer, 0, bytesToWrite);
                            wrote = true;
                            this.dspState.synthesis_read(bout);
                        }
                    }
                    if (this.page.eos() != 0) {
                        this.endOfBitStream = true;
                    }
                    if (this.endOfBitStream || !wrote) continue;
                    return;
                }
                if (this.endOfBitStream) continue;
                this.bytes = 0;
                int index = this.syncState.buffer(512);
                if (index >= 0) {
                    this.buffer = this.syncState.data;
                    try {
                        this.bytes = this.input.read(this.buffer, index, 512);
                    }
                    catch (Exception e) {
                        throw new GdxRuntimeException("Error during Vorbis decoding.", e);
                    }
                } else {
                    this.bytes = 0;
                }
                this.syncState.wrote(this.bytes);
                if (this.bytes != 0) continue;
                this.endOfBitStream = true;
            }
            this.streamState.clear();
            this.vorbisBlock.clear();
            this.dspState.clear();
            this.oggInfo.clear();
        }
        this.syncState.clear();
        this.endOfStream = true;
    }

    @Override
    public int read() {
        if (this.readIndex >= this.pcmBuffer.position()) {
            this.pcmBuffer.clear();
            this.readPCM();
            this.readIndex = 0;
        }
        if (this.readIndex >= this.pcmBuffer.position()) {
            return -1;
        }
        int value = this.pcmBuffer.get(this.readIndex);
        if (value < 0) {
            value = 256 + value;
        }
        ++this.readIndex;
        return value;
    }

    public boolean atEnd() {
        return this.endOfStream && this.readIndex >= this.pcmBuffer.position();
    }

    @Override
    public int read(byte[] b, int off, int len) {
        for (int i = 0; i < len; ++i) {
            int value = this.read();
            if (value < 0) {
                if (i == 0) {
                    return -1;
                }
                return i;
            }
            b[i] = (byte)value;
        }
        return len;
    }

    @Override
    public int read(byte[] b) {
        return this.read(b, 0, b.length);
    }

    @Override
    public void close() {
        StreamUtils.closeQuietly(this.input);
    }
}


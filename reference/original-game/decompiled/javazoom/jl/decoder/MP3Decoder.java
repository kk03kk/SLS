/*
 * Decompiled with CFR 0.152.
 */
package javazoom.jl.decoder;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.FrameDecoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.LayerIDecoder;
import javazoom.jl.decoder.LayerIIDecoder;
import javazoom.jl.decoder.LayerIIIDecoder;
import javazoom.jl.decoder.OutputBuffer;
import javazoom.jl.decoder.SynthesisFilter;

public class MP3Decoder {
    private OutputBuffer output;
    private SynthesisFilter filter1;
    private SynthesisFilter filter2;
    private LayerIIIDecoder l3decoder;
    private LayerIIDecoder l2decoder;
    private LayerIDecoder l1decoder;
    private int outputFrequency;
    private int outputChannels;
    private boolean initialized;
    public static final int DECODER_ERROR = 512;
    public static final int UNKNOWN_ERROR = 512;
    public static final int UNSUPPORTED_LAYER = 513;
    public static final int ILLEGAL_SUBBAND_ALLOCATION = 514;

    public OutputBuffer decodeFrame(Header header, Bitstream stream) throws DecoderException {
        if (!this.initialized) {
            this.initialize(header);
        }
        int layer = header.layer();
        FrameDecoder decoder = this.retrieveDecoder(header, stream, layer);
        decoder.decodeFrame();
        return this.output;
    }

    public void setOutputBuffer(OutputBuffer out) {
        this.output = out;
    }

    public int getOutputFrequency() {
        return this.outputFrequency;
    }

    public int getOutputChannels() {
        return this.outputChannels;
    }

    protected DecoderException newDecoderException(int errorcode) {
        return new DecoderException(errorcode, null);
    }

    protected DecoderException newDecoderException(int errorcode, Throwable throwable) {
        return new DecoderException(errorcode, throwable);
    }

    protected FrameDecoder retrieveDecoder(Header header, Bitstream stream, int layer) throws DecoderException {
        FrameDecoder decoder = null;
        switch (layer) {
            case 3: {
                if (this.l3decoder == null) {
                    this.l3decoder = new LayerIIIDecoder(stream, header, this.filter1, this.filter2, this.output, 0);
                }
                decoder = this.l3decoder;
                break;
            }
            case 2: {
                if (this.l2decoder == null) {
                    this.l2decoder = new LayerIIDecoder();
                    this.l2decoder.create(stream, header, this.filter1, this.filter2, this.output, 0);
                }
                decoder = this.l2decoder;
                break;
            }
            case 1: {
                if (this.l1decoder == null) {
                    this.l1decoder = new LayerIDecoder();
                    this.l1decoder.create(stream, header, this.filter1, this.filter2, this.output, 0);
                }
                decoder = this.l1decoder;
            }
        }
        if (decoder == null) {
            throw this.newDecoderException(513, null);
        }
        return decoder;
    }

    private void initialize(Header header) throws DecoderException {
        int channels;
        float scalefactor = 32700.0f;
        int mode = header.mode();
        header.layer();
        int n = channels = mode == 3 ? 1 : 2;
        if (this.output == null) {
            throw new RuntimeException("Output buffer was not set.");
        }
        this.filter1 = new SynthesisFilter(0, scalefactor, null);
        if (channels == 2) {
            this.filter2 = new SynthesisFilter(1, scalefactor, null);
        }
        this.outputChannels = channels;
        this.outputFrequency = header.frequency();
        this.initialized = true;
    }
}


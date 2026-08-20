/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils.compression.rangecoder;

import java.io.IOException;
import java.io.OutputStream;

public class Encoder {
    static final int kTopMask = -16777216;
    static final int kNumBitModelTotalBits = 11;
    static final int kBitModelTotal = 2048;
    static final int kNumMoveBits = 5;
    OutputStream Stream;
    long Low;
    int Range;
    int _cacheSize;
    int _cache;
    long _position;
    static final int kNumMoveReducingBits = 2;
    public static final int kNumBitPriceShiftBits = 6;
    private static int[] ProbPrices = new int[512];

    public void SetStream(OutputStream stream) {
        this.Stream = stream;
    }

    public void ReleaseStream() {
        this.Stream = null;
    }

    public void Init() {
        this._position = 0L;
        this.Low = 0L;
        this.Range = -1;
        this._cacheSize = 1;
        this._cache = 0;
    }

    public void FlushData() throws IOException {
        for (int i = 0; i < 5; ++i) {
            this.ShiftLow();
        }
    }

    public void FlushStream() throws IOException {
        this.Stream.flush();
    }

    public void ShiftLow() throws IOException {
        int LowHi = (int)(this.Low >>> 32);
        if (LowHi != 0 || this.Low < 0xFF000000L) {
            this._position += (long)this._cacheSize;
            int temp = this._cache;
            do {
                this.Stream.write(temp + LowHi);
                temp = 255;
            } while (--this._cacheSize != 0);
            this._cache = (int)this.Low >>> 24;
        }
        ++this._cacheSize;
        this.Low = (this.Low & 0xFFFFFFL) << 8;
    }

    public void EncodeDirectBits(int v, int numTotalBits) throws IOException {
        for (int i = numTotalBits - 1; i >= 0; --i) {
            this.Range >>>= 1;
            if ((v >>> i & 1) == 1) {
                this.Low += (long)this.Range;
            }
            if ((this.Range & 0xFF000000) != 0) continue;
            this.Range <<= 8;
            this.ShiftLow();
        }
    }

    public long GetProcessedSizeAdd() {
        return (long)this._cacheSize + this._position + 4L;
    }

    public static void InitBitModels(short[] probs) {
        for (int i = 0; i < probs.length; ++i) {
            probs[i] = 1024;
        }
    }

    public void Encode(short[] probs, int index, int symbol) throws IOException {
        short prob = probs[index];
        int newBound = (this.Range >>> 11) * prob;
        if (symbol == 0) {
            this.Range = newBound;
            probs[index] = (short)(prob + (2048 - prob >>> 5));
        } else {
            this.Low += (long)newBound & 0xFFFFFFFFL;
            this.Range -= newBound;
            probs[index] = (short)(prob - (prob >>> 5));
        }
        if ((this.Range & 0xFF000000) == 0) {
            this.Range <<= 8;
            this.ShiftLow();
        }
    }

    public static int GetPrice(int Prob, int symbol) {
        return ProbPrices[((Prob - symbol ^ -symbol) & 0x7FF) >>> 2];
    }

    public static int GetPrice0(int Prob) {
        return ProbPrices[Prob >>> 2];
    }

    public static int GetPrice1(int Prob) {
        return ProbPrices[2048 - Prob >>> 2];
    }

    static {
        int kNumBits = 9;
        for (int i = kNumBits - 1; i >= 0; --i) {
            int start = 1 << kNumBits - i - 1;
            int end = 1 << kNumBits - i;
            for (int j = start; j < end; ++j) {
                Encoder.ProbPrices[j] = (i << 6) + (end - j << 6 >>> kNumBits - i - 1);
            }
        }
    }
}


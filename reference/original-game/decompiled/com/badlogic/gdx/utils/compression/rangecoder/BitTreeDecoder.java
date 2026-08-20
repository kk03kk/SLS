/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils.compression.rangecoder;

import com.badlogic.gdx.utils.compression.rangecoder.Decoder;
import java.io.IOException;

public class BitTreeDecoder {
    short[] Models;
    int NumBitLevels;

    public BitTreeDecoder(int numBitLevels) {
        this.NumBitLevels = numBitLevels;
        this.Models = new short[1 << numBitLevels];
    }

    public void Init() {
        Decoder.InitBitModels(this.Models);
    }

    public int Decode(Decoder rangeDecoder) throws IOException {
        int m = 1;
        for (int bitIndex = this.NumBitLevels; bitIndex != 0; --bitIndex) {
            m = (m << 1) + rangeDecoder.DecodeBit(this.Models, m);
        }
        return m - (1 << this.NumBitLevels);
    }

    public int ReverseDecode(Decoder rangeDecoder) throws IOException {
        int m = 1;
        int symbol = 0;
        for (int bitIndex = 0; bitIndex < this.NumBitLevels; ++bitIndex) {
            int bit = rangeDecoder.DecodeBit(this.Models, m);
            m <<= 1;
            m += bit;
            symbol |= bit << bitIndex;
        }
        return symbol;
    }

    public static int ReverseDecode(short[] Models, int startIndex, Decoder rangeDecoder, int NumBitLevels) throws IOException {
        int m = 1;
        int symbol = 0;
        for (int bitIndex = 0; bitIndex < NumBitLevels; ++bitIndex) {
            int bit = rangeDecoder.DecodeBit(Models, startIndex + m);
            m <<= 1;
            m += bit;
            symbol |= bit << bitIndex;
        }
        return symbol;
    }
}


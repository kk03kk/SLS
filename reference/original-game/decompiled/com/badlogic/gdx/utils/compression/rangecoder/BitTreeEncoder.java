/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils.compression.rangecoder;

import com.badlogic.gdx.utils.compression.rangecoder.Decoder;
import com.badlogic.gdx.utils.compression.rangecoder.Encoder;
import java.io.IOException;

public class BitTreeEncoder {
    short[] Models;
    int NumBitLevels;

    public BitTreeEncoder(int numBitLevels) {
        this.NumBitLevels = numBitLevels;
        this.Models = new short[1 << numBitLevels];
    }

    public void Init() {
        Decoder.InitBitModels(this.Models);
    }

    public void Encode(Encoder rangeEncoder, int symbol) throws IOException {
        int m = 1;
        int bitIndex = this.NumBitLevels;
        while (bitIndex != 0) {
            int bit = symbol >>> --bitIndex & 1;
            rangeEncoder.Encode(this.Models, m, bit);
            m = m << 1 | bit;
        }
    }

    public void ReverseEncode(Encoder rangeEncoder, int symbol) throws IOException {
        int m = 1;
        for (int i = 0; i < this.NumBitLevels; ++i) {
            int bit = symbol & 1;
            rangeEncoder.Encode(this.Models, m, bit);
            m = m << 1 | bit;
            symbol >>= 1;
        }
    }

    public int GetPrice(int symbol) {
        int price = 0;
        int m = 1;
        int bitIndex = this.NumBitLevels;
        while (bitIndex != 0) {
            int bit = symbol >>> --bitIndex & 1;
            price += Encoder.GetPrice(this.Models[m], bit);
            m = (m << 1) + bit;
        }
        return price;
    }

    public int ReverseGetPrice(int symbol) {
        int price = 0;
        int m = 1;
        for (int i = this.NumBitLevels; i != 0; --i) {
            int bit = symbol & 1;
            symbol >>>= 1;
            price += Encoder.GetPrice(this.Models[m], bit);
            m = m << 1 | bit;
        }
        return price;
    }

    public static int ReverseGetPrice(short[] Models, int startIndex, int NumBitLevels, int symbol) {
        int price = 0;
        int m = 1;
        for (int i = NumBitLevels; i != 0; --i) {
            int bit = symbol & 1;
            symbol >>>= 1;
            price += Encoder.GetPrice(Models[startIndex + m], bit);
            m = m << 1 | bit;
        }
        return price;
    }

    public static void ReverseEncode(short[] Models, int startIndex, Encoder rangeEncoder, int NumBitLevels, int symbol) throws IOException {
        int m = 1;
        for (int i = 0; i < NumBitLevels; ++i) {
            int bit = symbol & 1;
            rangeEncoder.Encode(Models, startIndex + m, bit);
            m = m << 1 | bit;
            symbol >>= 1;
        }
    }
}


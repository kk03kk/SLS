/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.utils;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class DataInput
extends DataInputStream {
    private char[] chars = new char[32];

    public DataInput(InputStream in) {
        super(in);
    }

    public int readInt(boolean optimizePositive) throws IOException {
        int b = this.read();
        int result = b & 0x7F;
        if ((b & 0x80) != 0) {
            b = this.read();
            result |= (b & 0x7F) << 7;
            if ((b & 0x80) != 0) {
                b = this.read();
                result |= (b & 0x7F) << 14;
                if ((b & 0x80) != 0) {
                    b = this.read();
                    result |= (b & 0x7F) << 21;
                    if ((b & 0x80) != 0) {
                        b = this.read();
                        result |= (b & 0x7F) << 28;
                    }
                }
            }
        }
        return optimizePositive ? result : result >>> 1 ^ -(result & 1);
    }

    public String readString() throws IOException {
        int charCount = this.readInt(true);
        switch (charCount) {
            case 0: {
                return null;
            }
            case 1: {
                return "";
            }
        }
        if (this.chars.length < --charCount) {
            this.chars = new char[charCount];
        }
        char[] chars = this.chars;
        int charIndex = 0;
        int b = 0;
        while (charIndex < charCount && (b = this.read()) <= 127) {
            chars[charIndex++] = (char)b;
        }
        if (charIndex < charCount) {
            this.readUtf8_slow(charCount, charIndex, b);
        }
        return new String(chars, 0, charCount);
    }

    private void readUtf8_slow(int charCount, int charIndex, int b) throws IOException {
        char[] chars = this.chars;
        while (true) {
            switch (b >> 4) {
                case 0: 
                case 1: 
                case 2: 
                case 3: 
                case 4: 
                case 5: 
                case 6: 
                case 7: {
                    chars[charIndex] = (char)b;
                    break;
                }
                case 12: 
                case 13: {
                    chars[charIndex] = (char)((b & 0x1F) << 6 | this.read() & 0x3F);
                    break;
                }
                case 14: {
                    chars[charIndex] = (char)((b & 0xF) << 12 | (this.read() & 0x3F) << 6 | this.read() & 0x3F);
                }
            }
            if (++charIndex >= charCount) break;
            b = this.read() & 0xFF;
        }
    }
}


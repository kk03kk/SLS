/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.net.io.FromNetASCIIInputStream;

public final class FromNetASCIIOutputStream
extends FilterOutputStream {
    private boolean __lastWasCR = false;

    public FromNetASCIIOutputStream(OutputStream output) {
        super(output);
    }

    private void __write(int ch) throws IOException {
        switch (ch) {
            case 13: {
                this.__lastWasCR = true;
                break;
            }
            case 10: {
                if (this.__lastWasCR) {
                    this.out.write(FromNetASCIIInputStream._lineSeparatorBytes);
                    this.__lastWasCR = false;
                    break;
                }
                this.__lastWasCR = false;
                this.out.write(10);
                break;
            }
            default: {
                if (this.__lastWasCR) {
                    this.out.write(13);
                    this.__lastWasCR = false;
                }
                this.out.write(ch);
            }
        }
    }

    @Override
    public synchronized void write(int ch) throws IOException {
        if (FromNetASCIIInputStream._noConversionRequired) {
            this.out.write(ch);
            return;
        }
        this.__write(ch);
    }

    @Override
    public synchronized void write(byte[] buffer) throws IOException {
        this.write(buffer, 0, buffer.length);
    }

    @Override
    public synchronized void write(byte[] buffer, int offset, int length) throws IOException {
        if (FromNetASCIIInputStream._noConversionRequired) {
            this.out.write(buffer, offset, length);
            return;
        }
        while (length-- > 0) {
            this.__write(buffer[offset++]);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (FromNetASCIIInputStream._noConversionRequired) {
            super.close();
            return;
        }
        if (this.__lastWasCR) {
            this.out.write(13);
        }
        super.close();
    }
}


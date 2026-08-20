/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class ToNetASCIIOutputStream
extends FilterOutputStream {
    private boolean __lastWasCR = false;

    public ToNetASCIIOutputStream(OutputStream output) {
        super(output);
    }

    @Override
    public synchronized void write(int ch) throws IOException {
        switch (ch) {
            case 13: {
                this.__lastWasCR = true;
                this.out.write(13);
                return;
            }
            case 10: {
                if (this.__lastWasCR) break;
                this.out.write(13);
            }
        }
        this.__lastWasCR = false;
        this.out.write(ch);
    }

    @Override
    public synchronized void write(byte[] buffer) throws IOException {
        this.write(buffer, 0, buffer.length);
    }

    @Override
    public synchronized void write(byte[] buffer, int offset, int length) throws IOException {
        while (length-- > 0) {
            this.write(buffer[offset++]);
        }
    }
}


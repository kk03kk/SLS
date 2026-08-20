/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.io;

import java.io.IOException;
import java.io.Writer;

public final class DotTerminatedMessageWriter
extends Writer {
    private static final int __NOTHING_SPECIAL_STATE = 0;
    private static final int __LAST_WAS_CR_STATE = 1;
    private static final int __LAST_WAS_NL_STATE = 2;
    private int __state;
    private Writer __output;

    public DotTerminatedMessageWriter(Writer output) {
        super((Object)output);
        this.__output = output;
        this.__state = 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void write(int ch) throws IOException {
        Object object = this.lock;
        synchronized (object) {
            switch (ch) {
                case 13: {
                    this.__state = 1;
                    this.__output.write(13);
                    return;
                }
                case 10: {
                    if (this.__state != 1) {
                        this.__output.write(13);
                    }
                    this.__output.write(10);
                    this.__state = 2;
                    return;
                }
                case 46: {
                    if (this.__state != 2) break;
                    this.__output.write(46);
                }
            }
            this.__state = 0;
            this.__output.write(ch);
            return;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void write(char[] buffer, int offset, int length) throws IOException {
        Object object = this.lock;
        synchronized (object) {
            while (length-- > 0) {
                this.write(buffer[offset++]);
            }
        }
    }

    @Override
    public void write(char[] buffer) throws IOException {
        this.write(buffer, 0, buffer.length);
    }

    @Override
    public void write(String string) throws IOException {
        this.write(string.toCharArray());
    }

    @Override
    public void write(String string, int offset, int length) throws IOException {
        this.write(string.toCharArray(), offset, length);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void flush() throws IOException {
        Object object = this.lock;
        synchronized (object) {
            this.__output.flush();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void close() throws IOException {
        Object object = this.lock;
        synchronized (object) {
            if (this.__output == null) {
                return;
            }
            if (this.__state == 1) {
                this.__output.write(10);
            } else if (this.__state != 2) {
                this.__output.write("\r\n");
            }
            this.__output.write(".\r\n");
            this.__output.flush();
            this.__output = null;
        }
    }
}


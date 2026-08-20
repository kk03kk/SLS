/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public final class DotTerminatedMessageReader
extends BufferedReader {
    private static final char LF = '\n';
    private static final char CR = '\r';
    private static final int DOT = 46;
    private boolean atBeginning = true;
    private boolean eof = false;
    private boolean seenCR;

    public DotTerminatedMessageReader(Reader reader) {
        super(reader);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int read() throws IOException {
        Object object = this.lock;
        synchronized (object) {
            if (this.eof) {
                return -1;
            }
            int chint = super.read();
            if (chint == -1) {
                this.eof = true;
                return -1;
            }
            if (this.atBeginning) {
                this.atBeginning = false;
                if (chint == 46) {
                    this.mark(2);
                    chint = super.read();
                    if (chint == -1) {
                        this.eof = true;
                        return 46;
                    }
                    if (chint == 46) {
                        return chint;
                    }
                    if (chint == 13) {
                        chint = super.read();
                        if (chint == -1) {
                            this.reset();
                            return 46;
                        }
                        if (chint == 10) {
                            this.atBeginning = true;
                            this.eof = true;
                            return -1;
                        }
                    }
                    this.reset();
                    return 46;
                }
            }
            if (this.seenCR) {
                this.seenCR = false;
                if (chint == 10) {
                    this.atBeginning = true;
                }
            }
            if (chint == 13) {
                this.seenCR = true;
            }
            return chint;
        }
    }

    @Override
    public int read(char[] buffer) throws IOException {
        return this.read(buffer, 0, buffer.length);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int read(char[] buffer, int offset, int length) throws IOException {
        if (length < 1) {
            return 0;
        }
        Object object = this.lock;
        synchronized (object) {
            int ch = this.read();
            if (ch == -1) {
                return -1;
            }
            int off = offset;
            do {
                buffer[offset++] = (char)ch;
            } while (--length > 0 && (ch = this.read()) != -1);
            return offset - off;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void close() throws IOException {
        Object object = this.lock;
        synchronized (object) {
            if (!this.eof) {
                while (this.read() != -1) {
                }
            }
            this.eof = true;
            this.atBeginning = false;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        Object object = this.lock;
        synchronized (object) {
            int intch;
            while ((intch = this.read()) != -1) {
                if (intch == 10 && this.atBeginning) {
                    return sb.substring(0, sb.length() - 1);
                }
                sb.append((char)intch);
            }
        }
        String string = sb.toString();
        if (string.length() == 0) {
            return null;
        }
        return string;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public final class CRLFLineReader
extends BufferedReader {
    private static final char LF = '\n';
    private static final char CR = '\r';

    public CRLFLineReader(Reader reader) {
        super(reader);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean prevWasCR = false;
        Object object = this.lock;
        synchronized (object) {
            int intch;
            while ((intch = this.read()) != -1) {
                if (prevWasCR && intch == 10) {
                    return sb.substring(0, sb.length() - 1);
                }
                prevWasCR = intch == 13;
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


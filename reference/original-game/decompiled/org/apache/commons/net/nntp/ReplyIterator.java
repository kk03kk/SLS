/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.nntp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.commons.net.io.DotTerminatedMessageReader;
import org.apache.commons.net.io.Util;

class ReplyIterator
implements Iterator<String>,
Iterable<String> {
    private final BufferedReader reader;
    private String line;
    private Exception savedException;

    ReplyIterator(BufferedReader _reader, boolean addDotReader) throws IOException {
        this.reader = addDotReader ? new DotTerminatedMessageReader(_reader) : _reader;
        this.line = this.reader.readLine();
        if (this.line == null) {
            Util.closeQuietly(this.reader);
        }
    }

    ReplyIterator(BufferedReader _reader) throws IOException {
        this(_reader, true);
    }

    @Override
    public boolean hasNext() {
        if (this.savedException != null) {
            throw new NoSuchElementException(this.savedException.toString());
        }
        return this.line != null;
    }

    @Override
    public String next() throws NoSuchElementException {
        if (this.savedException != null) {
            throw new NoSuchElementException(this.savedException.toString());
        }
        String prev = this.line;
        if (prev == null) {
            throw new NoSuchElementException();
        }
        try {
            this.line = this.reader.readLine();
            if (this.line == null) {
                Util.closeQuietly(this.reader);
            }
        }
        catch (IOException ex) {
            this.savedException = ex;
            Util.closeQuietly(this.reader);
        }
        return prev;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<String> iterator() {
        return this;
    }
}


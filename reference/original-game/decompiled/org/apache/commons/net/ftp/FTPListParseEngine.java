/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileEntryParser;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.apache.commons.net.ftp.FTPFileFilters;
import org.apache.commons.net.util.Charsets;

public class FTPListParseEngine {
    private List<String> entries = new LinkedList<String>();
    private ListIterator<String> _internalIterator = this.entries.listIterator();
    private final FTPFileEntryParser parser;
    private final boolean saveUnparseableEntries;

    public FTPListParseEngine(FTPFileEntryParser parser) {
        this(parser, null);
    }

    FTPListParseEngine(FTPFileEntryParser parser, FTPClientConfig configuration) {
        this.parser = parser;
        this.saveUnparseableEntries = configuration != null ? configuration.getUnparseableEntries() : false;
    }

    public void readServerList(InputStream stream, String encoding) throws IOException {
        this.entries = new LinkedList<String>();
        this.readStream(stream, encoding);
        this.parser.preParse(this.entries);
        this.resetIterator();
    }

    private void readStream(InputStream stream, String encoding) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Charsets.toCharset(encoding)));
        String line = this.parser.readNextEntry(reader);
        while (line != null) {
            this.entries.add(line);
            line = this.parser.readNextEntry(reader);
        }
        reader.close();
    }

    public FTPFile[] getNext(int quantityRequested) {
        LinkedList<FTPFile> tmpResults = new LinkedList<FTPFile>();
        for (int count = quantityRequested; count > 0 && this._internalIterator.hasNext(); --count) {
            String entry = this._internalIterator.next();
            FTPFile temp = this.parser.parseFTPEntry(entry);
            if (temp == null && this.saveUnparseableEntries) {
                temp = new FTPFile(entry);
            }
            tmpResults.add(temp);
        }
        return tmpResults.toArray(new FTPFile[tmpResults.size()]);
    }

    public FTPFile[] getPrevious(int quantityRequested) {
        LinkedList<FTPFile> tmpResults = new LinkedList<FTPFile>();
        for (int count = quantityRequested; count > 0 && this._internalIterator.hasPrevious(); --count) {
            String entry = this._internalIterator.previous();
            FTPFile temp = this.parser.parseFTPEntry(entry);
            if (temp == null && this.saveUnparseableEntries) {
                temp = new FTPFile(entry);
            }
            tmpResults.add(0, temp);
        }
        return tmpResults.toArray(new FTPFile[tmpResults.size()]);
    }

    public FTPFile[] getFiles() throws IOException {
        return this.getFiles(FTPFileFilters.NON_NULL);
    }

    public FTPFile[] getFiles(FTPFileFilter filter) throws IOException {
        ArrayList<FTPFile> tmpResults = new ArrayList<FTPFile>();
        for (String entry : this.entries) {
            FTPFile temp = this.parser.parseFTPEntry(entry);
            if (temp == null && this.saveUnparseableEntries) {
                temp = new FTPFile(entry);
            }
            if (!filter.accept(temp)) continue;
            tmpResults.add(temp);
        }
        return tmpResults.toArray(new FTPFile[tmpResults.size()]);
    }

    public boolean hasNext() {
        return this._internalIterator.hasNext();
    }

    public boolean hasPrevious() {
        return this._internalIterator.hasPrevious();
    }

    public void resetIterator() {
        this._internalIterator = this.entries.listIterator();
    }

    @Deprecated
    public void readServerList(InputStream stream) throws IOException {
        this.readServerList(stream, null);
    }
}


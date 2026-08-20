/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import org.apache.commons.net.ftp.FTPFileEntryParser;

public abstract class FTPFileEntryParserImpl
implements FTPFileEntryParser {
    @Override
    public String readNextEntry(BufferedReader reader) throws IOException {
        return reader.readLine();
    }

    @Override
    public List<String> preParse(List<String> original) {
        return original;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp.parser;

import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFileEntryParser;
import org.apache.commons.net.ftp.parser.ParserInitializationException;

public interface FTPFileEntryParserFactory {
    public FTPFileEntryParser createFileEntryParser(String var1) throws ParserInitializationException;

    public FTPFileEntryParser createFileEntryParser(FTPClientConfig var1) throws ParserInitializationException;
}


/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp.parser;

import java.io.File;
import java.text.ParseException;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.parser.ConfigurableFTPFileEntryParserImpl;

public class OS400FTPEntryParser
extends ConfigurableFTPFileEntryParserImpl {
    private static final String DEFAULT_DATE_FORMAT = "yy/MM/dd HH:mm:ss";
    private static final String REGEX = "(\\S+)\\s+(?:(\\d+)\\s+)?(?:(\\S+)\\s+(\\S+)\\s+)?(\\*STMF|\\*DIR|\\*FILE|\\*MEM)\\s+(?:(\\S+)\\s*)?";

    public OS400FTPEntryParser() {
        this((FTPClientConfig)null);
    }

    public OS400FTPEntryParser(FTPClientConfig config) {
        super(REGEX);
        this.configure(config);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    @Override
    public FTPFile parseFTPEntry(String entry) {
        int pos;
        int type;
        FTPFile file = new FTPFile();
        file.setRawListing(entry);
        if (!this.matches(entry)) return null;
        String usr = this.group(1);
        String filesize = this.group(2);
        String datestr = "";
        if (!this.isNullOrEmpty(this.group(3)) || !this.isNullOrEmpty(this.group(4))) {
            datestr = this.group(3) + " " + this.group(4);
        }
        String typeStr = this.group(5);
        String name = this.group(6);
        boolean mustScanForPathSeparator = true;
        try {
            file.setTimestamp(super.parseTimestamp(datestr));
        }
        catch (ParseException e) {
            // empty catch block
        }
        if (typeStr.equalsIgnoreCase("*STMF")) {
            type = 0;
            if (this.isNullOrEmpty(filesize)) return null;
            if (this.isNullOrEmpty(name)) {
                return null;
            }
        } else if (typeStr.equalsIgnoreCase("*DIR")) {
            type = 1;
            if (this.isNullOrEmpty(filesize)) return null;
            if (this.isNullOrEmpty(name)) {
                return null;
            }
        } else if (typeStr.equalsIgnoreCase("*FILE")) {
            if (name == null || !name.toUpperCase().endsWith(".SAVF")) return null;
            mustScanForPathSeparator = false;
            type = 0;
        } else if (typeStr.equalsIgnoreCase("*MEM")) {
            mustScanForPathSeparator = false;
            type = 0;
            if (this.isNullOrEmpty(name)) {
                return null;
            }
            if (!this.isNullOrEmpty(filesize) || !this.isNullOrEmpty(datestr)) {
                return null;
            }
            name = name.replace('/', File.separatorChar);
        } else {
            type = 3;
        }
        file.setType(type);
        file.setUser(usr);
        try {
            file.setSize(Long.parseLong(filesize));
        }
        catch (NumberFormatException e) {
            // empty catch block
        }
        if (name.endsWith("/")) {
            name = name.substring(0, name.length() - 1);
        }
        if (mustScanForPathSeparator && (pos = name.lastIndexOf(47)) > -1) {
            name = name.substring(pos + 1);
        }
        file.setName(name);
        return file;
    }

    private boolean isNullOrEmpty(String string) {
        return string == null || string.length() == 0;
    }

    @Override
    protected FTPClientConfig getDefaultConfiguration() {
        return new FTPClientConfig("OS/400", DEFAULT_DATE_FORMAT, null, null, null, null);
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp.parser;

import java.text.ParseException;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.parser.ConfigurableFTPFileEntryParserImpl;

public class OS2FTPEntryParser
extends ConfigurableFTPFileEntryParserImpl {
    private static final String DEFAULT_DATE_FORMAT = "MM-dd-yy HH:mm";
    private static final String REGEX = "\\s*([0-9]+)\\s*(\\s+|[A-Z]+)\\s*(DIR|\\s+)\\s*(\\S+)\\s+(\\S+)\\s+(\\S.*)";

    public OS2FTPEntryParser() {
        this((FTPClientConfig)null);
    }

    public OS2FTPEntryParser(FTPClientConfig config) {
        super(REGEX);
        this.configure(config);
    }

    @Override
    public FTPFile parseFTPEntry(String entry) {
        FTPFile f = new FTPFile();
        if (this.matches(entry)) {
            String size = this.group(1);
            String attrib = this.group(2);
            String dirString = this.group(3);
            String datestr = this.group(4) + " " + this.group(5);
            String name = this.group(6);
            try {
                f.setTimestamp(super.parseTimestamp(datestr));
            }
            catch (ParseException e) {
                // empty catch block
            }
            if (dirString.trim().equals("DIR") || attrib.trim().equals("DIR")) {
                f.setType(1);
            } else {
                f.setType(0);
            }
            f.setName(name.trim());
            f.setSize(Long.parseLong(size.trim()));
            return f;
        }
        return null;
    }

    @Override
    protected FTPClientConfig getDefaultConfiguration() {
        return new FTPClientConfig("OS/2", DEFAULT_DATE_FORMAT, null, null, null, null);
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp.parser;

import java.text.ParseException;
import java.util.List;
import java.util.ListIterator;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.parser.ConfigurableFTPFileEntryParserImpl;

public class UnixFTPEntryParser
extends ConfigurableFTPFileEntryParserImpl {
    static final String DEFAULT_DATE_FORMAT = "MMM d yyyy";
    static final String DEFAULT_RECENT_DATE_FORMAT = "MMM d HH:mm";
    static final String NUMERIC_DATE_FORMAT = "yyyy-MM-dd HH:mm";
    public static final FTPClientConfig NUMERIC_DATE_CONFIG = new FTPClientConfig("UNIX", "yyyy-MM-dd HH:mm", null, null, null, null);
    private static final String REGEX = "([bcdelfmpSs-])(((r|-)(w|-)([xsStTL-]))((r|-)(w|-)([xsStTL-]))((r|-)(w|-)([xsStTL-])))\\+?\\s*(\\d+)\\s+(?:(\\S+(?:\\s\\S+)*?)\\s+)?(?:(\\S+(?:\\s\\S+)*)\\s+)?(\\d+(?:,\\s*\\d+)?)\\s+((?:\\d+[-/]\\d+[-/]\\d+)|(?:\\S{3}\\s+\\d{1,2})|(?:\\d{1,2}\\s+\\S{3}))\\s+(\\d+(?::\\d+)?)\\s(.*)";
    private final boolean trimLeadingSpaces;

    public UnixFTPEntryParser() {
        this((FTPClientConfig)null);
    }

    public UnixFTPEntryParser(FTPClientConfig config) {
        this(config, false);
    }

    public UnixFTPEntryParser(FTPClientConfig config, boolean trimLeadingSpaces) {
        super(REGEX);
        this.configure(config);
        this.trimLeadingSpaces = trimLeadingSpaces;
    }

    @Override
    public List<String> preParse(List<String> original) {
        ListIterator<String> iter = original.listIterator();
        while (iter.hasNext()) {
            String entry = iter.next();
            if (!entry.matches("^total \\d+$")) continue;
            iter.remove();
        }
        return original;
    }

    @Override
    public FTPFile parseFTPEntry(String entry) {
        FTPFile file = new FTPFile();
        file.setRawListing(entry);
        boolean isDevice = false;
        if (this.matches(entry)) {
            int type;
            String typeStr = this.group(1);
            String hardLinkCount = this.group(15);
            String usr = this.group(16);
            String grp = this.group(17);
            String filesize = this.group(18);
            String datestr = this.group(19) + " " + this.group(20);
            String name = this.group(21);
            if (this.trimLeadingSpaces) {
                name = name.replaceFirst("^\\s+", "");
            }
            try {
                file.setTimestamp(super.parseTimestamp(datestr));
            }
            catch (ParseException e) {
                // empty catch block
            }
            switch (typeStr.charAt(0)) {
                case 'd': {
                    type = 1;
                    break;
                }
                case 'e': {
                    type = 2;
                    break;
                }
                case 'l': {
                    type = 2;
                    break;
                }
                case 'b': 
                case 'c': {
                    isDevice = true;
                    type = 0;
                    break;
                }
                case '-': 
                case 'f': {
                    type = 0;
                    break;
                }
                default: {
                    type = 3;
                }
            }
            file.setType(type);
            int g = 4;
            int access = 0;
            while (access < 3) {
                file.setPermission(access, 0, !this.group(g).equals("-"));
                file.setPermission(access, 1, !this.group(g + 1).equals("-"));
                String execPerm = this.group(g + 2);
                if (!execPerm.equals("-") && !Character.isUpperCase(execPerm.charAt(0))) {
                    file.setPermission(access, 2, true);
                } else {
                    file.setPermission(access, 2, false);
                }
                ++access;
                g += 4;
            }
            if (!isDevice) {
                try {
                    file.setHardLinkCount(Integer.parseInt(hardLinkCount));
                }
                catch (NumberFormatException e) {
                    // empty catch block
                }
            }
            file.setUser(usr);
            file.setGroup(grp);
            try {
                file.setSize(Long.parseLong(filesize));
            }
            catch (NumberFormatException e) {
                // empty catch block
            }
            if (type == 2) {
                int end = name.indexOf(" -> ");
                if (end == -1) {
                    file.setName(name);
                } else {
                    file.setName(name.substring(0, end));
                    file.setLink(name.substring(end + 4));
                }
            } else {
                file.setName(name);
            }
            return file;
        }
        return null;
    }

    @Override
    protected FTPClientConfig getDefaultConfiguration() {
        return new FTPClientConfig("UNIX", DEFAULT_DATE_FORMAT, DEFAULT_RECENT_DATE_FORMAT, null, null, null);
    }
}


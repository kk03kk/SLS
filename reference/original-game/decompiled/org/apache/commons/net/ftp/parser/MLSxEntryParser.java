/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp.parser;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileEntryParserImpl;

public class MLSxEntryParser
extends FTPFileEntryParserImpl {
    private static final MLSxEntryParser PARSER = new MLSxEntryParser();
    private static final HashMap<String, Integer> TYPE_TO_INT = new HashMap();
    private static int[] UNIX_GROUPS;
    private static int[][] UNIX_PERMS;

    @Override
    public FTPFile parseFTPEntry(String entry) {
        if (entry.startsWith(" ")) {
            if (entry.length() > 1) {
                FTPFile file = new FTPFile();
                file.setRawListing(entry);
                file.setName(entry.substring(1));
                return file;
            }
            return null;
        }
        String[] parts = entry.split(" ", 2);
        if (parts.length != 2 || parts[1].length() == 0) {
            return null;
        }
        String factList = parts[0];
        if (!factList.endsWith(";")) {
            return null;
        }
        FTPFile file = new FTPFile();
        file.setRawListing(entry);
        file.setName(parts[1]);
        String[] facts = factList.split(";");
        boolean hasUnixMode = parts[0].toLowerCase(Locale.ENGLISH).contains("unix.mode=");
        for (String fact : facts) {
            String[] factparts = fact.split("=", -1);
            if (factparts.length != 2) {
                return null;
            }
            String factname = factparts[0].toLowerCase(Locale.ENGLISH);
            String factvalue = factparts[1];
            if (factvalue.length() == 0) continue;
            String valueLowerCase = factvalue.toLowerCase(Locale.ENGLISH);
            if ("size".equals(factname)) {
                file.setSize(Long.parseLong(factvalue));
                continue;
            }
            if ("sizd".equals(factname)) {
                file.setSize(Long.parseLong(factvalue));
                continue;
            }
            if ("modify".equals(factname)) {
                Calendar parsed = MLSxEntryParser.parseGMTdateTime(factvalue);
                if (parsed == null) {
                    return null;
                }
                file.setTimestamp(parsed);
                continue;
            }
            if ("type".equals(factname)) {
                Integer intType = TYPE_TO_INT.get(valueLowerCase);
                if (intType == null) {
                    file.setType(3);
                    continue;
                }
                file.setType(intType);
                continue;
            }
            if (factname.startsWith("unix.")) {
                String unixfact = factname.substring("unix.".length()).toLowerCase(Locale.ENGLISH);
                if ("group".equals(unixfact)) {
                    file.setGroup(factvalue);
                    continue;
                }
                if ("owner".equals(unixfact)) {
                    file.setUser(factvalue);
                    continue;
                }
                if (!"mode".equals(unixfact)) continue;
                int off = factvalue.length() - 3;
                for (int i = 0; i < 3; ++i) {
                    int ch = factvalue.charAt(off + i) - 48;
                    if (ch < 0 || ch > 7) continue;
                    for (int p : UNIX_PERMS[ch]) {
                        file.setPermission(UNIX_GROUPS[i], p, true);
                    }
                }
                continue;
            }
            if (hasUnixMode || !"perm".equals(factname)) continue;
            this.doUnixPerms(file, valueLowerCase);
        }
        return file;
    }

    public static Calendar parseGMTdateTime(String timestamp) {
        boolean hasMillis;
        SimpleDateFormat sdf;
        if (timestamp.contains(".")) {
            sdf = new SimpleDateFormat("yyyyMMddHHmmss.SSS");
            hasMillis = true;
        } else {
            sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            hasMillis = false;
        }
        TimeZone GMT = TimeZone.getTimeZone("GMT");
        sdf.setTimeZone(GMT);
        GregorianCalendar gc = new GregorianCalendar(GMT);
        ParsePosition pos = new ParsePosition(0);
        sdf.setLenient(false);
        Date parsed = sdf.parse(timestamp, pos);
        if (pos.getIndex() != timestamp.length()) {
            return null;
        }
        gc.setTime(parsed);
        if (!hasMillis) {
            gc.clear(14);
        }
        return gc;
    }

    private void doUnixPerms(FTPFile file, String valueLowerCase) {
        block12: for (char c : valueLowerCase.toCharArray()) {
            switch (c) {
                case 'a': {
                    file.setPermission(0, 1, true);
                    continue block12;
                }
                case 'c': {
                    file.setPermission(0, 1, true);
                    continue block12;
                }
                case 'd': {
                    file.setPermission(0, 1, true);
                    continue block12;
                }
                case 'e': {
                    file.setPermission(0, 0, true);
                    continue block12;
                }
                case 'f': {
                    continue block12;
                }
                case 'l': {
                    file.setPermission(0, 2, true);
                    continue block12;
                }
                case 'm': {
                    file.setPermission(0, 1, true);
                    continue block12;
                }
                case 'p': {
                    file.setPermission(0, 1, true);
                    continue block12;
                }
                case 'r': {
                    file.setPermission(0, 0, true);
                    continue block12;
                }
                case 'w': {
                    file.setPermission(0, 1, true);
                    continue block12;
                }
            }
        }
    }

    public static FTPFile parseEntry(String entry) {
        return PARSER.parseFTPEntry(entry);
    }

    public static MLSxEntryParser getInstance() {
        return PARSER;
    }

    static {
        TYPE_TO_INT.put("file", 0);
        TYPE_TO_INT.put("cdir", 1);
        TYPE_TO_INT.put("pdir", 1);
        TYPE_TO_INT.put("dir", 1);
        UNIX_GROUPS = new int[]{0, 1, 2};
        UNIX_PERMS = new int[][]{new int[0], {2}, {1}, {2, 1}, {0}, {0, 2}, {0, 1}, {0, 1, 2}};
    }
}


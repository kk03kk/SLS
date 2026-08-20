/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp.parser;

import java.text.ParseException;
import java.util.Calendar;

public interface FTPTimestampParser {
    public static final String DEFAULT_SDF = "MMM d yyyy";
    public static final String DEFAULT_RECENT_SDF = "MMM d HH:mm";

    public Calendar parseTimestamp(String var1) throws ParseException;
}


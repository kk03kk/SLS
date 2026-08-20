/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp.parser;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.apache.commons.net.ftp.Configurable;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.parser.FTPTimestampParser;

public class FTPTimestampParserImpl
implements FTPTimestampParser,
Configurable {
    private SimpleDateFormat defaultDateFormat;
    private int defaultDateSmallestUnitIndex;
    private SimpleDateFormat recentDateFormat;
    private int recentDateSmallestUnitIndex;
    private boolean lenientFutureDates = false;
    private static final int[] CALENDAR_UNITS = new int[]{14, 13, 12, 11, 5, 2, 1};

    private static int getEntry(SimpleDateFormat dateFormat) {
        if (dateFormat == null) {
            return 0;
        }
        String FORMAT_CHARS = "SsmHdM";
        String pattern = dateFormat.toPattern();
        for (char ch : "SsmHdM".toCharArray()) {
            if (pattern.indexOf(ch) == -1) continue;
            switch (ch) {
                case 'S': {
                    return FTPTimestampParserImpl.indexOf(14);
                }
                case 's': {
                    return FTPTimestampParserImpl.indexOf(13);
                }
                case 'm': {
                    return FTPTimestampParserImpl.indexOf(12);
                }
                case 'H': {
                    return FTPTimestampParserImpl.indexOf(11);
                }
                case 'd': {
                    return FTPTimestampParserImpl.indexOf(5);
                }
                case 'M': {
                    return FTPTimestampParserImpl.indexOf(2);
                }
            }
        }
        return 0;
    }

    private static int indexOf(int calendarUnit) {
        for (int i = 0; i < CALENDAR_UNITS.length; ++i) {
            if (calendarUnit != CALENDAR_UNITS[i]) continue;
            return i;
        }
        return 0;
    }

    private static void setPrecision(int index, Calendar working) {
        if (index <= 0) {
            return;
        }
        int field = CALENDAR_UNITS[index - 1];
        int value = working.get(field);
        if (value == 0) {
            working.clear(field);
        }
    }

    public FTPTimestampParserImpl() {
        this.setDefaultDateFormat("MMM d yyyy", null);
        this.setRecentDateFormat("MMM d HH:mm", null);
    }

    @Override
    public Calendar parseTimestamp(String timestampStr) throws ParseException {
        Calendar now = Calendar.getInstance();
        return this.parseTimestamp(timestampStr, now);
    }

    public Calendar parseTimestamp(String timestampStr, Calendar serverTime) throws ParseException {
        ParsePosition pp;
        Calendar working = (Calendar)serverTime.clone();
        working.setTimeZone(this.getServerTimeZone());
        Date parsed = null;
        if (this.recentDateFormat != null) {
            Calendar now = (Calendar)serverTime.clone();
            now.setTimeZone(this.getServerTimeZone());
            if (this.lenientFutureDates) {
                now.add(5, 1);
            }
            String year = Integer.toString(now.get(1));
            String timeStampStrPlusYear = timestampStr + " " + year;
            SimpleDateFormat hackFormatter = new SimpleDateFormat(this.recentDateFormat.toPattern() + " yyyy", this.recentDateFormat.getDateFormatSymbols());
            hackFormatter.setLenient(false);
            hackFormatter.setTimeZone(this.recentDateFormat.getTimeZone());
            ParsePosition pp2 = new ParsePosition(0);
            parsed = hackFormatter.parse(timeStampStrPlusYear, pp2);
            if (parsed != null && pp2.getIndex() == timeStampStrPlusYear.length()) {
                working.setTime(parsed);
                if (working.after(now)) {
                    working.add(1, -1);
                }
                FTPTimestampParserImpl.setPrecision(this.recentDateSmallestUnitIndex, working);
                return working;
            }
        }
        if ((parsed = this.defaultDateFormat.parse(timestampStr, pp = new ParsePosition(0))) == null || pp.getIndex() != timestampStr.length()) {
            throw new ParseException("Timestamp '" + timestampStr + "' could not be parsed using a server time of " + serverTime.getTime().toString(), pp.getErrorIndex());
        }
        working.setTime(parsed);
        FTPTimestampParserImpl.setPrecision(this.defaultDateSmallestUnitIndex, working);
        return working;
    }

    public SimpleDateFormat getDefaultDateFormat() {
        return this.defaultDateFormat;
    }

    public String getDefaultDateFormatString() {
        return this.defaultDateFormat.toPattern();
    }

    private void setDefaultDateFormat(String format, DateFormatSymbols dfs) {
        if (format != null) {
            this.defaultDateFormat = dfs != null ? new SimpleDateFormat(format, dfs) : new SimpleDateFormat(format);
            this.defaultDateFormat.setLenient(false);
        } else {
            this.defaultDateFormat = null;
        }
        this.defaultDateSmallestUnitIndex = FTPTimestampParserImpl.getEntry(this.defaultDateFormat);
    }

    public SimpleDateFormat getRecentDateFormat() {
        return this.recentDateFormat;
    }

    public String getRecentDateFormatString() {
        return this.recentDateFormat.toPattern();
    }

    private void setRecentDateFormat(String format, DateFormatSymbols dfs) {
        if (format != null) {
            this.recentDateFormat = dfs != null ? new SimpleDateFormat(format, dfs) : new SimpleDateFormat(format);
            this.recentDateFormat.setLenient(false);
        } else {
            this.recentDateFormat = null;
        }
        this.recentDateSmallestUnitIndex = FTPTimestampParserImpl.getEntry(this.recentDateFormat);
    }

    public String[] getShortMonths() {
        return this.defaultDateFormat.getDateFormatSymbols().getShortMonths();
    }

    public TimeZone getServerTimeZone() {
        return this.defaultDateFormat.getTimeZone();
    }

    private void setServerTimeZone(String serverTimeZoneId) {
        TimeZone serverTimeZone = TimeZone.getDefault();
        if (serverTimeZoneId != null) {
            serverTimeZone = TimeZone.getTimeZone(serverTimeZoneId);
        }
        this.defaultDateFormat.setTimeZone(serverTimeZone);
        if (this.recentDateFormat != null) {
            this.recentDateFormat.setTimeZone(serverTimeZone);
        }
    }

    @Override
    public void configure(FTPClientConfig config) {
        DateFormatSymbols dfs = null;
        String languageCode = config.getServerLanguageCode();
        String shortmonths = config.getShortMonthNames();
        dfs = shortmonths != null ? FTPClientConfig.getDateFormatSymbols(shortmonths) : (languageCode != null ? FTPClientConfig.lookupDateFormatSymbols(languageCode) : FTPClientConfig.lookupDateFormatSymbols("en"));
        String recentFormatString = config.getRecentDateFormatStr();
        this.setRecentDateFormat(recentFormatString, dfs);
        String defaultFormatString = config.getDefaultDateFormatStr();
        if (defaultFormatString == null) {
            throw new IllegalArgumentException("defaultFormatString cannot be null");
        }
        this.setDefaultDateFormat(defaultFormatString, dfs);
        this.setServerTimeZone(config.getServerTimeZoneId());
        this.lenientFutureDates = config.isLenientFutureDates();
    }

    boolean isLenientFutureDates() {
        return this.lenientFutureDates;
    }

    void setLenientFutureDates(boolean lenientFutureDates) {
        this.lenientFutureDates = lenientFutureDates;
    }
}


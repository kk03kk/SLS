/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ntp;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TimeStamp
implements Serializable,
Comparable<TimeStamp> {
    private static final long serialVersionUID = 8139806907588338737L;
    protected static final long msb0baseTime = 2085978496000L;
    protected static final long msb1baseTime = -2208988800000L;
    public static final String NTP_DATE_FORMAT = "EEE, MMM dd yyyy HH:mm:ss.SSS";
    private final long ntpTime;
    private DateFormat simpleFormatter;
    private DateFormat utcFormatter;

    public TimeStamp(long ntpTime) {
        this.ntpTime = ntpTime;
    }

    public TimeStamp(String hexStamp) throws NumberFormatException {
        this.ntpTime = TimeStamp.decodeNtpHexString(hexStamp);
    }

    public TimeStamp(Date d) {
        this.ntpTime = d == null ? 0L : TimeStamp.toNtpTime(d.getTime());
    }

    public long ntpValue() {
        return this.ntpTime;
    }

    public long getSeconds() {
        return this.ntpTime >>> 32 & 0xFFFFFFFFL;
    }

    public long getFraction() {
        return this.ntpTime & 0xFFFFFFFFL;
    }

    public long getTime() {
        return TimeStamp.getTime(this.ntpTime);
    }

    public Date getDate() {
        long time = TimeStamp.getTime(this.ntpTime);
        return new Date(time);
    }

    public static long getTime(long ntpTimeValue) {
        long seconds = ntpTimeValue >>> 32 & 0xFFFFFFFFL;
        long fraction = ntpTimeValue & 0xFFFFFFFFL;
        fraction = Math.round(1000.0 * (double)fraction / 4.294967296E9);
        long msb = seconds & 0x80000000L;
        if (msb == 0L) {
            return 2085978496000L + seconds * 1000L + fraction;
        }
        return -2208988800000L + seconds * 1000L + fraction;
    }

    public static TimeStamp getNtpTime(long date) {
        return new TimeStamp(TimeStamp.toNtpTime(date));
    }

    public static TimeStamp getCurrentTime() {
        return TimeStamp.getNtpTime(System.currentTimeMillis());
    }

    protected static long decodeNtpHexString(String hexString) throws NumberFormatException {
        if (hexString == null) {
            throw new NumberFormatException("null");
        }
        int ind = hexString.indexOf(46);
        if (ind == -1) {
            if (hexString.length() == 0) {
                return 0L;
            }
            return Long.parseLong(hexString, 16) << 32;
        }
        return Long.parseLong(hexString.substring(0, ind), 16) << 32 | Long.parseLong(hexString.substring(ind + 1), 16);
    }

    public static TimeStamp parseNtpString(String s) throws NumberFormatException {
        return new TimeStamp(TimeStamp.decodeNtpHexString(s));
    }

    protected static long toNtpTime(long t) {
        boolean useBase1 = t < 2085978496000L;
        long baseTime = useBase1 ? t - -2208988800000L : t - 2085978496000L;
        long seconds = baseTime / 1000L;
        long fraction = baseTime % 1000L * 0x100000000L / 1000L;
        if (useBase1) {
            seconds |= 0x80000000L;
        }
        long time = seconds << 32 | fraction;
        return time;
    }

    public int hashCode() {
        return (int)(this.ntpTime ^ this.ntpTime >>> 32);
    }

    public boolean equals(Object obj) {
        if (obj instanceof TimeStamp) {
            return this.ntpTime == ((TimeStamp)obj).ntpValue();
        }
        return false;
    }

    public String toString() {
        return TimeStamp.toString(this.ntpTime);
    }

    private static void appendHexString(StringBuilder buf, long l) {
        String s = Long.toHexString(l);
        for (int i = s.length(); i < 8; ++i) {
            buf.append('0');
        }
        buf.append(s);
    }

    public static String toString(long ntpTime) {
        StringBuilder buf = new StringBuilder();
        TimeStamp.appendHexString(buf, ntpTime >>> 32 & 0xFFFFFFFFL);
        buf.append('.');
        TimeStamp.appendHexString(buf, ntpTime & 0xFFFFFFFFL);
        return buf.toString();
    }

    public String toDateString() {
        if (this.simpleFormatter == null) {
            this.simpleFormatter = new SimpleDateFormat(NTP_DATE_FORMAT, Locale.US);
            this.simpleFormatter.setTimeZone(TimeZone.getDefault());
        }
        Date ntpDate = this.getDate();
        return this.simpleFormatter.format(ntpDate);
    }

    public String toUTCString() {
        if (this.utcFormatter == null) {
            this.utcFormatter = new SimpleDateFormat("EEE, MMM dd yyyy HH:mm:ss.SSS 'UTC'", Locale.US);
            this.utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        }
        Date ntpDate = this.getDate();
        return this.utcFormatter.format(ntpDate);
    }

    @Override
    public int compareTo(TimeStamp anotherTimeStamp) {
        long thisVal = this.ntpTime;
        long anotherVal = anotherTimeStamp.ntpTime;
        return thisVal < anotherVal ? -1 : (thisVal == anotherVal ? 0 : 1);
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ntp;

import java.net.DatagramPacket;
import org.apache.commons.net.ntp.TimeStamp;

public interface NtpV3Packet {
    public static final int NTP_PORT = 123;
    public static final int LI_NO_WARNING = 0;
    public static final int LI_LAST_MINUTE_HAS_61_SECONDS = 1;
    public static final int LI_LAST_MINUTE_HAS_59_SECONDS = 2;
    public static final int LI_ALARM_CONDITION = 3;
    public static final int MODE_RESERVED = 0;
    public static final int MODE_SYMMETRIC_ACTIVE = 1;
    public static final int MODE_SYMMETRIC_PASSIVE = 2;
    public static final int MODE_CLIENT = 3;
    public static final int MODE_SERVER = 4;
    public static final int MODE_BROADCAST = 5;
    public static final int MODE_CONTROL_MESSAGE = 6;
    public static final int MODE_PRIVATE = 7;
    public static final int NTP_MINPOLL = 4;
    public static final int NTP_MAXPOLL = 14;
    public static final int NTP_MINCLOCK = 1;
    public static final int NTP_MAXCLOCK = 10;
    public static final int VERSION_3 = 3;
    public static final int VERSION_4 = 4;
    public static final String TYPE_NTP = "NTP";
    public static final String TYPE_ICMP = "ICMP";
    public static final String TYPE_TIME = "TIME";
    public static final String TYPE_DAYTIME = "DAYTIME";

    public DatagramPacket getDatagramPacket();

    public void setDatagramPacket(DatagramPacket var1);

    public int getLeapIndicator();

    public void setLeapIndicator(int var1);

    public int getMode();

    public String getModeName();

    public void setMode(int var1);

    public int getPoll();

    public void setPoll(int var1);

    public int getPrecision();

    public void setPrecision(int var1);

    public int getRootDelay();

    public void setRootDelay(int var1);

    public double getRootDelayInMillisDouble();

    public int getRootDispersion();

    public void setRootDispersion(int var1);

    public long getRootDispersionInMillis();

    public double getRootDispersionInMillisDouble();

    public int getVersion();

    public void setVersion(int var1);

    public int getStratum();

    public void setStratum(int var1);

    public String getReferenceIdString();

    public int getReferenceId();

    public void setReferenceId(int var1);

    public TimeStamp getTransmitTimeStamp();

    public TimeStamp getReferenceTimeStamp();

    public TimeStamp getOriginateTimeStamp();

    public TimeStamp getReceiveTimeStamp();

    public void setTransmitTime(TimeStamp var1);

    public void setReferenceTime(TimeStamp var1);

    public void setOriginateTimeStamp(TimeStamp var1);

    public void setReceiveTimeStamp(TimeStamp var1);

    public String getType();
}


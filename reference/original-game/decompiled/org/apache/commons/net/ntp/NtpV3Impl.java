/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ntp;

import java.net.DatagramPacket;
import java.util.Arrays;
import org.apache.commons.net.ntp.NtpUtils;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeStamp;

public class NtpV3Impl
implements NtpV3Packet {
    private static final int MODE_INDEX = 0;
    private static final int MODE_SHIFT = 0;
    private static final int VERSION_INDEX = 0;
    private static final int VERSION_SHIFT = 3;
    private static final int LI_INDEX = 0;
    private static final int LI_SHIFT = 6;
    private static final int STRATUM_INDEX = 1;
    private static final int POLL_INDEX = 2;
    private static final int PRECISION_INDEX = 3;
    private static final int ROOT_DELAY_INDEX = 4;
    private static final int ROOT_DISPERSION_INDEX = 8;
    private static final int REFERENCE_ID_INDEX = 12;
    private static final int REFERENCE_TIMESTAMP_INDEX = 16;
    private static final int ORIGINATE_TIMESTAMP_INDEX = 24;
    private static final int RECEIVE_TIMESTAMP_INDEX = 32;
    private static final int TRANSMIT_TIMESTAMP_INDEX = 40;
    private final byte[] buf = new byte[48];
    private volatile DatagramPacket dp;

    @Override
    public int getMode() {
        return NtpV3Impl.ui(this.buf[0]) >> 0 & 7;
    }

    @Override
    public String getModeName() {
        return NtpUtils.getModeName(this.getMode());
    }

    @Override
    public void setMode(int mode) {
        this.buf[0] = (byte)(this.buf[0] & 0xF8 | mode & 7);
    }

    @Override
    public int getLeapIndicator() {
        return NtpV3Impl.ui(this.buf[0]) >> 6 & 3;
    }

    @Override
    public void setLeapIndicator(int li) {
        this.buf[0] = (byte)(this.buf[0] & 0x3F | (li & 3) << 6);
    }

    @Override
    public int getPoll() {
        return this.buf[2];
    }

    @Override
    public void setPoll(int poll) {
        this.buf[2] = (byte)(poll & 0xFF);
    }

    @Override
    public int getPrecision() {
        return this.buf[3];
    }

    @Override
    public void setPrecision(int precision) {
        this.buf[3] = (byte)(precision & 0xFF);
    }

    @Override
    public int getVersion() {
        return NtpV3Impl.ui(this.buf[0]) >> 3 & 7;
    }

    @Override
    public void setVersion(int version) {
        this.buf[0] = (byte)(this.buf[0] & 0xC7 | (version & 7) << 3);
    }

    @Override
    public int getStratum() {
        return NtpV3Impl.ui(this.buf[1]);
    }

    @Override
    public void setStratum(int stratum) {
        this.buf[1] = (byte)(stratum & 0xFF);
    }

    @Override
    public int getRootDelay() {
        return this.getInt(4);
    }

    @Override
    public void setRootDelay(int delay) {
        this.setInt(4, delay);
    }

    @Override
    public double getRootDelayInMillisDouble() {
        double l = this.getRootDelay();
        return l / 65.536;
    }

    @Override
    public int getRootDispersion() {
        return this.getInt(8);
    }

    @Override
    public void setRootDispersion(int dispersion) {
        this.setInt(8, dispersion);
    }

    @Override
    public long getRootDispersionInMillis() {
        long l = this.getRootDispersion();
        return l * 1000L / 65536L;
    }

    @Override
    public double getRootDispersionInMillisDouble() {
        double l = this.getRootDispersion();
        return l / 65.536;
    }

    @Override
    public void setReferenceId(int refId) {
        this.setInt(12, refId);
    }

    @Override
    public int getReferenceId() {
        return this.getInt(12);
    }

    @Override
    public String getReferenceIdString() {
        int version = this.getVersion();
        int stratum = this.getStratum();
        if (version == 3 || version == 4) {
            if (stratum == 0 || stratum == 1) {
                return this.idAsString();
            }
            if (version == 4) {
                return this.idAsHex();
            }
        }
        if (stratum >= 2) {
            return this.idAsIPAddress();
        }
        return this.idAsHex();
    }

    private String idAsIPAddress() {
        return NtpV3Impl.ui(this.buf[12]) + "." + NtpV3Impl.ui(this.buf[13]) + "." + NtpV3Impl.ui(this.buf[14]) + "." + NtpV3Impl.ui(this.buf[15]);
    }

    private String idAsString() {
        char c;
        StringBuilder id = new StringBuilder();
        for (int i = 0; i <= 3 && (c = (char)this.buf[12 + i]) != '\u0000'; ++i) {
            id.append(c);
        }
        return id.toString();
    }

    private String idAsHex() {
        return Integer.toHexString(this.getReferenceId());
    }

    @Override
    public TimeStamp getTransmitTimeStamp() {
        return this.getTimestamp(40);
    }

    @Override
    public void setTransmitTime(TimeStamp ts) {
        this.setTimestamp(40, ts);
    }

    @Override
    public void setOriginateTimeStamp(TimeStamp ts) {
        this.setTimestamp(24, ts);
    }

    @Override
    public TimeStamp getOriginateTimeStamp() {
        return this.getTimestamp(24);
    }

    @Override
    public TimeStamp getReferenceTimeStamp() {
        return this.getTimestamp(16);
    }

    @Override
    public void setReferenceTime(TimeStamp ts) {
        this.setTimestamp(16, ts);
    }

    @Override
    public TimeStamp getReceiveTimeStamp() {
        return this.getTimestamp(32);
    }

    @Override
    public void setReceiveTimeStamp(TimeStamp ts) {
        this.setTimestamp(32, ts);
    }

    @Override
    public String getType() {
        return "NTP";
    }

    private int getInt(int index) {
        int i = NtpV3Impl.ui(this.buf[index]) << 24 | NtpV3Impl.ui(this.buf[index + 1]) << 16 | NtpV3Impl.ui(this.buf[index + 2]) << 8 | NtpV3Impl.ui(this.buf[index + 3]);
        return i;
    }

    private void setInt(int idx, int value) {
        for (int i = 3; i >= 0; --i) {
            this.buf[idx + i] = (byte)(value & 0xFF);
            value >>>= 8;
        }
    }

    private TimeStamp getTimestamp(int index) {
        return new TimeStamp(this.getLong(index));
    }

    private long getLong(int index) {
        long i = NtpV3Impl.ul(this.buf[index]) << 56 | NtpV3Impl.ul(this.buf[index + 1]) << 48 | NtpV3Impl.ul(this.buf[index + 2]) << 40 | NtpV3Impl.ul(this.buf[index + 3]) << 32 | NtpV3Impl.ul(this.buf[index + 4]) << 24 | NtpV3Impl.ul(this.buf[index + 5]) << 16 | NtpV3Impl.ul(this.buf[index + 6]) << 8 | NtpV3Impl.ul(this.buf[index + 7]);
        return i;
    }

    private void setTimestamp(int index, TimeStamp t) {
        long ntpTime = t == null ? 0L : t.ntpValue();
        for (int i = 7; i >= 0; --i) {
            this.buf[index + i] = (byte)(ntpTime & 0xFFL);
            ntpTime >>>= 8;
        }
    }

    @Override
    public synchronized DatagramPacket getDatagramPacket() {
        if (this.dp == null) {
            this.dp = new DatagramPacket(this.buf, this.buf.length);
            this.dp.setPort(123);
        }
        return this.dp;
    }

    @Override
    public void setDatagramPacket(DatagramPacket srcDp) {
        if (srcDp == null || srcDp.getLength() < this.buf.length) {
            throw new IllegalArgumentException();
        }
        byte[] incomingBuf = srcDp.getData();
        int len = srcDp.getLength();
        if (len > this.buf.length) {
            len = this.buf.length;
        }
        System.arraycopy(incomingBuf, 0, this.buf, 0, len);
        DatagramPacket dp = this.getDatagramPacket();
        dp.setAddress(srcDp.getAddress());
        int port = srcDp.getPort();
        dp.setPort(port > 0 ? port : 123);
        dp.setData(this.buf);
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        NtpV3Impl other = (NtpV3Impl)obj;
        return Arrays.equals(this.buf, other.buf);
    }

    public int hashCode() {
        return Arrays.hashCode(this.buf);
    }

    protected static final int ui(byte b) {
        int i = b & 0xFF;
        return i;
    }

    protected static final long ul(byte b) {
        long i = b & 0xFF;
        return i;
    }

    public String toString() {
        return "[version:" + this.getVersion() + ", mode:" + this.getMode() + ", poll:" + this.getPoll() + ", precision:" + this.getPrecision() + ", delay:" + this.getRootDelay() + ", dispersion(ms):" + this.getRootDispersionInMillisDouble() + ", id:" + this.getReferenceIdString() + ", xmitTime:" + this.getTransmitTimeStamp().toDateString() + " ]";
    }
}


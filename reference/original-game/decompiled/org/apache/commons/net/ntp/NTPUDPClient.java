/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ntp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import org.apache.commons.net.DatagramSocketClient;
import org.apache.commons.net.ntp.NtpV3Impl;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

public final class NTPUDPClient
extends DatagramSocketClient {
    public static final int DEFAULT_PORT = 123;
    private int _version = 3;

    public TimeInfo getTime(InetAddress host, int port) throws IOException {
        if (!this.isOpen()) {
            this.open();
        }
        NtpV3Impl message = new NtpV3Impl();
        message.setMode(3);
        message.setVersion(this._version);
        DatagramPacket sendPacket = message.getDatagramPacket();
        sendPacket.setAddress(host);
        sendPacket.setPort(port);
        NtpV3Impl recMessage = new NtpV3Impl();
        DatagramPacket receivePacket = recMessage.getDatagramPacket();
        TimeStamp now = TimeStamp.getCurrentTime();
        message.setTransmitTime(now);
        this._socket_.send(sendPacket);
        this._socket_.receive(receivePacket);
        long returnTime = System.currentTimeMillis();
        TimeInfo info = new TimeInfo((NtpV3Packet)recMessage, returnTime, false);
        return info;
    }

    public TimeInfo getTime(InetAddress host) throws IOException {
        return this.getTime(host, 123);
    }

    public int getVersion() {
        return this._version;
    }

    public void setVersion(int version) {
        this._version = version;
    }
}


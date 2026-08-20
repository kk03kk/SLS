/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.daytime;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import org.apache.commons.net.DatagramSocketClient;

public final class DaytimeUDPClient
extends DatagramSocketClient {
    public static final int DEFAULT_PORT = 13;
    private final byte[] __dummyData = new byte[1];
    private final byte[] __timeData = new byte[256];

    public String getTime(InetAddress host, int port) throws IOException {
        DatagramPacket sendPacket = new DatagramPacket(this.__dummyData, this.__dummyData.length, host, port);
        DatagramPacket receivePacket = new DatagramPacket(this.__timeData, this.__timeData.length);
        this._socket_.send(sendPacket);
        this._socket_.receive(receivePacket);
        return new String(receivePacket.getData(), 0, receivePacket.getLength(), this.getCharsetName());
    }

    public String getTime(InetAddress host) throws IOException {
        return this.getTime(host, 13);
    }
}


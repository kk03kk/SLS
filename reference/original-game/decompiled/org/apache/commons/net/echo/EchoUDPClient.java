/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.echo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import org.apache.commons.net.discard.DiscardUDPClient;

public final class EchoUDPClient
extends DiscardUDPClient {
    public static final int DEFAULT_PORT = 7;
    private final DatagramPacket __receivePacket = new DatagramPacket(new byte[0], 0);

    @Override
    public void send(byte[] data, int length, InetAddress host) throws IOException {
        this.send(data, length, host, 7);
    }

    @Override
    public void send(byte[] data, InetAddress host) throws IOException {
        this.send(data, data.length, host, 7);
    }

    public int receive(byte[] data, int length) throws IOException {
        this.__receivePacket.setData(data);
        this.__receivePacket.setLength(length);
        this._socket_.receive(this.__receivePacket);
        return this.__receivePacket.getLength();
    }

    public int receive(byte[] data) throws IOException {
        return this.receive(data, data.length);
    }
}


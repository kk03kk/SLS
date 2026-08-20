/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.tftp;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import org.apache.commons.net.DatagramSocketClient;
import org.apache.commons.net.tftp.TFTPPacket;
import org.apache.commons.net.tftp.TFTPPacketException;
import org.apache.commons.net.tftp.TFTPRequestPacket;

public class TFTP
extends DatagramSocketClient {
    public static final int ASCII_MODE = 0;
    public static final int NETASCII_MODE = 0;
    public static final int BINARY_MODE = 1;
    public static final int IMAGE_MODE = 1;
    public static final int OCTET_MODE = 1;
    public static final int DEFAULT_TIMEOUT = 5000;
    public static final int DEFAULT_PORT = 69;
    static final int PACKET_SIZE = 516;
    private byte[] __receiveBuffer;
    private DatagramPacket __receiveDatagram;
    private DatagramPacket __sendDatagram;
    byte[] _sendBuffer;

    public static final String getModeName(int mode) {
        return TFTPRequestPacket._modeStrings[mode];
    }

    public TFTP() {
        this.setDefaultTimeout(5000);
        this.__receiveBuffer = null;
        this.__receiveDatagram = null;
    }

    public final void discardPackets() throws IOException {
        DatagramPacket datagram = new DatagramPacket(new byte[516], 516);
        int to = this.getSoTimeout();
        this.setSoTimeout(1);
        try {
            while (true) {
                this._socket_.receive(datagram);
            }
        }
        catch (SocketException e) {
        }
        catch (InterruptedIOException interruptedIOException) {
            // empty catch block
        }
        this.setSoTimeout(to);
    }

    public final TFTPPacket bufferedReceive() throws IOException, InterruptedIOException, SocketException, TFTPPacketException {
        this.__receiveDatagram.setData(this.__receiveBuffer);
        this.__receiveDatagram.setLength(this.__receiveBuffer.length);
        this._socket_.receive(this.__receiveDatagram);
        return TFTPPacket.newTFTPPacket(this.__receiveDatagram);
    }

    public final void bufferedSend(TFTPPacket packet) throws IOException {
        this._socket_.send(packet._newDatagram(this.__sendDatagram, this._sendBuffer));
    }

    public final void beginBufferedOps() {
        this.__receiveBuffer = new byte[516];
        this.__receiveDatagram = new DatagramPacket(this.__receiveBuffer, this.__receiveBuffer.length);
        this._sendBuffer = new byte[516];
        this.__sendDatagram = new DatagramPacket(this._sendBuffer, this._sendBuffer.length);
    }

    public final void endBufferedOps() {
        this.__receiveBuffer = null;
        this.__receiveDatagram = null;
        this._sendBuffer = null;
        this.__sendDatagram = null;
    }

    public final void send(TFTPPacket packet) throws IOException {
        this._socket_.send(packet.newDatagram());
    }

    public final TFTPPacket receive() throws IOException, InterruptedIOException, SocketException, TFTPPacketException {
        DatagramPacket packet = new DatagramPacket(new byte[516], 516);
        this._socket_.receive(packet);
        return TFTPPacket.newTFTPPacket(packet);
    }
}


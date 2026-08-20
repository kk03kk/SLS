/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import org.apache.commons.net.tftp.TFTPPacket;
import org.apache.commons.net.tftp.TFTPPacketException;

public final class TFTPErrorPacket
extends TFTPPacket {
    public static final int UNDEFINED = 0;
    public static final int FILE_NOT_FOUND = 1;
    public static final int ACCESS_VIOLATION = 2;
    public static final int OUT_OF_SPACE = 3;
    public static final int ILLEGAL_OPERATION = 4;
    public static final int UNKNOWN_TID = 5;
    public static final int FILE_EXISTS = 6;
    public static final int NO_SUCH_USER = 7;
    int _error;
    String _message;

    public TFTPErrorPacket(InetAddress destination, int port, int error, String message) {
        super(5, destination, port);
        this._error = error;
        this._message = message;
    }

    TFTPErrorPacket(DatagramPacket datagram) throws TFTPPacketException {
        super(5, datagram.getAddress(), datagram.getPort());
        byte[] data = datagram.getData();
        int length = datagram.getLength();
        if (this.getType() != data[1]) {
            throw new TFTPPacketException("TFTP operator code does not match type.");
        }
        this._error = (data[2] & 0xFF) << 8 | data[3] & 0xFF;
        if (length < 5) {
            throw new TFTPPacketException("Bad error packet. No message.");
        }
        StringBuilder buffer = new StringBuilder();
        for (int index = 4; index < length && data[index] != 0; ++index) {
            buffer.append((char)data[index]);
        }
        this._message = buffer.toString();
    }

    @Override
    DatagramPacket _newDatagram(DatagramPacket datagram, byte[] data) {
        int length = this._message.length();
        data[0] = 0;
        data[1] = (byte)this._type;
        data[2] = (byte)((this._error & 0xFFFF) >> 8);
        data[3] = (byte)(this._error & 0xFF);
        System.arraycopy(this._message.getBytes(), 0, data, 4, length);
        data[length + 4] = 0;
        datagram.setAddress(this._address);
        datagram.setPort(this._port);
        datagram.setData(data);
        datagram.setLength(length + 4);
        return datagram;
    }

    @Override
    public DatagramPacket newDatagram() {
        int length = this._message.length();
        byte[] data = new byte[length + 5];
        data[0] = 0;
        data[1] = (byte)this._type;
        data[2] = (byte)((this._error & 0xFFFF) >> 8);
        data[3] = (byte)(this._error & 0xFF);
        System.arraycopy(this._message.getBytes(), 0, data, 4, length);
        data[length + 4] = 0;
        return new DatagramPacket(data, data.length, this._address, this._port);
    }

    public int getError() {
        return this._error;
    }

    public String getMessage() {
        return this._message;
    }
}


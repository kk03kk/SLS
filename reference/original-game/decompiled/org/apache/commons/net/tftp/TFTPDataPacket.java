/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import org.apache.commons.net.tftp.TFTPPacket;
import org.apache.commons.net.tftp.TFTPPacketException;

public final class TFTPDataPacket
extends TFTPPacket {
    public static final int MAX_DATA_LENGTH = 512;
    public static final int MIN_DATA_LENGTH = 0;
    int _blockNumber;
    int _length;
    int _offset;
    byte[] _data;

    public TFTPDataPacket(InetAddress destination, int port, int blockNumber, byte[] data, int offset, int length) {
        super(3, destination, port);
        this._blockNumber = blockNumber;
        this._data = data;
        this._offset = offset;
        this._length = length > 512 ? 512 : length;
    }

    public TFTPDataPacket(InetAddress destination, int port, int blockNumber, byte[] data) {
        this(destination, port, blockNumber, data, 0, data.length);
    }

    TFTPDataPacket(DatagramPacket datagram) throws TFTPPacketException {
        super(3, datagram.getAddress(), datagram.getPort());
        this._data = datagram.getData();
        this._offset = 4;
        if (this.getType() != this._data[1]) {
            throw new TFTPPacketException("TFTP operator code does not match type.");
        }
        this._blockNumber = (this._data[2] & 0xFF) << 8 | this._data[3] & 0xFF;
        this._length = datagram.getLength() - 4;
        if (this._length > 512) {
            this._length = 512;
        }
    }

    @Override
    DatagramPacket _newDatagram(DatagramPacket datagram, byte[] data) {
        data[0] = 0;
        data[1] = (byte)this._type;
        data[2] = (byte)((this._blockNumber & 0xFFFF) >> 8);
        data[3] = (byte)(this._blockNumber & 0xFF);
        if (data != this._data) {
            System.arraycopy(this._data, this._offset, data, 4, this._length);
        }
        datagram.setAddress(this._address);
        datagram.setPort(this._port);
        datagram.setData(data);
        datagram.setLength(this._length + 4);
        return datagram;
    }

    @Override
    public DatagramPacket newDatagram() {
        byte[] data = new byte[this._length + 4];
        data[0] = 0;
        data[1] = (byte)this._type;
        data[2] = (byte)((this._blockNumber & 0xFFFF) >> 8);
        data[3] = (byte)(this._blockNumber & 0xFF);
        System.arraycopy(this._data, this._offset, data, 4, this._length);
        return new DatagramPacket(data, this._length + 4, this._address, this._port);
    }

    public int getBlockNumber() {
        return this._blockNumber;
    }

    public void setBlockNumber(int blockNumber) {
        this._blockNumber = blockNumber;
    }

    public void setData(byte[] data, int offset, int length) {
        this._data = data;
        this._offset = offset;
        this._length = length;
        this._length = length > 512 ? 512 : length;
    }

    public int getDataLength() {
        return this._length;
    }

    public int getDataOffset() {
        return this._offset;
    }

    public byte[] getData() {
        return this._data;
    }
}


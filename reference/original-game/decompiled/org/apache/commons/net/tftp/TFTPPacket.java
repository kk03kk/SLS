/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import org.apache.commons.net.tftp.TFTPAckPacket;
import org.apache.commons.net.tftp.TFTPDataPacket;
import org.apache.commons.net.tftp.TFTPErrorPacket;
import org.apache.commons.net.tftp.TFTPPacketException;
import org.apache.commons.net.tftp.TFTPReadRequestPacket;
import org.apache.commons.net.tftp.TFTPWriteRequestPacket;

public abstract class TFTPPacket {
    static final int MIN_PACKET_SIZE = 4;
    public static final int READ_REQUEST = 1;
    public static final int WRITE_REQUEST = 2;
    public static final int DATA = 3;
    public static final int ACKNOWLEDGEMENT = 4;
    public static final int ERROR = 5;
    public static final int SEGMENT_SIZE = 512;
    int _type;
    int _port;
    InetAddress _address;

    public static final TFTPPacket newTFTPPacket(DatagramPacket datagram) throws TFTPPacketException {
        TFTPPacket packet = null;
        if (datagram.getLength() < 4) {
            throw new TFTPPacketException("Bad packet. Datagram data length is too short.");
        }
        byte[] data = datagram.getData();
        switch (data[1]) {
            case 1: {
                packet = new TFTPReadRequestPacket(datagram);
                break;
            }
            case 2: {
                packet = new TFTPWriteRequestPacket(datagram);
                break;
            }
            case 3: {
                packet = new TFTPDataPacket(datagram);
                break;
            }
            case 4: {
                packet = new TFTPAckPacket(datagram);
                break;
            }
            case 5: {
                packet = new TFTPErrorPacket(datagram);
                break;
            }
            default: {
                throw new TFTPPacketException("Bad packet.  Invalid TFTP operator code.");
            }
        }
        return packet;
    }

    TFTPPacket(int type, InetAddress address, int port) {
        this._type = type;
        this._address = address;
        this._port = port;
    }

    abstract DatagramPacket _newDatagram(DatagramPacket var1, byte[] var2);

    public abstract DatagramPacket newDatagram();

    public final int getType() {
        return this._type;
    }

    public final InetAddress getAddress() {
        return this._address;
    }

    public final int getPort() {
        return this._port;
    }

    public final void setPort(int port) {
        this._port = port;
    }

    public final void setAddress(InetAddress address) {
        this._address = address;
    }
}


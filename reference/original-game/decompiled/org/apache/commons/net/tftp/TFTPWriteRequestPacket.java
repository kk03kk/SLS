/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.tftp;

import java.net.DatagramPacket;
import java.net.InetAddress;
import org.apache.commons.net.tftp.TFTPPacketException;
import org.apache.commons.net.tftp.TFTPRequestPacket;

public final class TFTPWriteRequestPacket
extends TFTPRequestPacket {
    public TFTPWriteRequestPacket(InetAddress destination, int port, String filename, int mode) {
        super(destination, port, 2, filename, mode);
    }

    TFTPWriteRequestPacket(DatagramPacket datagram) throws TFTPPacketException {
        super(2, datagram);
    }
}


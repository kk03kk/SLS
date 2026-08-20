/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.tftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.apache.commons.net.io.FromNetASCIIOutputStream;
import org.apache.commons.net.io.ToNetASCIIInputStream;
import org.apache.commons.net.tftp.TFTP;
import org.apache.commons.net.tftp.TFTPAckPacket;
import org.apache.commons.net.tftp.TFTPDataPacket;
import org.apache.commons.net.tftp.TFTPErrorPacket;
import org.apache.commons.net.tftp.TFTPPacket;
import org.apache.commons.net.tftp.TFTPPacketException;
import org.apache.commons.net.tftp.TFTPReadRequestPacket;
import org.apache.commons.net.tftp.TFTPWriteRequestPacket;

public class TFTPClient
extends TFTP {
    public static final int DEFAULT_MAX_TIMEOUTS = 5;
    private int __maxTimeouts = 5;

    public void setMaxTimeouts(int numTimeouts) {
        this.__maxTimeouts = numTimeouts < 1 ? 1 : numTimeouts;
    }

    public int getMaxTimeouts() {
        return this.__maxTimeouts;
    }

    public int receiveFile(String filename, int mode, OutputStream output, InetAddress host, int port) throws IOException {
        TFTPPacket received = null;
        TFTPAckPacket ack = new TFTPAckPacket(host, port, 0);
        this.beginBufferedOps();
        int bytesRead = 0;
        int hostPort = 0;
        int lastBlock = 0;
        int dataLength = 0;
        int block = 1;
        if (mode == 0) {
            output = new FromNetASCIIOutputStream(output);
        }
        TFTPPacket sent = new TFTPReadRequestPacket(host, port, filename, mode);
        block10: do {
            block16: {
                TFTPErrorPacket error;
                this.bufferedSend(sent);
                block11: while (true) {
                    int timeouts = 0;
                    try {
                        received = this.bufferedReceive();
                    }
                    catch (SocketException e) {
                        if (++timeouts < this.__maxTimeouts) continue block10;
                        this.endBufferedOps();
                        throw new IOException("Connection timed out.");
                    }
                    catch (InterruptedIOException e) {
                        if (++timeouts < this.__maxTimeouts) continue block10;
                        this.endBufferedOps();
                        throw new IOException("Connection timed out.");
                    }
                    catch (TFTPPacketException e) {
                        this.endBufferedOps();
                        throw new IOException("Bad packet: " + e.getMessage());
                    }
                    if (lastBlock == 0) {
                        hostPort = received.getPort();
                        ack.setPort(hostPort);
                        if (!host.equals(received.getAddress())) {
                            host = received.getAddress();
                            ack.setAddress(host);
                            sent.setAddress(host);
                        }
                    }
                    if (!host.equals(received.getAddress()) || received.getPort() != hostPort) break;
                    switch (received.getType()) {
                        case 5: {
                            error = (TFTPErrorPacket)received;
                            this.endBufferedOps();
                            throw new IOException("Error code " + error.getError() + " received: " + error.getMessage());
                        }
                        case 3: {
                            TFTPDataPacket data = (TFTPDataPacket)received;
                            dataLength = data.getDataLength();
                            lastBlock = data.getBlockNumber();
                            if (lastBlock == block) {
                                try {
                                    output.write(data.getData(), data.getDataOffset(), dataLength);
                                }
                                catch (IOException e) {
                                    error = new TFTPErrorPacket(host, hostPort, 3, "File write failed.");
                                    this.bufferedSend(error);
                                    this.endBufferedOps();
                                    throw e;
                                }
                                if (++block <= 65535) break block16;
                                block = 0;
                                break block16;
                            }
                            this.discardPackets();
                            if (lastBlock != (block == 0 ? 65535 : block - 1)) continue block11;
                            continue block10;
                        }
                        default: {
                            this.endBufferedOps();
                            throw new IOException("Received unexpected packet type.");
                        }
                    }
                    break;
                }
                error = new TFTPErrorPacket(received.getAddress(), received.getPort(), 5, "Unexpected host or port.");
                this.bufferedSend(error);
                continue;
            }
            ack.setBlockNumber(lastBlock);
            sent = ack;
            bytesRead += dataLength;
        } while (dataLength == 512);
        this.bufferedSend(sent);
        this.endBufferedOps();
        return bytesRead;
    }

    public int receiveFile(String filename, int mode, OutputStream output, String hostname, int port) throws UnknownHostException, IOException {
        return this.receiveFile(filename, mode, output, InetAddress.getByName(hostname), port);
    }

    public int receiveFile(String filename, int mode, OutputStream output, InetAddress host) throws IOException {
        return this.receiveFile(filename, mode, output, host, 69);
    }

    public int receiveFile(String filename, int mode, OutputStream output, String hostname) throws UnknownHostException, IOException {
        return this.receiveFile(filename, mode, output, InetAddress.getByName(hostname), 69);
    }

    public void sendFile(String filename, int mode, InputStream input, InetAddress host, int port) throws IOException {
        TFTPPacket received = null;
        TFTPDataPacket data = new TFTPDataPacket(host, port, 0, this._sendBuffer, 4, 0);
        boolean justStarted = true;
        this.beginBufferedOps();
        int totalThisPacket = 0;
        int bytesRead = 0;
        int hostPort = 0;
        int lastBlock = 0;
        int dataLength = 0;
        int block = 0;
        boolean lastAckWait = false;
        if (mode == 0) {
            input = new ToNetASCIIInputStream(input);
        }
        TFTPPacket sent = new TFTPWriteRequestPacket(host, port, filename, mode);
        block8: do {
            block18: {
                TFTPErrorPacket error;
                this.bufferedSend(sent);
                block9: while (true) {
                    int timeouts = 0;
                    try {
                        received = this.bufferedReceive();
                    }
                    catch (SocketException e) {
                        if (++timeouts < this.__maxTimeouts) continue block8;
                        this.endBufferedOps();
                        throw new IOException("Connection timed out.");
                    }
                    catch (InterruptedIOException e) {
                        if (++timeouts < this.__maxTimeouts) continue block8;
                        this.endBufferedOps();
                        throw new IOException("Connection timed out.");
                    }
                    catch (TFTPPacketException e) {
                        this.endBufferedOps();
                        throw new IOException("Bad packet: " + e.getMessage());
                    }
                    if (justStarted) {
                        justStarted = false;
                        hostPort = received.getPort();
                        data.setPort(hostPort);
                        if (!host.equals(received.getAddress())) {
                            host = received.getAddress();
                            data.setAddress(host);
                            sent.setAddress(host);
                        }
                    }
                    if (!host.equals(received.getAddress()) || received.getPort() != hostPort) break;
                    switch (received.getType()) {
                        case 5: {
                            error = (TFTPErrorPacket)received;
                            this.endBufferedOps();
                            throw new IOException("Error code " + error.getError() + " received: " + error.getMessage());
                        }
                        case 4: {
                            TFTPAckPacket ack = (TFTPAckPacket)received;
                            lastBlock = ack.getBlockNumber();
                            if (lastBlock == block) {
                                if (++block > 65535) {
                                    block = 0;
                                }
                                if (lastAckWait) {
                                    break block8;
                                }
                                break block18;
                            }
                            this.discardPackets();
                            continue block9;
                        }
                        default: {
                            this.endBufferedOps();
                            throw new IOException("Received unexpected packet type.");
                        }
                    }
                    break;
                }
                error = new TFTPErrorPacket(received.getAddress(), received.getPort(), 5, "Unexpected host or port.");
                this.bufferedSend(error);
                continue;
            }
            dataLength = 512;
            int offset = 4;
            totalThisPacket = 0;
            while (dataLength > 0 && (bytesRead = input.read(this._sendBuffer, offset, dataLength)) > 0) {
                offset += bytesRead;
                dataLength -= bytesRead;
                totalThisPacket += bytesRead;
            }
            if (totalThisPacket < 512) {
                lastAckWait = true;
            }
            data.setBlockNumber(block);
            data.setData(this._sendBuffer, 4, totalThisPacket);
            sent = data;
        } while (totalThisPacket > 0 || lastAckWait);
        this.endBufferedOps();
    }

    public void sendFile(String filename, int mode, InputStream input, String hostname, int port) throws UnknownHostException, IOException {
        this.sendFile(filename, mode, input, InetAddress.getByName(hostname), port);
    }

    public void sendFile(String filename, int mode, InputStream input, InetAddress host) throws IOException {
        this.sendFile(filename, mode, input, host, 69);
    }

    public void sendFile(String filename, int mode, InputStream input, String hostname) throws UnknownHostException, IOException {
        this.sendFile(filename, mode, input, InetAddress.getByName(hostname), 69);
    }
}


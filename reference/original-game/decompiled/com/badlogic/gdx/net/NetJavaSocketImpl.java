/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.net;

import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.net.SocketHints;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class NetJavaSocketImpl
implements Socket {
    private java.net.Socket socket;

    public NetJavaSocketImpl(Net.Protocol protocol, String host, int port, SocketHints hints) {
        try {
            this.socket = new java.net.Socket();
            this.applyHints(hints);
            InetSocketAddress address = new InetSocketAddress(host, port);
            if (hints != null) {
                this.socket.connect(address, hints.connectTimeout);
            } else {
                this.socket.connect(address);
            }
        }
        catch (Exception e) {
            throw new GdxRuntimeException("Error making a socket connection to " + host + ":" + port, e);
        }
    }

    public NetJavaSocketImpl(java.net.Socket socket, SocketHints hints) {
        this.socket = socket;
        this.applyHints(hints);
    }

    private void applyHints(SocketHints hints) {
        if (hints != null) {
            try {
                this.socket.setPerformancePreferences(hints.performancePrefConnectionTime, hints.performancePrefLatency, hints.performancePrefBandwidth);
                this.socket.setTrafficClass(hints.trafficClass);
                this.socket.setTcpNoDelay(hints.tcpNoDelay);
                this.socket.setKeepAlive(hints.keepAlive);
                this.socket.setSendBufferSize(hints.sendBufferSize);
                this.socket.setReceiveBufferSize(hints.receiveBufferSize);
                this.socket.setSoLinger(hints.linger, hints.lingerDuration);
                this.socket.setSoTimeout(hints.socketTimeout);
            }
            catch (Exception e) {
                throw new GdxRuntimeException("Error setting socket hints.", e);
            }
        }
    }

    @Override
    public boolean isConnected() {
        if (this.socket != null) {
            return this.socket.isConnected();
        }
        return false;
    }

    @Override
    public InputStream getInputStream() {
        try {
            return this.socket.getInputStream();
        }
        catch (Exception e) {
            throw new GdxRuntimeException("Error getting input stream from socket.", e);
        }
    }

    @Override
    public OutputStream getOutputStream() {
        try {
            return this.socket.getOutputStream();
        }
        catch (Exception e) {
            throw new GdxRuntimeException("Error getting output stream from socket.", e);
        }
    }

    @Override
    public String getRemoteAddress() {
        return this.socket.getRemoteSocketAddress().toString();
    }

    @Override
    public void dispose() {
        if (this.socket != null) {
            try {
                this.socket.close();
                this.socket = null;
            }
            catch (Exception e) {
                throw new GdxRuntimeException("Error closing socket.", e);
            }
        }
    }
}


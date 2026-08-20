/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;

public class FTPSServerSocketFactory
extends ServerSocketFactory {
    private final SSLContext context;

    public FTPSServerSocketFactory(SSLContext context) {
        this.context = context;
    }

    @Override
    public ServerSocket createServerSocket() throws IOException {
        return this.init(this.context.getServerSocketFactory().createServerSocket());
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        return this.init(this.context.getServerSocketFactory().createServerSocket(port));
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog) throws IOException {
        return this.init(this.context.getServerSocketFactory().createServerSocket(port, backlog));
    }

    @Override
    public ServerSocket createServerSocket(int port, int backlog, InetAddress ifAddress) throws IOException {
        return this.init(this.context.getServerSocketFactory().createServerSocket(port, backlog, ifAddress));
    }

    public ServerSocket init(ServerSocket socket) {
        ((SSLServerSocket)socket).setUseClientMode(true);
        return socket;
    }
}


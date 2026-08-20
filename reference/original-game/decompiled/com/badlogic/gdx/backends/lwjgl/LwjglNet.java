/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.lwjgl;

import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.NetJavaImpl;
import com.badlogic.gdx.net.NetJavaServerSocketImpl;
import com.badlogic.gdx.net.NetJavaSocketImpl;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.ServerSocketHints;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.net.SocketHints;
import org.lwjgl.Sys;

public class LwjglNet
implements Net {
    NetJavaImpl netJavaImpl = new NetJavaImpl();

    @Override
    public void sendHttpRequest(Net.HttpRequest httpRequest, Net.HttpResponseListener httpResponseListener) {
        this.netJavaImpl.sendHttpRequest(httpRequest, httpResponseListener);
    }

    @Override
    public void cancelHttpRequest(Net.HttpRequest httpRequest) {
        this.netJavaImpl.cancelHttpRequest(httpRequest);
    }

    @Override
    public ServerSocket newServerSocket(Net.Protocol protocol, String ipAddress, int port, ServerSocketHints hints) {
        return new NetJavaServerSocketImpl(protocol, ipAddress, port, hints);
    }

    @Override
    public ServerSocket newServerSocket(Net.Protocol protocol, int port, ServerSocketHints hints) {
        return new NetJavaServerSocketImpl(protocol, port, hints);
    }

    @Override
    public Socket newClientSocket(Net.Protocol protocol, String host, int port, SocketHints hints) {
        return new NetJavaSocketImpl(protocol, host, port, hints);
    }

    @Override
    public boolean openURI(String URI2) {
        return Sys.openURL(URI2);
    }
}


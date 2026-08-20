/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.backends.headless;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.NetJavaImpl;
import com.badlogic.gdx.net.NetJavaServerSocketImpl;
import com.badlogic.gdx.net.NetJavaSocketImpl;
import com.badlogic.gdx.net.ServerSocket;
import com.badlogic.gdx.net.ServerSocketHints;
import com.badlogic.gdx.net.Socket;
import com.badlogic.gdx.net.SocketHints;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.net.URI;

public class HeadlessNet
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
    public ServerSocket newServerSocket(Net.Protocol protocol, String hostname, int port, ServerSocketHints hints) {
        return new NetJavaServerSocketImpl(protocol, hostname, port, hints);
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
        boolean result = false;
        try {
            if (!GraphicsEnvironment.isHeadless() && Desktop.isDesktopSupported()) {
                if (Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create(URI2));
                    result = true;
                }
            } else {
                Gdx.app.error("HeadlessNet", "Opening URIs on this environment is not supported. Ignoring.");
            }
        }
        catch (Throwable t) {
            Gdx.app.error("HeadlessNet", "Failed to open URI. ", t);
        }
        return result;
    }
}


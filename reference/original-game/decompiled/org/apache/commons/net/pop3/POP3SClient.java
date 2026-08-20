/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.pop3;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import org.apache.commons.net.io.CRLFLineReader;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.util.SSLContextUtils;
import org.apache.commons.net.util.SSLSocketUtils;

public class POP3SClient
extends POP3Client {
    private static final int DEFAULT_POP3S_PORT = 995;
    private static final String DEFAULT_PROTOCOL = "TLS";
    private final boolean isImplicit;
    private final String protocol;
    private SSLContext context = null;
    private String[] suites = null;
    private String[] protocols = null;
    private TrustManager trustManager = null;
    private KeyManager keyManager = null;
    private HostnameVerifier hostnameVerifier = null;
    private boolean tlsEndpointChecking;

    public POP3SClient() {
        this(DEFAULT_PROTOCOL, false);
    }

    public POP3SClient(boolean implicit) {
        this(DEFAULT_PROTOCOL, implicit);
    }

    public POP3SClient(String proto) {
        this(proto, false);
    }

    public POP3SClient(String proto, boolean implicit) {
        this(proto, implicit, null);
    }

    public POP3SClient(String proto, boolean implicit, SSLContext ctx) {
        this.protocol = proto;
        this.isImplicit = implicit;
        this.context = ctx;
        if (this.isImplicit) {
            this.setDefaultPort(995);
        }
    }

    public POP3SClient(boolean implicit, SSLContext ctx) {
        this(DEFAULT_PROTOCOL, implicit, ctx);
    }

    public POP3SClient(SSLContext context) {
        this(false, context);
    }

    @Override
    protected void _connectAction_() throws IOException {
        if (this.isImplicit) {
            this.performSSLNegotiation();
        }
        super._connectAction_();
    }

    private void initSSLContext() throws IOException {
        if (this.context == null) {
            this.context = SSLContextUtils.createSSLContext(this.protocol, this.getKeyManager(), this.getTrustManager());
        }
    }

    private void performSSLNegotiation() throws IOException {
        this.initSSLContext();
        SSLSocketFactory ssf = this.context.getSocketFactory();
        String host = this._hostname_ != null ? this._hostname_ : this.getRemoteAddress().getHostAddress();
        int port = this.getRemotePort();
        SSLSocket socket = (SSLSocket)ssf.createSocket(this._socket_, host, port, true);
        socket.setEnableSessionCreation(true);
        socket.setUseClientMode(true);
        if (this.tlsEndpointChecking) {
            SSLSocketUtils.enableEndpointNameVerification(socket);
        }
        if (this.protocols != null) {
            socket.setEnabledProtocols(this.protocols);
        }
        if (this.suites != null) {
            socket.setEnabledCipherSuites(this.suites);
        }
        socket.startHandshake();
        this._socket_ = socket;
        this._input_ = socket.getInputStream();
        this._output_ = socket.getOutputStream();
        this._reader = new CRLFLineReader(new InputStreamReader(this._input_, "ISO-8859-1"));
        this._writer = new BufferedWriter(new OutputStreamWriter(this._output_, "ISO-8859-1"));
        if (this.hostnameVerifier != null && !this.hostnameVerifier.verify(host, socket.getSession())) {
            throw new SSLHandshakeException("Hostname doesn't match certificate");
        }
    }

    private KeyManager getKeyManager() {
        return this.keyManager;
    }

    public void setKeyManager(KeyManager newKeyManager) {
        this.keyManager = newKeyManager;
    }

    public void setEnabledCipherSuites(String[] cipherSuites) {
        this.suites = new String[cipherSuites.length];
        System.arraycopy(cipherSuites, 0, this.suites, 0, cipherSuites.length);
    }

    public String[] getEnabledCipherSuites() {
        if (this._socket_ instanceof SSLSocket) {
            return ((SSLSocket)this._socket_).getEnabledCipherSuites();
        }
        return null;
    }

    public void setEnabledProtocols(String[] protocolVersions) {
        this.protocols = new String[protocolVersions.length];
        System.arraycopy(protocolVersions, 0, this.protocols, 0, protocolVersions.length);
    }

    public String[] getEnabledProtocols() {
        if (this._socket_ instanceof SSLSocket) {
            return ((SSLSocket)this._socket_).getEnabledProtocols();
        }
        return null;
    }

    public boolean execTLS() throws SSLException, IOException {
        if (this.sendCommand("STLS") != 0) {
            return false;
        }
        this.performSSLNegotiation();
        return true;
    }

    public TrustManager getTrustManager() {
        return this.trustManager;
    }

    public void setTrustManager(TrustManager newTrustManager) {
        this.trustManager = newTrustManager;
    }

    public HostnameVerifier getHostnameVerifier() {
        return this.hostnameVerifier;
    }

    public void setHostnameVerifier(HostnameVerifier newHostnameVerifier) {
        this.hostnameVerifier = newHostnameVerifier;
    }

    public boolean isEndpointCheckingEnabled() {
        return this.tlsEndpointChecking;
    }

    public void setEndpointCheckingEnabled(boolean enable) {
        this.tlsEndpointChecking = enable;
    }
}


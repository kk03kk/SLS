/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCommand;
import org.apache.commons.net.ftp.FTPSServerSocketFactory;
import org.apache.commons.net.ftp.FTPSSocketFactory;
import org.apache.commons.net.util.Base64;
import org.apache.commons.net.util.SSLContextUtils;
import org.apache.commons.net.util.SSLSocketUtils;
import org.apache.commons.net.util.TrustManagerUtils;

public class FTPSClient
extends FTPClient {
    public static final int DEFAULT_FTPS_DATA_PORT = 989;
    public static final int DEFAULT_FTPS_PORT = 990;
    private static final String[] PROT_COMMAND_VALUE = new String[]{"C", "E", "S", "P"};
    private static final String DEFAULT_PROT = "C";
    private static final String DEFAULT_PROTOCOL = "TLS";
    private static final String CMD_AUTH = "AUTH";
    private static final String CMD_ADAT = "ADAT";
    private static final String CMD_PROT = "PROT";
    private static final String CMD_PBSZ = "PBSZ";
    private static final String CMD_MIC = "MIC";
    private static final String CMD_CONF = "CONF";
    private static final String CMD_ENC = "ENC";
    private static final String CMD_CCC = "CCC";
    private final boolean isImplicit;
    private final String protocol;
    private String auth = "TLS";
    private SSLContext context;
    private Socket plainSocket;
    private boolean isCreation = true;
    private boolean isClientMode = true;
    private boolean isNeedClientAuth = false;
    private boolean isWantClientAuth = false;
    private String[] suites = null;
    private String[] protocols = null;
    private TrustManager trustManager = TrustManagerUtils.getValidateServerCertificateTrustManager();
    private KeyManager keyManager = null;
    private HostnameVerifier hostnameVerifier = null;
    private boolean tlsEndpointChecking;
    @Deprecated
    public static String KEYSTORE_ALGORITHM;
    @Deprecated
    public static String TRUSTSTORE_ALGORITHM;
    @Deprecated
    public static String PROVIDER;
    @Deprecated
    public static String STORE_TYPE;

    public FTPSClient() {
        this(DEFAULT_PROTOCOL, false);
    }

    public FTPSClient(boolean isImplicit) {
        this(DEFAULT_PROTOCOL, isImplicit);
    }

    public FTPSClient(String protocol) {
        this(protocol, false);
    }

    public FTPSClient(String protocol, boolean isImplicit) {
        this.protocol = protocol;
        this.isImplicit = isImplicit;
        if (isImplicit) {
            this.setDefaultPort(990);
        }
    }

    public FTPSClient(boolean isImplicit, SSLContext context) {
        this(DEFAULT_PROTOCOL, isImplicit);
        this.context = context;
    }

    public FTPSClient(SSLContext context) {
        this(false, context);
    }

    public void setAuthValue(String auth) {
        this.auth = auth;
    }

    public String getAuthValue() {
        return this.auth;
    }

    @Override
    protected void _connectAction_() throws IOException {
        if (this.isImplicit) {
            this.sslNegotiation();
        }
        super._connectAction_();
        if (!this.isImplicit) {
            this.execAUTH();
            this.sslNegotiation();
        }
    }

    protected void execAUTH() throws SSLException, IOException {
        int replyCode = this.sendCommand(CMD_AUTH, this.auth);
        if (334 != replyCode && 234 != replyCode) {
            throw new SSLException(this.getReplyString());
        }
    }

    private void initSslContext() throws IOException {
        if (this.context == null) {
            this.context = SSLContextUtils.createSSLContext(this.protocol, this.getKeyManager(), this.getTrustManager());
        }
    }

    protected void sslNegotiation() throws IOException {
        this.plainSocket = this._socket_;
        this.initSslContext();
        SSLSocketFactory ssf = this.context.getSocketFactory();
        String host = this._hostname_ != null ? this._hostname_ : this.getRemoteAddress().getHostAddress();
        int port = this._socket_.getPort();
        SSLSocket socket = (SSLSocket)ssf.createSocket(this._socket_, host, port, false);
        socket.setEnableSessionCreation(this.isCreation);
        socket.setUseClientMode(this.isClientMode);
        if (this.isClientMode) {
            if (this.tlsEndpointChecking) {
                SSLSocketUtils.enableEndpointNameVerification(socket);
            }
        } else {
            socket.setNeedClientAuth(this.isNeedClientAuth);
            socket.setWantClientAuth(this.isWantClientAuth);
        }
        if (this.protocols != null) {
            socket.setEnabledProtocols(this.protocols);
        }
        if (this.suites != null) {
            socket.setEnabledCipherSuites(this.suites);
        }
        socket.startHandshake();
        this._socket_ = socket;
        this._controlInput_ = new BufferedReader(new InputStreamReader(socket.getInputStream(), this.getControlEncoding()));
        this._controlOutput_ = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), this.getControlEncoding()));
        if (this.isClientMode && this.hostnameVerifier != null && !this.hostnameVerifier.verify(host, socket.getSession())) {
            throw new SSLHandshakeException("Hostname doesn't match certificate");
        }
    }

    private KeyManager getKeyManager() {
        return this.keyManager;
    }

    public void setKeyManager(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    public void setEnabledSessionCreation(boolean isCreation) {
        this.isCreation = isCreation;
    }

    public boolean getEnableSessionCreation() {
        if (this._socket_ instanceof SSLSocket) {
            return ((SSLSocket)this._socket_).getEnableSessionCreation();
        }
        return false;
    }

    public void setNeedClientAuth(boolean isNeedClientAuth) {
        this.isNeedClientAuth = isNeedClientAuth;
    }

    public boolean getNeedClientAuth() {
        if (this._socket_ instanceof SSLSocket) {
            return ((SSLSocket)this._socket_).getNeedClientAuth();
        }
        return false;
    }

    public void setWantClientAuth(boolean isWantClientAuth) {
        this.isWantClientAuth = isWantClientAuth;
    }

    public boolean getWantClientAuth() {
        if (this._socket_ instanceof SSLSocket) {
            return ((SSLSocket)this._socket_).getWantClientAuth();
        }
        return false;
    }

    public void setUseClientMode(boolean isClientMode) {
        this.isClientMode = isClientMode;
    }

    public boolean getUseClientMode() {
        if (this._socket_ instanceof SSLSocket) {
            return ((SSLSocket)this._socket_).getUseClientMode();
        }
        return false;
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

    public void execPBSZ(long pbsz) throws SSLException, IOException {
        if (pbsz < 0L || 0xFFFFFFFFL < pbsz) {
            throw new IllegalArgumentException();
        }
        int status = this.sendCommand(CMD_PBSZ, String.valueOf(pbsz));
        if (200 != status) {
            throw new SSLException(this.getReplyString());
        }
    }

    public long parsePBSZ(long pbsz) throws SSLException, IOException {
        long replysz;
        this.execPBSZ(pbsz);
        long minvalue = pbsz;
        String remainder = this.extractPrefixedData("PBSZ=", this.getReplyString());
        if (remainder != null && (replysz = Long.parseLong(remainder)) < minvalue) {
            minvalue = replysz;
        }
        return minvalue;
    }

    public void execPROT(String prot) throws SSLException, IOException {
        if (prot == null) {
            prot = DEFAULT_PROT;
        }
        if (!this.checkPROTValue(prot)) {
            throw new IllegalArgumentException();
        }
        if (200 != this.sendCommand(CMD_PROT, prot)) {
            throw new SSLException(this.getReplyString());
        }
        if (DEFAULT_PROT.equals(prot)) {
            this.setSocketFactory(null);
            this.setServerSocketFactory(null);
        } else {
            this.setSocketFactory(new FTPSSocketFactory(this.context));
            this.setServerSocketFactory(new FTPSServerSocketFactory(this.context));
            this.initSslContext();
        }
    }

    private boolean checkPROTValue(String prot) {
        for (String element : PROT_COMMAND_VALUE) {
            if (!element.equals(prot)) continue;
            return true;
        }
        return false;
    }

    @Override
    public int sendCommand(String command, String args) throws IOException {
        int repCode = super.sendCommand(command, args);
        if (CMD_CCC.equals(command)) {
            if (200 == repCode) {
                this._socket_.close();
                this._socket_ = this.plainSocket;
                this._controlInput_ = new BufferedReader(new InputStreamReader(this._socket_.getInputStream(), this.getControlEncoding()));
                this._controlOutput_ = new BufferedWriter(new OutputStreamWriter(this._socket_.getOutputStream(), this.getControlEncoding()));
            } else {
                throw new SSLException(this.getReplyString());
            }
        }
        return repCode;
    }

    @Override
    @Deprecated
    protected Socket _openDataConnection_(int command, String arg) throws IOException {
        return this._openDataConnection_(FTPCommand.getCommand(command), arg);
    }

    @Override
    protected Socket _openDataConnection_(String command, String arg) throws IOException {
        Socket socket = super._openDataConnection_(command, arg);
        this._prepareDataSocket_(socket);
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket)socket;
            sslSocket.setUseClientMode(this.isClientMode);
            sslSocket.setEnableSessionCreation(this.isCreation);
            if (!this.isClientMode) {
                sslSocket.setNeedClientAuth(this.isNeedClientAuth);
                sslSocket.setWantClientAuth(this.isWantClientAuth);
            }
            if (this.suites != null) {
                sslSocket.setEnabledCipherSuites(this.suites);
            }
            if (this.protocols != null) {
                sslSocket.setEnabledProtocols(this.protocols);
            }
            sslSocket.startHandshake();
        }
        return socket;
    }

    protected void _prepareDataSocket_(Socket socket) throws IOException {
    }

    public TrustManager getTrustManager() {
        return this.trustManager;
    }

    public void setTrustManager(TrustManager trustManager) {
        this.trustManager = trustManager;
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

    @Override
    public void disconnect() throws IOException {
        super.disconnect();
        this.setSocketFactory(null);
        this.setServerSocketFactory(null);
    }

    public int execAUTH(String mechanism) throws IOException {
        return this.sendCommand(CMD_AUTH, mechanism);
    }

    public int execADAT(byte[] data) throws IOException {
        if (data != null) {
            return this.sendCommand(CMD_ADAT, Base64.encodeBase64StringUnChunked(data));
        }
        return this.sendCommand(CMD_ADAT);
    }

    public int execCCC() throws IOException {
        int repCode = this.sendCommand(CMD_CCC);
        return repCode;
    }

    public int execMIC(byte[] data) throws IOException {
        if (data != null) {
            return this.sendCommand(CMD_MIC, Base64.encodeBase64StringUnChunked(data));
        }
        return this.sendCommand(CMD_MIC, "");
    }

    public int execCONF(byte[] data) throws IOException {
        if (data != null) {
            return this.sendCommand(CMD_CONF, Base64.encodeBase64StringUnChunked(data));
        }
        return this.sendCommand(CMD_CONF, "");
    }

    public int execENC(byte[] data) throws IOException {
        if (data != null) {
            return this.sendCommand(CMD_ENC, Base64.encodeBase64StringUnChunked(data));
        }
        return this.sendCommand(CMD_ENC, "");
    }

    public byte[] parseADATReply(String reply) {
        if (reply == null) {
            return null;
        }
        return Base64.decodeBase64(this.extractPrefixedData("ADAT=", reply));
    }

    private String extractPrefixedData(String prefix, String reply) {
        int idx = reply.indexOf(prefix);
        if (idx == -1) {
            return null;
        }
        return reply.substring(idx + prefix.length()).trim();
    }
}


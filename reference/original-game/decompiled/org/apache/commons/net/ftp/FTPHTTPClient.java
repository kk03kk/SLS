/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.ftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Inet6Address;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.util.Base64;

public class FTPHTTPClient
extends FTPClient {
    private final String proxyHost;
    private final int proxyPort;
    private final String proxyUsername;
    private final String proxyPassword;
    private static final byte[] CRLF = new byte[]{13, 10};
    private final Base64 base64 = new Base64();
    private String tunnelHost;

    public FTPHTTPClient(String proxyHost, int proxyPort, String proxyUser, String proxyPass) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.proxyUsername = proxyUser;
        this.proxyPassword = proxyPass;
        this.tunnelHost = null;
    }

    public FTPHTTPClient(String proxyHost, int proxyPort) {
        this(proxyHost, proxyPort, null, null);
    }

    @Override
    @Deprecated
    protected Socket _openDataConnection_(int command, String arg) throws IOException {
        return super._openDataConnection_(command, arg);
    }

    @Override
    protected Socket _openDataConnection_(String command, String arg) throws IOException {
        boolean attemptEPSV;
        if (this.getDataConnectionMode() != 2) {
            throw new IllegalStateException("Only passive connection mode supported");
        }
        boolean isInet6Address = this.getRemoteAddress() instanceof Inet6Address;
        String passiveHost = null;
        boolean bl = attemptEPSV = this.isUseEPSVwithIPv4() || isInet6Address;
        if (attemptEPSV && this.epsv() == 229) {
            this._parseExtendedPassiveModeReply((String)this._replyLines.get(0));
            passiveHost = this.tunnelHost;
        } else {
            if (isInet6Address) {
                return null;
            }
            if (this.pasv() != 227) {
                return null;
            }
            this._parsePassiveModeReply((String)this._replyLines.get(0));
            passiveHost = this.getPassiveHost();
        }
        Socket socket = this._socketFactory_.createSocket(this.proxyHost, this.proxyPort);
        InputStream is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();
        this.tunnelHandshake(passiveHost, this.getPassivePort(), is, os);
        if (this.getRestartOffset() > 0L && !this.restart(this.getRestartOffset())) {
            socket.close();
            return null;
        }
        if (!FTPReply.isPositivePreliminary(this.sendCommand(command, arg))) {
            socket.close();
            return null;
        }
        return socket;
    }

    @Override
    public void connect(String host, int port) throws SocketException, IOException {
        BufferedReader socketIsReader;
        this._socket_ = this._socketFactory_.createSocket(this.proxyHost, this.proxyPort);
        this._input_ = this._socket_.getInputStream();
        this._output_ = this._socket_.getOutputStream();
        try {
            socketIsReader = this.tunnelHandshake(host, port, this._input_, this._output_);
        }
        catch (Exception e) {
            IOException ioe = new IOException("Could not connect to " + host + " using port " + port);
            ioe.initCause(e);
            throw ioe;
        }
        super._connectAction_(socketIsReader);
    }

    private BufferedReader tunnelHandshake(String host, int port, InputStream input, OutputStream output) throws IOException, UnsupportedEncodingException {
        String connectString = "CONNECT " + host + ":" + port + " HTTP/1.1";
        String hostString = "Host: " + host + ":" + port;
        this.tunnelHost = host;
        output.write(connectString.getBytes("UTF-8"));
        output.write(CRLF);
        output.write(hostString.getBytes("UTF-8"));
        output.write(CRLF);
        if (this.proxyUsername != null && this.proxyPassword != null) {
            String auth = this.proxyUsername + ":" + this.proxyPassword;
            String header = "Proxy-Authorization: Basic " + this.base64.encodeToString(auth.getBytes("UTF-8"));
            output.write(header.getBytes("UTF-8"));
        }
        output.write(CRLF);
        ArrayList<String> response = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, this.getCharset()));
        String line = reader.readLine();
        while (line != null && line.length() > 0) {
            response.add(line);
            line = reader.readLine();
        }
        int size = response.size();
        if (size == 0) {
            throw new IOException("No response from proxy");
        }
        String code = null;
        String resp = (String)response.get(0);
        if (!resp.startsWith("HTTP/") || resp.length() < 12) {
            throw new IOException("Invalid response from proxy: " + resp);
        }
        code = resp.substring(9, 12);
        if (!"200".equals(code)) {
            StringBuilder msg = new StringBuilder();
            msg.append("HTTPTunnelConnector: connection failed\r\n");
            msg.append("Response received from the proxy:\r\n");
            for (String line2 : response) {
                msg.append(line2);
                msg.append("\r\n");
            }
            throw new IOException(msg.toString());
        }
        return reader;
    }
}


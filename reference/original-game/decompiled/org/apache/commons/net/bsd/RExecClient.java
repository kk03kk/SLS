/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.bsd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.apache.commons.net.SocketClient;
import org.apache.commons.net.io.SocketInputStream;

public class RExecClient
extends SocketClient {
    protected static final char NULL_CHAR = '\u0000';
    public static final int DEFAULT_PORT = 512;
    private boolean __remoteVerificationEnabled;
    protected InputStream _errorStream_ = null;

    InputStream _createErrorStream() throws IOException {
        ServerSocket server = this._serverSocketFactory_.createServerSocket(0, 1, this.getLocalAddress());
        this._output_.write(Integer.toString(server.getLocalPort()).getBytes("UTF-8"));
        this._output_.write(0);
        this._output_.flush();
        Socket socket = server.accept();
        server.close();
        if (this.__remoteVerificationEnabled && !this.verifyRemote(socket)) {
            socket.close();
            throw new IOException("Security violation: unexpected connection attempt by " + socket.getInetAddress().getHostAddress());
        }
        return new SocketInputStream(socket, socket.getInputStream());
    }

    public RExecClient() {
        this.setDefaultPort(512);
    }

    public InputStream getInputStream() {
        return this._input_;
    }

    public OutputStream getOutputStream() {
        return this._output_;
    }

    public InputStream getErrorStream() {
        return this._errorStream_;
    }

    public void rexec(String username, String password, String command, boolean separateErrorStream) throws IOException {
        if (separateErrorStream) {
            this._errorStream_ = this._createErrorStream();
        } else {
            this._output_.write(0);
        }
        this._output_.write(username.getBytes(this.getCharset()));
        this._output_.write(0);
        this._output_.write(password.getBytes(this.getCharset()));
        this._output_.write(0);
        this._output_.write(command.getBytes(this.getCharset()));
        this._output_.write(0);
        this._output_.flush();
        int ch = this._input_.read();
        if (ch > 0) {
            StringBuilder buffer = new StringBuilder();
            while ((ch = this._input_.read()) != -1 && ch != 10) {
                buffer.append((char)ch);
            }
            throw new IOException(buffer.toString());
        }
        if (ch < 0) {
            throw new IOException("Server closed connection.");
        }
    }

    public void rexec(String username, String password, String command) throws IOException {
        this.rexec(username, password, command, false);
    }

    @Override
    public void disconnect() throws IOException {
        if (this._errorStream_ != null) {
            this._errorStream_.close();
        }
        this._errorStream_ = null;
        super.disconnect();
    }

    public final void setRemoteVerificationEnabled(boolean enable) {
        this.__remoteVerificationEnabled = enable;
    }

    public final boolean isRemoteVerificationEnabled() {
        return this.__remoteVerificationEnabled;
    }
}


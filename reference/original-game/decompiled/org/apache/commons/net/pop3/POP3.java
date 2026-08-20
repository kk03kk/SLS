/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.pop3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.net.MalformedServerReplyException;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ProtocolCommandSupport;
import org.apache.commons.net.SocketClient;
import org.apache.commons.net.io.CRLFLineReader;
import org.apache.commons.net.pop3.POP3Command;

public class POP3
extends SocketClient {
    public static final int DEFAULT_PORT = 110;
    public static final int DISCONNECTED_STATE = -1;
    public static final int AUTHORIZATION_STATE = 0;
    public static final int TRANSACTION_STATE = 1;
    public static final int UPDATE_STATE = 2;
    static final String _OK = "+OK";
    static final String _OK_INT = "+ ";
    static final String _ERROR = "-ERR";
    static final String _DEFAULT_ENCODING = "ISO-8859-1";
    private int __popState;
    BufferedWriter _writer;
    BufferedReader _reader;
    int _replyCode;
    String _lastReplyLine;
    List<String> _replyLines;
    protected ProtocolCommandSupport _commandSupport_;

    public POP3() {
        this.setDefaultPort(110);
        this.__popState = -1;
        this._reader = null;
        this._writer = null;
        this._replyLines = new ArrayList<String>();
        this._commandSupport_ = new ProtocolCommandSupport(this);
    }

    private void __getReply() throws IOException {
        this._replyLines.clear();
        String line = this._reader.readLine();
        if (line == null) {
            throw new EOFException("Connection closed without indication.");
        }
        if (line.startsWith(_OK)) {
            this._replyCode = 0;
        } else if (line.startsWith(_ERROR)) {
            this._replyCode = 1;
        } else if (line.startsWith(_OK_INT)) {
            this._replyCode = 2;
        } else {
            throw new MalformedServerReplyException("Received invalid POP3 protocol response from server." + line);
        }
        this._replyLines.add(line);
        this._lastReplyLine = line;
        this.fireReplyReceived(this._replyCode, this.getReplyString());
    }

    @Override
    protected void _connectAction_() throws IOException {
        super._connectAction_();
        this._reader = new CRLFLineReader(new InputStreamReader(this._input_, _DEFAULT_ENCODING));
        this._writer = new BufferedWriter(new OutputStreamWriter(this._output_, _DEFAULT_ENCODING));
        this.__getReply();
        this.setState(0);
    }

    public void setState(int state) {
        this.__popState = state;
    }

    public int getState() {
        return this.__popState;
    }

    public void getAdditionalReply() throws IOException {
        String line = this._reader.readLine();
        while (line != null) {
            this._replyLines.add(line);
            if (line.equals(".")) break;
            line = this._reader.readLine();
        }
    }

    @Override
    public void disconnect() throws IOException {
        super.disconnect();
        this._reader = null;
        this._writer = null;
        this._lastReplyLine = null;
        this._replyLines.clear();
        this.setState(-1);
    }

    public int sendCommand(String command, String args) throws IOException {
        if (this._writer == null) {
            throw new IllegalStateException("Socket is not connected");
        }
        StringBuilder __commandBuffer = new StringBuilder();
        __commandBuffer.append(command);
        if (args != null) {
            __commandBuffer.append(' ');
            __commandBuffer.append(args);
        }
        __commandBuffer.append("\r\n");
        String message = __commandBuffer.toString();
        this._writer.write(message);
        this._writer.flush();
        this.fireCommandSent(command, message);
        this.__getReply();
        return this._replyCode;
    }

    public int sendCommand(String command) throws IOException {
        return this.sendCommand(command, null);
    }

    public int sendCommand(int command, String args) throws IOException {
        return this.sendCommand(POP3Command._commands[command], args);
    }

    public int sendCommand(int command) throws IOException {
        return this.sendCommand(POP3Command._commands[command], null);
    }

    public String[] getReplyStrings() {
        return this._replyLines.toArray(new String[this._replyLines.size()]);
    }

    public String getReplyString() {
        StringBuilder buffer = new StringBuilder(256);
        for (String entry : this._replyLines) {
            buffer.append(entry);
            buffer.append("\r\n");
        }
        return buffer.toString();
    }

    public void removeProtocolCommandistener(ProtocolCommandListener listener) {
        this.removeProtocolCommandListener(listener);
    }

    @Override
    protected ProtocolCommandSupport getCommandSupport() {
        return this._commandSupport_;
    }
}


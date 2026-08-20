/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.imap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.net.SocketClient;
import org.apache.commons.net.imap.IMAPCommand;
import org.apache.commons.net.imap.IMAPReply;
import org.apache.commons.net.io.CRLFLineReader;

public class IMAP
extends SocketClient {
    public static final int DEFAULT_PORT = 143;
    protected static final String __DEFAULT_ENCODING = "ISO-8859-1";
    private IMAPState __state;
    protected BufferedWriter __writer;
    protected BufferedReader _reader;
    private int _replyCode;
    private final List<String> _replyLines;
    public static final IMAPChunkListener TRUE_CHUNK_LISTENER = new IMAPChunkListener(){

        @Override
        public boolean chunkReceived(IMAP imap) {
            return true;
        }
    };
    private volatile IMAPChunkListener __chunkListener;
    private final char[] _initialID = new char[]{'A', 'A', 'A', 'A'};

    public IMAP() {
        this.setDefaultPort(143);
        this.__state = IMAPState.DISCONNECTED_STATE;
        this._reader = null;
        this.__writer = null;
        this._replyLines = new ArrayList<String>();
        this.createCommandSupport();
    }

    private void __getReply() throws IOException {
        this.__getReply(true);
    }

    private void __getReply(boolean wantTag) throws IOException {
        this._replyLines.clear();
        String line = this._reader.readLine();
        if (line == null) {
            throw new EOFException("Connection closed without indication.");
        }
        this._replyLines.add(line);
        if (wantTag) {
            while (IMAPReply.isUntagged(line)) {
                boolean clear;
                IMAPChunkListener il;
                boolean isMultiLine;
                int literalCount = IMAPReply.literalCount(line);
                boolean bl = isMultiLine = literalCount >= 0;
                while (literalCount >= 0) {
                    line = this._reader.readLine();
                    if (line == null) {
                        throw new EOFException("Connection closed without indication.");
                    }
                    this._replyLines.add(line);
                    literalCount -= line.length() + 2;
                }
                if (isMultiLine && (il = this.__chunkListener) != null && (clear = il.chunkReceived(this))) {
                    this.fireReplyReceived(3, this.getReplyString());
                    this._replyLines.clear();
                }
                if ((line = this._reader.readLine()) == null) {
                    throw new EOFException("Connection closed without indication.");
                }
                this._replyLines.add(line);
            }
            this._replyCode = IMAPReply.getReplyCode(line);
        } else {
            this._replyCode = IMAPReply.getUntaggedReplyCode(line);
        }
        this.fireReplyReceived(this._replyCode, this.getReplyString());
    }

    @Override
    protected void fireReplyReceived(int replyCode, String ignored) {
        if (this.getCommandSupport().getListenerCount() > 0) {
            this.getCommandSupport().fireReplyReceived(replyCode, this.getReplyString());
        }
    }

    @Override
    protected void _connectAction_() throws IOException {
        super._connectAction_();
        this._reader = new CRLFLineReader(new InputStreamReader(this._input_, __DEFAULT_ENCODING));
        this.__writer = new BufferedWriter(new OutputStreamWriter(this._output_, __DEFAULT_ENCODING));
        int tmo = this.getSoTimeout();
        if (tmo <= 0) {
            this.setSoTimeout(this.connectTimeout);
        }
        this.__getReply(false);
        if (tmo <= 0) {
            this.setSoTimeout(tmo);
        }
        this.setState(IMAPState.NOT_AUTH_STATE);
    }

    protected void setState(IMAPState state) {
        this.__state = state;
    }

    public IMAPState getState() {
        return this.__state;
    }

    @Override
    public void disconnect() throws IOException {
        super.disconnect();
        this._reader = null;
        this.__writer = null;
        this._replyLines.clear();
        this.setState(IMAPState.DISCONNECTED_STATE);
    }

    private int sendCommandWithID(String commandID, String command, String args) throws IOException {
        StringBuilder __commandBuffer = new StringBuilder();
        if (commandID != null) {
            __commandBuffer.append(commandID);
            __commandBuffer.append(' ');
        }
        __commandBuffer.append(command);
        if (args != null) {
            __commandBuffer.append(' ');
            __commandBuffer.append(args);
        }
        __commandBuffer.append("\r\n");
        String message = __commandBuffer.toString();
        this.__writer.write(message);
        this.__writer.flush();
        this.fireCommandSent(command, message);
        this.__getReply();
        return this._replyCode;
    }

    public int sendCommand(String command, String args) throws IOException {
        return this.sendCommandWithID(this.generateCommandID(), command, args);
    }

    public int sendCommand(String command) throws IOException {
        return this.sendCommand(command, null);
    }

    public int sendCommand(IMAPCommand command, String args) throws IOException {
        return this.sendCommand(command.getIMAPCommand(), args);
    }

    public boolean doCommand(IMAPCommand command, String args) throws IOException {
        return IMAPReply.isSuccess(this.sendCommand(command, args));
    }

    public int sendCommand(IMAPCommand command) throws IOException {
        return this.sendCommand(command, null);
    }

    public boolean doCommand(IMAPCommand command) throws IOException {
        return IMAPReply.isSuccess(this.sendCommand(command));
    }

    public int sendData(String command) throws IOException {
        return this.sendCommandWithID(null, command, null);
    }

    public String[] getReplyStrings() {
        return this._replyLines.toArray(new String[this._replyLines.size()]);
    }

    public String getReplyString() {
        StringBuilder buffer = new StringBuilder(256);
        for (String s : this._replyLines) {
            buffer.append(s);
            buffer.append("\r\n");
        }
        return buffer.toString();
    }

    public void setChunkListener(IMAPChunkListener listener) {
        this.__chunkListener = listener;
    }

    protected String generateCommandID() {
        String res = new String(this._initialID);
        boolean carry = true;
        for (int i = this._initialID.length - 1; carry && i >= 0; --i) {
            if (this._initialID[i] == 'Z') {
                this._initialID[i] = 65;
                continue;
            }
            int n = i;
            this._initialID[n] = (char)(this._initialID[n] + '\u0001');
            carry = false;
        }
        return res;
    }

    public static interface IMAPChunkListener {
        public boolean chunkReceived(IMAP var1);
    }

    public static enum IMAPState {
        DISCONNECTED_STATE,
        NOT_AUTH_STATE,
        AUTH_STATE,
        LOGOUT_STATE;

    }
}


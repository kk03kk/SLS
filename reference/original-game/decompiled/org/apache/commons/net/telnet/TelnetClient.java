/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.telnet;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.Telnet;
import org.apache.commons.net.telnet.TelnetInputListener;
import org.apache.commons.net.telnet.TelnetInputStream;
import org.apache.commons.net.telnet.TelnetNotificationHandler;
import org.apache.commons.net.telnet.TelnetOptionHandler;
import org.apache.commons.net.telnet.TelnetOutputStream;

public class TelnetClient
extends Telnet {
    private InputStream __input = null;
    private OutputStream __output = null;
    protected boolean readerThread = true;
    private TelnetInputListener inputListener;

    public TelnetClient() {
        super("VT100");
    }

    public TelnetClient(String termtype) {
        super(termtype);
    }

    void _flushOutputStream() throws IOException {
        this._output_.flush();
    }

    void _closeOutputStream() throws IOException {
        this._output_.close();
    }

    @Override
    protected void _connectAction_() throws IOException {
        super._connectAction_();
        TelnetInputStream tmp = new TelnetInputStream(this._input_, this, this.readerThread);
        if (this.readerThread) {
            tmp._start();
        }
        this.__input = new BufferedInputStream(tmp);
        this.__output = new TelnetOutputStream(this);
    }

    @Override
    public void disconnect() throws IOException {
        if (this.__input != null) {
            this.__input.close();
        }
        if (this.__output != null) {
            this.__output.close();
        }
        super.disconnect();
    }

    public OutputStream getOutputStream() {
        return this.__output;
    }

    public InputStream getInputStream() {
        return this.__input;
    }

    public boolean getLocalOptionState(int option) {
        return this._stateIsWill(option) && this._requestedWill(option);
    }

    public boolean getRemoteOptionState(int option) {
        return this._stateIsDo(option) && this._requestedDo(option);
    }

    public boolean sendAYT(long timeout) throws IOException, IllegalArgumentException, InterruptedException {
        return this._sendAYT(timeout);
    }

    public void sendSubnegotiation(int[] message) throws IOException, IllegalArgumentException {
        if (message.length < 1) {
            throw new IllegalArgumentException("zero length message");
        }
        this._sendSubnegotiation(message);
    }

    public void sendCommand(byte command) throws IOException, IllegalArgumentException {
        this._sendCommand(command);
    }

    @Override
    public void addOptionHandler(TelnetOptionHandler opthand) throws InvalidTelnetOptionException, IOException {
        super.addOptionHandler(opthand);
    }

    @Override
    public void deleteOptionHandler(int optcode) throws InvalidTelnetOptionException, IOException {
        super.deleteOptionHandler(optcode);
    }

    public void registerSpyStream(OutputStream spystream) {
        super._registerSpyStream(spystream);
    }

    public void stopSpyStream() {
        super._stopSpyStream();
    }

    @Override
    public void registerNotifHandler(TelnetNotificationHandler notifhand) {
        super.registerNotifHandler(notifhand);
    }

    @Override
    public void unregisterNotifHandler() {
        super.unregisterNotifHandler();
    }

    public void setReaderThread(boolean flag) {
        this.readerThread = flag;
    }

    public boolean getReaderThread() {
        return this.readerThread;
    }

    public synchronized void registerInputListener(TelnetInputListener listener) {
        this.inputListener = listener;
    }

    public synchronized void unregisterInputListener() {
        this.inputListener = null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void notifyInputListener() {
        TelnetInputListener listener;
        TelnetClient telnetClient = this;
        synchronized (telnetClient) {
            listener = this.inputListener;
        }
        if (listener != null) {
            listener.telnetInputAvailable();
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.telnet;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.net.telnet.TelnetClient;

final class TelnetOutputStream
extends OutputStream {
    private final TelnetClient __client;
    private final boolean __convertCRtoCRLF = true;
    private boolean __lastWasCR = false;

    TelnetOutputStream(TelnetClient client) {
        this.__client = client;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void write(int ch) throws IOException {
        TelnetClient telnetClient = this.__client;
        synchronized (telnetClient) {
            ch &= 0xFF;
            if (this.__client._requestedWont(0)) {
                if (this.__lastWasCR) {
                    this.__client._sendByte(10);
                    if (ch == 10) {
                        this.__lastWasCR = false;
                        return;
                    }
                }
                switch (ch) {
                    case 13: {
                        this.__client._sendByte(13);
                        this.__lastWasCR = true;
                        break;
                    }
                    case 10: {
                        if (!this.__lastWasCR) {
                            this.__client._sendByte(13);
                        }
                        this.__client._sendByte(ch);
                        this.__lastWasCR = false;
                        break;
                    }
                    case 255: {
                        this.__client._sendByte(255);
                        this.__client._sendByte(255);
                        this.__lastWasCR = false;
                        break;
                    }
                    default: {
                        this.__client._sendByte(ch);
                        this.__lastWasCR = false;
                        break;
                    }
                }
            } else if (ch == 255) {
                this.__client._sendByte(ch);
                this.__client._sendByte(255);
            } else {
                this.__client._sendByte(ch);
            }
        }
    }

    @Override
    public void write(byte[] buffer) throws IOException {
        this.write(buffer, 0, buffer.length);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        TelnetClient telnetClient = this.__client;
        synchronized (telnetClient) {
            while (length-- > 0) {
                this.write(buffer[offset++]);
            }
        }
    }

    @Override
    public void flush() throws IOException {
        this.__client._flushOutputStream();
    }

    @Override
    public void close() throws IOException {
        this.__client._closeOutputStream();
    }
}


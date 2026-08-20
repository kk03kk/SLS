/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk;

import java.io.IOException;
import java.net.Socket;

public interface SocketFactory {
    public Socket createSocket() throws IOException;
}


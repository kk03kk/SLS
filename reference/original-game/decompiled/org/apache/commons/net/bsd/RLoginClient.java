/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.bsd;

import java.io.IOException;
import org.apache.commons.net.bsd.RCommandClient;

public class RLoginClient
extends RCommandClient {
    public static final int DEFAULT_PORT = 513;

    public RLoginClient() {
        this.setDefaultPort(513);
    }

    public void rlogin(String localUsername, String remoteUsername, String terminalType, int terminalSpeed) throws IOException {
        this.rexec(localUsername, remoteUsername, terminalType + "/" + terminalSpeed, false);
    }

    public void rlogin(String localUsername, String remoteUsername, String terminalType) throws IOException {
        this.rexec(localUsername, remoteUsername, terminalType, false);
    }
}


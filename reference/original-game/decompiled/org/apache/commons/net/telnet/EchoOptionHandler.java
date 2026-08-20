/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.telnet;

import org.apache.commons.net.telnet.TelnetOptionHandler;

public class EchoOptionHandler
extends TelnetOptionHandler {
    public EchoOptionHandler(boolean initlocal, boolean initremote, boolean acceptlocal, boolean acceptremote) {
        super(1, initlocal, initremote, acceptlocal, acceptremote);
    }

    public EchoOptionHandler() {
        super(1, false, false, false, false);
    }
}


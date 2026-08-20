/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.telnet;

import org.apache.commons.net.telnet.TelnetOptionHandler;

public class SuppressGAOptionHandler
extends TelnetOptionHandler {
    public SuppressGAOptionHandler(boolean initlocal, boolean initremote, boolean acceptlocal, boolean acceptremote) {
        super(3, initlocal, initremote, acceptlocal, acceptremote);
    }

    public SuppressGAOptionHandler() {
        super(3, false, false, false, false);
    }
}


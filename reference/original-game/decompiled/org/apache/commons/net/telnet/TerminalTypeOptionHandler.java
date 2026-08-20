/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.telnet;

import org.apache.commons.net.telnet.TelnetOptionHandler;

public class TerminalTypeOptionHandler
extends TelnetOptionHandler {
    private final String termType;
    protected static final int TERMINAL_TYPE = 24;
    protected static final int TERMINAL_TYPE_SEND = 1;
    protected static final int TERMINAL_TYPE_IS = 0;

    public TerminalTypeOptionHandler(String termtype, boolean initlocal, boolean initremote, boolean acceptlocal, boolean acceptremote) {
        super(24, initlocal, initremote, acceptlocal, acceptremote);
        this.termType = termtype;
    }

    public TerminalTypeOptionHandler(String termtype) {
        super(24, false, false, false, false);
        this.termType = termtype;
    }

    @Override
    public int[] answerSubnegotiation(int[] suboptionData, int suboptionLength) {
        if (suboptionData != null && suboptionLength > 1 && this.termType != null && suboptionData[0] == 24 && suboptionData[1] == 1) {
            int[] response = new int[this.termType.length() + 2];
            response[0] = 24;
            response[1] = 0;
            for (int ii = 0; ii < this.termType.length(); ++ii) {
                response[ii + 2] = this.termType.charAt(ii);
            }
            return response;
        }
        return null;
    }
}


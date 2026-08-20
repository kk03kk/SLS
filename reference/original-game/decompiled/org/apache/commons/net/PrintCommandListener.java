/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net;

import java.io.PrintStream;
import java.io.PrintWriter;
import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;

public class PrintCommandListener
implements ProtocolCommandListener {
    private final PrintWriter __writer;
    private final boolean __nologin;
    private final char __eolMarker;
    private final boolean __directionMarker;

    public PrintCommandListener(PrintStream stream) {
        this(new PrintWriter(stream));
    }

    public PrintCommandListener(PrintStream stream, boolean suppressLogin) {
        this(new PrintWriter(stream), suppressLogin);
    }

    public PrintCommandListener(PrintStream stream, boolean suppressLogin, char eolMarker) {
        this(new PrintWriter(stream), suppressLogin, eolMarker);
    }

    public PrintCommandListener(PrintStream stream, boolean suppressLogin, char eolMarker, boolean showDirection) {
        this(new PrintWriter(stream), suppressLogin, eolMarker, showDirection);
    }

    public PrintCommandListener(PrintWriter writer) {
        this(writer, false);
    }

    public PrintCommandListener(PrintWriter writer, boolean suppressLogin) {
        this(writer, suppressLogin, '\u0000');
    }

    public PrintCommandListener(PrintWriter writer, boolean suppressLogin, char eolMarker) {
        this(writer, suppressLogin, eolMarker, false);
    }

    public PrintCommandListener(PrintWriter writer, boolean suppressLogin, char eolMarker, boolean showDirection) {
        this.__writer = writer;
        this.__nologin = suppressLogin;
        this.__eolMarker = eolMarker;
        this.__directionMarker = showDirection;
    }

    @Override
    public void protocolCommandSent(ProtocolCommandEvent event) {
        if (this.__directionMarker) {
            this.__writer.print("> ");
        }
        if (this.__nologin) {
            String cmd = event.getCommand();
            if ("PASS".equalsIgnoreCase(cmd) || "USER".equalsIgnoreCase(cmd)) {
                this.__writer.print(cmd);
                this.__writer.println(" *******");
            } else {
                String IMAP_LOGIN = "LOGIN";
                if ("LOGIN".equalsIgnoreCase(cmd)) {
                    String msg = event.getMessage();
                    msg = msg.substring(0, msg.indexOf("LOGIN") + "LOGIN".length());
                    this.__writer.print(msg);
                    this.__writer.println(" *******");
                } else {
                    this.__writer.print(this.getPrintableString(event.getMessage()));
                }
            }
        } else {
            this.__writer.print(this.getPrintableString(event.getMessage()));
        }
        this.__writer.flush();
    }

    private String getPrintableString(String msg) {
        if (this.__eolMarker == '\u0000') {
            return msg;
        }
        int pos = msg.indexOf("\r\n");
        if (pos > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(msg.substring(0, pos));
            sb.append(this.__eolMarker);
            sb.append(msg.substring(pos));
            return sb.toString();
        }
        return msg;
    }

    @Override
    public void protocolReplyReceived(ProtocolCommandEvent event) {
        if (this.__directionMarker) {
            this.__writer.print("< ");
        }
        this.__writer.print(event.getMessage());
        this.__writer.flush();
    }
}


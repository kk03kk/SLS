/*
 * Decompiled with CFR 0.152.
 */
package org.apache.commons.net.smtp;

import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import org.apache.commons.net.io.DotTerminatedMessageWriter;
import org.apache.commons.net.smtp.RelayPath;
import org.apache.commons.net.smtp.SMTP;
import org.apache.commons.net.smtp.SMTPReply;

public class SMTPClient
extends SMTP {
    public SMTPClient() {
    }

    public SMTPClient(String encoding) {
        super(encoding);
    }

    public boolean completePendingCommand() throws IOException {
        return SMTPReply.isPositiveCompletion(this.getReply());
    }

    public boolean login(String hostname) throws IOException {
        return SMTPReply.isPositiveCompletion(this.helo(hostname));
    }

    public boolean login() throws IOException {
        InetAddress host = this.getLocalAddress();
        String name = host.getHostName();
        if (name == null) {
            return false;
        }
        return SMTPReply.isPositiveCompletion(this.helo(name));
    }

    public boolean setSender(RelayPath path) throws IOException {
        return SMTPReply.isPositiveCompletion(this.mail(path.toString()));
    }

    public boolean setSender(String address) throws IOException {
        return SMTPReply.isPositiveCompletion(this.mail("<" + address + ">"));
    }

    public boolean addRecipient(RelayPath path) throws IOException {
        return SMTPReply.isPositiveCompletion(this.rcpt(path.toString()));
    }

    public boolean addRecipient(String address) throws IOException {
        return SMTPReply.isPositiveCompletion(this.rcpt("<" + address + ">"));
    }

    public Writer sendMessageData() throws IOException {
        if (!SMTPReply.isPositiveIntermediate(this.data())) {
            return null;
        }
        return new DotTerminatedMessageWriter(this._writer);
    }

    public boolean sendShortMessageData(String message) throws IOException {
        Writer writer = this.sendMessageData();
        if (writer == null) {
            return false;
        }
        writer.write(message);
        writer.close();
        return this.completePendingCommand();
    }

    public boolean sendSimpleMessage(String sender, String recipient, String message) throws IOException {
        if (!this.setSender(sender)) {
            return false;
        }
        if (!this.addRecipient(recipient)) {
            return false;
        }
        return this.sendShortMessageData(message);
    }

    public boolean sendSimpleMessage(String sender, String[] recipients, String message) throws IOException {
        boolean oneSuccess = false;
        if (!this.setSender(sender)) {
            return false;
        }
        for (int count = 0; count < recipients.length; ++count) {
            if (!this.addRecipient(recipients[count])) continue;
            oneSuccess = true;
        }
        if (!oneSuccess) {
            return false;
        }
        return this.sendShortMessageData(message);
    }

    public boolean logout() throws IOException {
        return SMTPReply.isPositiveCompletion(this.quit());
    }

    public boolean reset() throws IOException {
        return SMTPReply.isPositiveCompletion(this.rset());
    }

    public boolean verify(String username) throws IOException {
        int result = this.vrfy(username);
        return result == 250 || result == 251;
    }

    public String listHelp() throws IOException {
        if (SMTPReply.isPositiveCompletion(this.help())) {
            return this.getReplyString();
        }
        return null;
    }

    public String listHelp(String command) throws IOException {
        if (SMTPReply.isPositiveCompletion(this.help(command))) {
            return this.getReplyString();
        }
        return null;
    }

    public boolean sendNoOp() throws IOException {
        return SMTPReply.isPositiveCompletion(this.noop());
    }
}


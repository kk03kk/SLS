/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk;

import com.gikk.twirk.Twirk;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.SocketTimeoutException;

class InputThread
extends Thread {
    private final Twirk connection;
    private final BufferedReader reader;
    private boolean isConnected = true;
    private boolean havePinged = false;

    public InputThread(Twirk connection, BufferedReader reader, BufferedWriter writer) {
        this.connection = connection;
        this.reader = reader;
        this.setName("Twirk-InputThread");
    }

    @Override
    public void run() {
        try {
            while (this.isConnected) {
                try {
                    String line = null;
                    while ((line = this.reader.readLine()) != null) {
                        this.havePinged = false;
                        try {
                            this.connection.incommingMessage(line);
                        }
                        catch (Exception e) {
                            System.err.println("Error in handling the incomming Irc Message");
                            e.printStackTrace();
                        }
                    }
                    this.isConnected = false;
                }
                catch (SocketTimeoutException e) {
                    if (!this.havePinged) {
                        this.connection.serverMessage("PING " + System.currentTimeMillis());
                        this.havePinged = true;
                        continue;
                    }
                    this.isConnected = false;
                }
                catch (IOException e) {
                    String message = e.getMessage();
                    if (!message.contains("Socket Closed")) {
                        if (message.contains("Connection reset") || message.contains("Stream closed")) {
                            System.err.println(message);
                        } else {
                            e.printStackTrace();
                        }
                    }
                    this.isConnected = false;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        if (this.connection.isConnected()) {
            this.connection.disconnect();
        }
    }

    public void end() {
        this.isConnected = false;
    }
}


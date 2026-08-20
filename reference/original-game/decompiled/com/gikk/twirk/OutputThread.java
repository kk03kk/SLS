/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk;

import com.gikk.twirk.OutputQueue;
import com.gikk.twirk.Twirk;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.SocketException;

class OutputThread
extends Thread {
    private final Twirk connection;
    private final BufferedWriter writer;
    private final OutputQueue queue;
    private boolean isConnected = true;
    private int messageGapMillis = 1500;

    public OutputThread(Twirk connection, OutputQueue queue, BufferedReader reader, BufferedWriter writer) {
        this.connection = connection;
        this.queue = queue;
        this.writer = writer;
        this.setName("Twirk-OutputThread");
    }

    @Override
    public void run() {
        while (this.isConnected) {
            try {
                String line = this.queue.next();
                if (line != null) {
                    this.sendLine(line);
                } else {
                    this.isConnected = this.connection.isConnected();
                }
                Thread.sleep(this.messageGapMillis);
            }
            catch (Exception exception) {}
        }
    }

    public void quickSend(String message) {
        try {
            this.sendLine(message);
        }
        catch (SocketException e) {
            System.err.println("Could not QuickSend message. Socket was closed (OutputThread @ Twirk)");
        }
    }

    public void end() {
        this.isConnected = false;
        this.queue.releaseWaitingThreads();
    }

    void setMessageDelay(int millis) {
        this.messageGapMillis = millis;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void sendLine(String message) throws SocketException {
        if (!this.isConnected) {
            System.err.println("Twirk is not connected! Sending messages will not succeed!");
        }
        if (this.connection.verboseMode) {
            System.out.println("OUT " + message);
        }
        if (message.length() > 510) {
            message = message.substring(0, 511);
        }
        try {
            BufferedWriter bufferedWriter = this.writer;
            synchronized (bufferedWriter) {
                this.writer.write(message + "\r\n");
                this.writer.flush();
            }
        }
        catch (IOException e) {
            if (e.getMessage().matches("Stream closed")) {
                System.err.println("Cannot send message: " + message + " Stream closed");
                return;
            }
            e.printStackTrace();
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package de.robojumper.ststwitch;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import de.robojumper.ststwitch.TwitchConfig;
import de.robojumper.ststwitch.TwitchMessageListener;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TwitchConnection
extends Thread {
    private final Twirk twirk;
    private static final Logger logger = LogManager.getLogger(TwitchConnection.class.getName());
    private volatile boolean wantsConnection = false;
    private volatile boolean shuttingDown = false;
    private int connectionAttempts = 0;
    private List<TwitchMessageListener> listeners = new ArrayList<TwitchMessageListener>();
    private final TwitchConfig twitchConfig;
    private Queue<String> outMessages = new ArrayDeque<String>();
    private Queue<MessageUserPair> inMessages = new ArrayDeque<MessageUserPair>();
    private boolean hasPulse;
    private final Object gameLock = new Object();
    private static final Pattern QUOTE_PATTERN = Pattern.compile("(?i)\"(.*)\"(?:.*?)-?- ?joinrbs(?:[ \\.,].*)?");

    public TwitchConfig getTwitchConfig() {
        return this.twitchConfig;
    }

    public TwitchConnection(TwitchConfig twitchConfig) throws IOException {
        this.twitchConfig = twitchConfig;
        logger.info("Connecting: username=" + twitchConfig.getUsername() + ", token(truncated)=" + TwitchConnection.truncateToken(twitchConfig.getToken()) + ", channel=" + twitchConfig.getChannel());
        TwirkBuilder tb = new TwirkBuilder(twitchConfig.getChannel(), twitchConfig.getUsername(), twitchConfig.getToken());
        this.twirk = tb.setSSL(true).build();
        this.twirk.addIrcListener(TwitchConnection.getMessageListener(this.twirk, this.gameLock, this));
    }

    private static String truncateToken(String token) {
        if (token.length() > 10) {
            return token.substring(0, 10);
        }
        return token;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void run() {
        while (!this.shuttingDown) {
            if (this.wantsConnection && !this.twirk.isConnected()) {
                this.attemptConnect();
            } else if (!this.wantsConnection && this.twirk.isConnected()) {
                this.twirk.disconnect();
            }
            if (this.twirk.isConnected()) {
                Object object = this.gameLock;
                synchronized (object) {
                    String message;
                    while ((message = this.outMessages.poll()) != null) {
                        this.twirk.channelMessage(message);
                    }
                }
            }
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException interruptedException) {}
        }
        this.twirk.close();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void update() {
        Object object = this.gameLock;
        synchronized (object) {
            MessageUserPair message;
            while ((message = this.inMessages.poll()) != null) {
                for (TwitchMessageListener l : this.listeners) {
                    l.onMessage(message.message, message.user);
                }
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void registerMessageListener(TwitchMessageListener l) {
        Object object = this.gameLock;
        synchronized (object) {
            this.listeners.add(l);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void setStatus(boolean shouldBeRunning) {
        Twirk twirk = this.twirk;
        synchronized (twirk) {
            this.wantsConnection = shouldBeRunning;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void toggleStatus() {
        Twirk twirk = this.twirk;
        synchronized (twirk) {
            this.setStatus(!this.wantsConnection);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void sendMessage(String msg) {
        Object object = this.gameLock;
        synchronized (object) {
            this.outMessages.add(msg);
            this.interrupt();
        }
    }

    ConnectionStatus getConnectionStatus() {
        if (this.wantsConnection) {
            if (this.twirk.isConnected()) {
                return ConnectionStatus.CONNECTED;
            }
            return ConnectionStatus.CONNECTING;
        }
        if (this.twirk.isConnected()) {
            return ConnectionStatus.DISCONNECTING;
        }
        return ConnectionStatus.DISCONNECTED;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    boolean popPulse() {
        Object object = this.gameLock;
        synchronized (object) {
            if (this.hasPulse) {
                this.hasPulse = false;
                return true;
            }
            return this.hasPulse;
        }
    }

    public void dispose() {
        this.shuttingDown = true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void attemptConnect() {
        block6: {
            if (!this.twirk.isConnected() && !this.shuttingDown) {
                try {
                    this.twirk.connect();
                }
                catch (IOException | InterruptedException e) {
                    ++this.connectionAttempts;
                    if (this.connectionAttempts <= 3) break block6;
                    this.connectionAttempts = 0;
                    Twirk twirk = this.twirk;
                    synchronized (twirk) {
                        this.wantsConnection = false;
                    }
                }
            }
        }
    }

    private static TwirkListener getMessageListener(Twirk twirk, final Object gameLock, final TwitchConnection conn) {
        return new TwirkListener(){

            @Override
            public void onDisconnect() {
            }

            /*
             * WARNING - Removed try catching itself - possible behaviour change.
             */
            @Override
            public void onPrivMsg(TwitchUser sender, TwitchMessage message) {
                String content = message.getContent().trim();
                String senderName = sender.getUserName();
                if (content.length() > 1) {
                    Object object = gameLock;
                    synchronized (object) {
                        Matcher m = QUOTE_PATTERN.matcher(content);
                        if (content.equals("#pulse")) {
                            conn.hasPulse = true;
                        }
                        conn.inMessages.add(new MessageUserPair(content, senderName));
                    }
                }
            }
        };
    }

    private static class MessageUserPair {
        final String message;
        final String user;

        MessageUserPair(String m, String u) {
            this.message = m;
            this.user = u;
        }
    }

    public static enum ConnectionStatus {
        CONNECTED,
        CONNECTING,
        DISCONNECTING,
        DISCONNECTED;

    }
}


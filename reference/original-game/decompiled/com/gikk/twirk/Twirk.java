/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk;

import com.gikk.twirk.InputThread;
import com.gikk.twirk.OutputQueue;
import com.gikk.twirk.OutputThread;
import com.gikk.twirk.TwirkBuilder;
import com.gikk.twirk.TwirkMaintainanceListener;
import com.gikk.twirk.events.TwirkListener;
import com.gikk.twirk.types.clearChat.ClearChat;
import com.gikk.twirk.types.clearChat.ClearChatBuilder;
import com.gikk.twirk.types.hostTarget.HostTarget;
import com.gikk.twirk.types.hostTarget.HostTargetBuilder;
import com.gikk.twirk.types.mode.Mode;
import com.gikk.twirk.types.mode.ModeBuilder;
import com.gikk.twirk.types.notice.Notice;
import com.gikk.twirk.types.notice.NoticeBuilder;
import com.gikk.twirk.types.reconnect.ReconnectBuilder;
import com.gikk.twirk.types.roomstate.Roomstate;
import com.gikk.twirk.types.roomstate.RoomstateBuilder;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.twitchMessage.TwitchMessageBuilder;
import com.gikk.twirk.types.usernotice.Usernotice;
import com.gikk.twirk.types.usernotice.UsernoticeBuilder;
import com.gikk.twirk.types.users.TwitchUser;
import com.gikk.twirk.types.users.TwitchUserBuilder;
import com.gikk.twirk.types.users.Userstate;
import com.gikk.twirk.types.users.UserstateBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Twirk {
    private final String nick;
    private final String pass;
    private final String channel;
    final boolean verboseMode;
    private OutputThread outThread;
    private InputThread inThread;
    private final OutputQueue queue;
    private boolean resourcesCreated = false;
    private boolean isConnected = false;
    private boolean isDisposed = false;
    private BufferedWriter writer = null;
    private BufferedReader reader = null;
    private final ArrayList<TwirkListener> listeners = new ArrayList();
    final Set<String> moderators = Collections.newSetFromMap(new ConcurrentHashMap());
    final Set<String> online = Collections.newSetFromMap(new ConcurrentHashMap());
    private final ClearChatBuilder clearChatBuilder;
    private final HostTargetBuilder hostTargetBuilder;
    private final ModeBuilder modeBuilder;
    private final NoticeBuilder noticeBuilder;
    private final RoomstateBuilder roomstateBuilder;
    private final TwitchMessageBuilder twitchMessageBuilder;
    private final TwitchUserBuilder twitchUserBuilder;
    private final UserstateBuilder userstateBuilder;
    private final UsernoticeBuilder usernoticeBuilder;
    private final ReconnectBuilder reconnectBuilder;
    private final Socket socket;

    Twirk(TwirkBuilder builder) {
        this.nick = builder.nick;
        this.pass = builder.oauth;
        this.channel = builder.channel;
        this.verboseMode = builder.verboseMode;
        this.clearChatBuilder = builder.getClearChatBuilder();
        this.hostTargetBuilder = builder.getHostTargetBuilder();
        this.modeBuilder = builder.getModeBuilder();
        this.noticeBuilder = builder.getNoticeBuilder();
        this.roomstateBuilder = builder.getRoomstateBuilder();
        this.twitchUserBuilder = builder.getTwitchUserBuilder();
        this.userstateBuilder = builder.getUserstateBuilder();
        this.twitchMessageBuilder = builder.getTwitchMessageBuilder();
        this.usernoticeBuilder = builder.getUsernoticeBuilder();
        this.reconnectBuilder = builder.getReconnectBuilder();
        this.socket = builder.getSocket();
        this.queue = new OutputQueue();
        this.addIrcListener(new TwirkMaintainanceListener(this));
    }

    public void serverMessage(String message) {
        this.outThread.quickSend(message);
    }

    public void whisper(TwitchUser receiver, String message) {
        this.whisper(receiver.getUserName(), message);
    }

    public void whisper(String userName, String message) {
        this.queue.add("PRIVMSG " + this.channel + " :/w " + userName + " " + message);
    }

    public void channelMessage(String message) {
        this.queue.add("PRIVMSG " + this.channel + " :" + message);
    }

    public void priorityChannelMessage(String message) {
        this.queue.addFirst("PRIVMSG " + this.channel + " :" + message);
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public boolean isDisposed() {
        return this.isDisposed;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Set<String> getUsersOnline() {
        HashSet<String> out = new HashSet<String>();
        Set<String> set = this.online;
        synchronized (set) {
            for (String s : this.online) {
                out.add(s);
            }
        }
        return out;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Set<String> getModsOnline() {
        HashSet<String> out = new HashSet<String>();
        Set<String> set = this.moderators;
        synchronized (set) {
            for (String s : this.moderators) {
                out.add(s);
            }
        }
        return out;
    }

    public String getNick() {
        return this.nick;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void addIrcListener(TwirkListener listener) {
        ArrayList<TwirkListener> arrayList = this.listeners;
        synchronized (arrayList) {
            this.listeners.add(listener);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean removeIrcListener(TwirkListener listener) {
        ArrayList<TwirkListener> arrayList = this.listeners;
        synchronized (arrayList) {
            return this.listeners.remove(listener);
        }
    }

    public synchronized boolean connect() throws IOException, InterruptedException {
        if (this.isDisposed) {
            System.err.println("\tError. Cannot connect. This Twirk instance has been disposed.");
            return false;
        }
        if (this.isConnected) {
            System.err.println("\tError. Cannot connect. Already connected to Twitch server");
            return false;
        }
        if (!this.resourcesCreated) {
            this.createResources();
        }
        int oldTimeout = this.socket.getSoTimeout();
        this.socket.setSoTimeout(10000);
        this.isConnected = this.doConnect();
        this.socket.setSoTimeout(oldTimeout);
        if (this.isConnected) {
            this.outThread.start();
            this.addCapacies();
            Thread.sleep(1000L);
            this.inThread.start();
            this.serverMessage("JOIN " + this.channel);
            for (TwirkListener listener : this.listeners) {
                listener.onConnect();
            }
            return true;
        }
        return false;
    }

    public synchronized void disconnect() {
        if (!this.isConnected || this.isDisposed) {
            return;
        }
        this.isConnected = false;
        System.out.println("\n\tDisconnecting from Twitch chat...");
        this.releaseResources();
        System.out.println("\tDisconnected from Twitch chat\n");
        for (TwirkListener l : this.listeners) {
            l.onDisconnect();
        }
    }

    public synchronized void close() {
        if (this.isDisposed) {
            return;
        }
        this.isConnected = false;
        this.isDisposed = true;
        System.out.println("\n\tDisposing of IRC...");
        this.releaseResources();
        System.out.println("\tDisposing of IRC completed\n");
    }

    private void createResources() throws IOException {
        this.socket.setSoTimeout(360000);
        this.writer = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), StandardCharsets.UTF_8));
        this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream(), StandardCharsets.UTF_8));
        this.outThread = new OutputThread(this, this.queue, this.reader, this.writer);
        this.inThread = new InputThread(this, this.reader, this.writer);
        this.resourcesCreated = true;
    }

    private void releaseResources() {
        this.resourcesCreated = false;
        this.outThread.end();
        this.inThread.end();
        try {
            this.socket.close();
        }
        catch (IOException iOException) {
            // empty catch block
        }
        try {
            this.reader.close();
        }
        catch (IOException iOException) {
            // empty catch block
        }
        try {
            this.writer.close();
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private boolean doConnect() throws IOException {
        String line;
        this.writer.write("PASS " + this.pass + "\r\n");
        this.writer.write("NICK " + this.nick + "\r\n");
        this.writer.write("USER " + this.nick + " 8 * : GikkBot\r\n");
        this.writer.flush();
        while ((line = this.reader.readLine()) != null) {
            if (this.verboseMode) {
                System.out.println("IN  " + line);
            }
            if (line.contains("004")) {
                return true;
            }
            if (!line.contains("Error logging in")) continue;
            return false;
        }
        return false;
    }

    private void addCapacies() {
        this.serverMessage("CAP REQ :twitch.tv/membership");
        this.serverMessage("CAP REQ :twitch.tv/commands");
        this.serverMessage("CAP REQ :twitch.tv/tags");
    }

    void setOutputMessageDelay(int millis) {
        this.outThread.setMessageDelay(millis);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    void incommingMessage(String line) {
        if (line.contains("PING")) {
            System.out.println("IN  " + line);
            this.serverMessage("PONG " + line.substring(5));
            return;
        }
        ArrayList<TwirkListener> arrayList = this.listeners;
        synchronized (arrayList) {
            String s;
            for (TwirkListener l : this.listeners) {
                l.onAnything(line);
            }
            TwitchMessage message = this.twitchMessageBuilder.build(line);
            switch (s = message.getCommand()) {
                case "JOIN": {
                    String userName = this.parseUsername(message.getPrefix());
                    for (TwirkListener l : this.listeners) {
                        l.onJoin(userName);
                    }
                    break;
                }
                case "MODE": {
                    Mode mode = this.modeBuilder.build(message);
                    for (TwirkListener l : this.listeners) {
                        l.onMode(mode);
                    }
                    break;
                }
                case "PART": {
                    String userName = this.parseUsername(message.getPrefix());
                    for (TwirkListener l : this.listeners) {
                        l.onPart(userName);
                    }
                    break;
                }
                case "CLEARCHAT": {
                    ClearChat clearChat = this.clearChatBuilder.build(message);
                    for (TwirkListener l : this.listeners) {
                        l.onClearChat(clearChat);
                    }
                    break;
                }
                case "PRIVMSG": {
                    TwitchUser user = this.twitchUserBuilder.build(message);
                    for (TwirkListener l : this.listeners) {
                        l.onPrivMsg(user, message);
                    }
                    break;
                }
                case "WHISPER": {
                    TwitchUser user = this.twitchUserBuilder.build(message);
                    for (TwirkListener l : this.listeners) {
                        l.onWhisper(user, message);
                    }
                    break;
                }
                case "NOTICE": {
                    Notice notice = this.noticeBuilder.build(message);
                    for (TwirkListener l : this.listeners) {
                        l.onNotice(notice);
                    }
                    break;
                }
                case "USERSTATE": {
                    Userstate userstate = this.userstateBuilder.build(message);
                    for (TwirkListener l : this.listeners) {
                        l.onUserstate(userstate);
                    }
                    break;
                }
                case "USERNOTICE": {
                    TwitchUser user = this.twitchUserBuilder.build(message);
                    Usernotice usernotice = this.usernoticeBuilder.build(message);
                    for (TwirkListener l : this.listeners) {
                        l.onUsernotice(user, usernotice);
                    }
                    break;
                }
                case "ROOMSTATE": {
                    Roomstate roomstate = this.roomstateBuilder.build(message);
                    for (TwirkListener l : this.listeners) {
                        l.onRoomstate(roomstate);
                    }
                    break;
                }
                case "HOSTTARGET": {
                    HostTarget hostTarget = this.hostTargetBuilder.build(message);
                    for (TwirkListener l : this.listeners) {
                        l.onHost(hostTarget);
                    }
                    break;
                }
                case "RECONNECT": {
                    for (TwirkListener twirkListener : this.listeners) {
                        twirkListener.onReconnect();
                    }
                    break;
                }
                case "353": {
                    List<String> users = Arrays.asList(message.getContent().split(" "));
                    this.online.addAll(users);
                    break;
                }
                case "366": {
                    Set<String> users = Collections.unmodifiableSet(this.online);
                    for (TwirkListener l : this.listeners) {
                        l.onNamesList(users);
                    }
                }
                default: {
                    for (TwirkListener twirkListener : this.listeners) {
                        twirkListener.onUnknown(line);
                    }
                }
            }
        }
    }

    private String parseUsername(String prefix) {
        return prefix.substring(prefix.charAt(0) == ':' ? 1 : 0, prefix.indexOf(33));
    }
}


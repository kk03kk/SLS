/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk;

import com.gikk.twirk.Twirk;
import com.gikk.twirk.types.clearChat.ClearChatBuilder;
import com.gikk.twirk.types.hostTarget.HostTargetBuilder;
import com.gikk.twirk.types.mode.ModeBuilder;
import com.gikk.twirk.types.notice.NoticeBuilder;
import com.gikk.twirk.types.reconnect.ReconnectBuilder;
import com.gikk.twirk.types.roomstate.RoomstateBuilder;
import com.gikk.twirk.types.twitchMessage.TwitchMessageBuilder;
import com.gikk.twirk.types.usernotice.UsernoticeBuilder;
import com.gikk.twirk.types.users.TwitchUserBuilder;
import com.gikk.twirk.types.users.UserstateBuilder;
import java.io.IOException;
import java.net.Socket;
import javax.net.ssl.SSLSocketFactory;

public class TwirkBuilder {
    boolean verboseMode = false;
    String server = "irc.chat.twitch.tv";
    int port = 6697;
    boolean useSSL = true;
    String nick = "";
    String oauth = "";
    String channel = "";
    private ClearChatBuilder clearChatBuilder;
    private HostTargetBuilder hostTargetBuilder;
    private ModeBuilder modeBuilder;
    private NoticeBuilder noticeBuilder;
    private RoomstateBuilder roomstateBuilder;
    private TwitchMessageBuilder twitchMessageBuilder;
    private TwitchUserBuilder twitchUserBuilder;
    private UserstateBuilder userstateBuilder;
    private UsernoticeBuilder usernoticeBuilder;
    private ReconnectBuilder reconnectBuilder;
    private Socket socket;

    public TwirkBuilder(String channel, String nick, String oauth) {
        this.channel = channel;
        this.nick = nick;
        this.oauth = oauth;
    }

    public TwirkBuilder setServer(String server) {
        this.server = server;
        return this;
    }

    public TwirkBuilder setPort(int port) {
        this.port = port;
        return this;
    }

    public TwirkBuilder setSSL(boolean ssl) {
        this.useSSL = ssl;
        return this;
    }

    public TwirkBuilder setVerboseMode(boolean verboseMode) {
        this.verboseMode = verboseMode;
        return this;
    }

    public TwirkBuilder setClearChatBuilder(ClearChatBuilder clearChatBuilder) {
        this.clearChatBuilder = clearChatBuilder;
        return this;
    }

    public TwirkBuilder setHostTargetBuilder(HostTargetBuilder hostTargetBuilder) {
        this.hostTargetBuilder = hostTargetBuilder;
        return this;
    }

    public TwirkBuilder setModeBuilder(ModeBuilder modeBuilder) {
        this.modeBuilder = modeBuilder;
        return this;
    }

    public TwirkBuilder setNoticeBuilder(NoticeBuilder noticeBuilder) {
        this.noticeBuilder = noticeBuilder;
        return this;
    }

    public TwirkBuilder setRoomstateBuilder(RoomstateBuilder roomstateBuilder) {
        this.roomstateBuilder = roomstateBuilder;
        return this;
    }

    public TwirkBuilder setTwitchMessageBuilder(TwitchMessageBuilder twitchMessageBuilder) {
        this.twitchMessageBuilder = twitchMessageBuilder;
        return this;
    }

    public TwirkBuilder setTwitchUserBuilder(TwitchUserBuilder twitchUserBuilder) {
        this.twitchUserBuilder = twitchUserBuilder;
        return this;
    }

    public TwirkBuilder setUsernoticeBuilder(UsernoticeBuilder usernoticeBuilder) {
        this.usernoticeBuilder = usernoticeBuilder;
        return this;
    }

    public TwirkBuilder setReconnectBuilder(ReconnectBuilder reconnectBuilder) {
        this.reconnectBuilder = reconnectBuilder;
        return this;
    }

    public TwirkBuilder setUserstateBuilder(UserstateBuilder userstateBuilder) {
        this.userstateBuilder = userstateBuilder;
        return this;
    }

    public TwirkBuilder setSocket(Socket socket) {
        this.socket = socket;
        return this;
    }

    public ClearChatBuilder getClearChatBuilder() {
        return this.clearChatBuilder != null ? this.clearChatBuilder : ClearChatBuilder.getDefault();
    }

    public HostTargetBuilder getHostTargetBuilder() {
        return this.hostTargetBuilder != null ? this.hostTargetBuilder : HostTargetBuilder.getDefault();
    }

    public ModeBuilder getModeBuilder() {
        return this.modeBuilder != null ? this.modeBuilder : ModeBuilder.getDefault();
    }

    public NoticeBuilder getNoticeBuilder() {
        return this.noticeBuilder != null ? this.noticeBuilder : NoticeBuilder.getDefault();
    }

    public RoomstateBuilder getRoomstateBuilder() {
        return this.roomstateBuilder != null ? this.roomstateBuilder : RoomstateBuilder.getDefault();
    }

    public TwitchMessageBuilder getTwitchMessageBuilder() {
        return this.twitchMessageBuilder != null ? this.twitchMessageBuilder : TwitchMessageBuilder.getDefault();
    }

    public TwitchUserBuilder getTwitchUserBuilder() {
        return this.twitchUserBuilder != null ? this.twitchUserBuilder : TwitchUserBuilder.getDefault();
    }

    public UserstateBuilder getUserstateBuilder() {
        return this.userstateBuilder != null ? this.userstateBuilder : UserstateBuilder.getDefault();
    }

    public UsernoticeBuilder getUsernoticeBuilder() {
        return this.usernoticeBuilder != null ? this.usernoticeBuilder : UsernoticeBuilder.getDefault();
    }

    public ReconnectBuilder getReconnectBuilder() {
        return this.reconnectBuilder != null ? this.reconnectBuilder : ReconnectBuilder.getDefault();
    }

    public Socket getSocket() {
        return this.socket;
    }

    public Twirk build() throws IOException {
        if (this.socket == null) {
            this.socket = this.useSSL ? SSLSocketFactory.getDefault().createSocket(this.server, this.port) : new Socket(this.server, this.port);
        }
        return new Twirk(this);
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.clearChat;

import com.gikk.twirk.enums.CLEARCHAT_MODE;
import com.gikk.twirk.types.TagMap;
import com.gikk.twirk.types.clearChat.ClearChat;
import com.gikk.twirk.types.clearChat.ClearChatBuilder;
import com.gikk.twirk.types.clearChat.ClearChatImpl;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;

class DefaultClearChatBuilder
implements ClearChatBuilder {
    CLEARCHAT_MODE mode;
    String target = "";
    int duration = -1;
    String reason = "";
    String rawLine;

    DefaultClearChatBuilder() {
    }

    @Override
    public ClearChat build(TwitchMessage twitchMessage) {
        this.rawLine = twitchMessage.getRaw();
        if (twitchMessage.getContent().isEmpty()) {
            this.mode = CLEARCHAT_MODE.COMPLETE;
            this.target = "";
        } else {
            this.mode = CLEARCHAT_MODE.USER;
            this.target = twitchMessage.getContent();
            TagMap r = twitchMessage.getTagMap();
            this.duration = r.getAsInt("ban-duration");
            this.reason = r.getAsString("ban-reason");
        }
        return new ClearChatImpl(this);
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.mode;

import com.gikk.twirk.types.mode.Mode;
import com.gikk.twirk.types.mode.ModeBuilder;
import com.gikk.twirk.types.mode.ModeImpl;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;

class DefaultModeBuilder
implements ModeBuilder {
    Mode.MODE_EVENT event;
    String user;
    String rawLine;

    DefaultModeBuilder() {
    }

    @Override
    public Mode build(TwitchMessage message) {
        this.rawLine = message.getRaw();
        String content = message.getContent();
        this.event = content.startsWith("+o") ? Mode.MODE_EVENT.GAINED_MOD : Mode.MODE_EVENT.LOST_MOD;
        this.user = content.substring(content.indexOf(32) + 1);
        return new ModeImpl(this);
    }
}


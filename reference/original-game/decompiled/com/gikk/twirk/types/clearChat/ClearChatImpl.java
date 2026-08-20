/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.clearChat;

import com.gikk.twirk.enums.CLEARCHAT_MODE;
import com.gikk.twirk.types.clearChat.ClearChat;
import com.gikk.twirk.types.clearChat.DefaultClearChatBuilder;

class ClearChatImpl
implements ClearChat {
    public final CLEARCHAT_MODE mode;
    public final String target;
    private final String reason;
    private final String rawLine;
    private final int duration;

    ClearChatImpl(DefaultClearChatBuilder builder) {
        this.mode = builder.mode;
        this.target = builder.target;
        this.reason = builder.reason;
        this.duration = builder.duration;
        this.rawLine = builder.rawLine;
    }

    @Override
    public CLEARCHAT_MODE getMode() {
        return this.mode;
    }

    @Override
    public String getTarget() {
        return this.target;
    }

    @Override
    public int getDuration() {
        return this.duration;
    }

    @Override
    public String getReason() {
        return this.reason;
    }

    @Override
    public String getRaw() {
        return this.rawLine;
    }
}


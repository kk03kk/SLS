/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.clearChat;

import com.gikk.twirk.types.clearChat.ClearChat;
import com.gikk.twirk.types.clearChat.DefaultClearChatBuilder;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;

public interface ClearChatBuilder {
    public ClearChat build(TwitchMessage var1);

    public static ClearChatBuilder getDefault() {
        return new DefaultClearChatBuilder();
    }
}


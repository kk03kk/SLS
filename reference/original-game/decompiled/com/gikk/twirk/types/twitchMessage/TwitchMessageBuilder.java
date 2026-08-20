/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.twitchMessage;

import com.gikk.twirk.types.twitchMessage.DefaultTwitchMessageBuilder;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;

public interface TwitchMessageBuilder {
    public TwitchMessage build(String var1);

    public static TwitchMessageBuilder getDefault() {
        return new DefaultTwitchMessageBuilder();
    }
}


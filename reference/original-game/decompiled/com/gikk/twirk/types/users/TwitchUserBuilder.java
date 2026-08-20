/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.users;

import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.DefaultTwitchUserBuilder;
import com.gikk.twirk.types.users.TwitchUser;

public interface TwitchUserBuilder {
    public TwitchUser build(TwitchMessage var1);

    public static TwitchUserBuilder getDefault() {
        return new DefaultTwitchUserBuilder();
    }
}


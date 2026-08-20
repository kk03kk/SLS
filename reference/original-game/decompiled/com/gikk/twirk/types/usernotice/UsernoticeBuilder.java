/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.usernotice;

import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.usernotice.DefaultUsernoticeBuilder;
import com.gikk.twirk.types.usernotice.Usernotice;

public interface UsernoticeBuilder {
    public Usernotice build(TwitchMessage var1);

    public static UsernoticeBuilder getDefault() {
        return new DefaultUsernoticeBuilder();
    }
}


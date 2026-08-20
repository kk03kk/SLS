/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.users;

import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.DefaultUserstateBuilder;
import com.gikk.twirk.types.users.Userstate;

public interface UserstateBuilder {
    public static UserstateBuilder getDefault() {
        return new DefaultUserstateBuilder();
    }

    public Userstate build(TwitchMessage var1);
}


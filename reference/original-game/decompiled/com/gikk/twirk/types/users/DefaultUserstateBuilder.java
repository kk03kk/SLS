/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.users;

import com.gikk.twirk.types.AbstractTwitchUserFields;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.Userstate;
import com.gikk.twirk.types.users.UserstateBuilder;
import com.gikk.twirk.types.users.UserstateImpl;

class DefaultUserstateBuilder
extends AbstractTwitchUserFields
implements UserstateBuilder {
    DefaultUserstateBuilder() {
    }

    @Override
    public Userstate build(TwitchMessage message) {
        this.parseUserProperties(message);
        return new UserstateImpl(this);
    }
}


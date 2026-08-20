/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.users;

import com.gikk.twirk.types.AbstractTwitchUserFields;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;
import com.gikk.twirk.types.users.TwitchUser;
import com.gikk.twirk.types.users.TwitchUserBuilder;
import com.gikk.twirk.types.users.TwitchUserImpl;

class DefaultTwitchUserBuilder
extends AbstractTwitchUserFields
implements TwitchUserBuilder {
    DefaultTwitchUserBuilder() {
    }

    @Override
    public TwitchUser build(TwitchMessage message) {
        this.parseUserProperties(message);
        return new TwitchUserImpl(this);
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.usernotice;

import com.gikk.twirk.types.AbstractEmoteMessage;
import com.gikk.twirk.types.usernotice.subtype.Raid;
import com.gikk.twirk.types.usernotice.subtype.Ritual;
import com.gikk.twirk.types.usernotice.subtype.Subscription;
import java.util.Optional;

public interface Usernotice
extends AbstractEmoteMessage {
    public String getMessage();

    public String getSystemMessage();

    public boolean isSubscription();

    public Optional<Subscription> getSubscription();

    public boolean isRaid();

    public Optional<Raid> getRaid();

    public boolean isRitual();

    public Optional<Ritual> getRitual();
}


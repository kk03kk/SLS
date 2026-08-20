/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.hostTarget;

import com.gikk.twirk.types.hostTarget.DefaultHostTargetBuilder;
import com.gikk.twirk.types.hostTarget.HostTarget;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;

public interface HostTargetBuilder {
    public static HostTargetBuilder getDefault() {
        return new DefaultHostTargetBuilder();
    }

    public HostTarget build(TwitchMessage var1);
}


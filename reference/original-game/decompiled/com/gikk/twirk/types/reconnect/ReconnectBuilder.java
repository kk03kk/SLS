/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.reconnect;

import com.gikk.twirk.types.reconnect.DefaultReconnectBuilder;
import com.gikk.twirk.types.reconnect.Reconnect;

public interface ReconnectBuilder {
    public static ReconnectBuilder getDefault() {
        return new DefaultReconnectBuilder();
    }

    public Reconnect build();
}


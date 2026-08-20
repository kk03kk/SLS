/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.reconnect;

import com.gikk.twirk.types.reconnect.Reconnect;
import com.gikk.twirk.types.reconnect.ReconnectBuilder;
import com.gikk.twirk.types.reconnect.ReconnectImpl;

public class DefaultReconnectBuilder
implements ReconnectBuilder {
    @Override
    public Reconnect build() {
        return new ReconnectImpl();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.mode;

import com.gikk.twirk.types.mode.DefaultModeBuilder;
import com.gikk.twirk.types.mode.Mode;

class ModeImpl
implements Mode {
    private final String user;
    private final Mode.MODE_EVENT event;
    private final String rawLine;

    ModeImpl(DefaultModeBuilder builder) {
        this.event = builder.event;
        this.user = builder.user;
        this.rawLine = builder.rawLine;
    }

    @Override
    public Mode.MODE_EVENT getEvent() {
        return this.event;
    }

    @Override
    public String getUser() {
        return this.user;
    }

    @Override
    public String getRaw() {
        return this.rawLine;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.roomstate;

import com.gikk.twirk.types.roomstate.DefaultRoomstateBuilder;
import com.gikk.twirk.types.roomstate.Roomstate;

class RoomstateImpl
implements Roomstate {
    private static final String LANGUAGE_IDENTIFIER = "broadcaster-lang=";
    private static final String R9K_IDENTIFIER = "r9k=";
    private static final String SUBS_IDENTIFIER = "subs-only=";
    private static final String SLOW_IDENTIFIER = "slow=";
    private final String broadcasterLanguage;
    private final int r9kMode;
    private final int subMode;
    private final int slowModeTimer;
    private final String rawLine;

    RoomstateImpl(DefaultRoomstateBuilder builder) {
        this.broadcasterLanguage = builder.broadcasterLanguage;
        this.r9kMode = builder.r9kMode;
        this.subMode = builder.subMode;
        this.slowModeTimer = builder.slowModeTimer;
        this.rawLine = builder.rawLine;
    }

    @Override
    public String getBroadcasterLanguage() {
        return this.broadcasterLanguage;
    }

    @Override
    public int get9kMode() {
        return this.r9kMode;
    }

    @Override
    public int getSubMode() {
        return this.subMode;
    }

    @Override
    public int getSlowModeTimer() {
        return this.slowModeTimer;
    }

    public String toString() {
        return LANGUAGE_IDENTIFIER + this.broadcasterLanguage + " " + (this.r9kMode == 0 ? "" : R9K_IDENTIFIER + this.r9kMode) + " " + (this.slowModeTimer == 0 ? "" : SLOW_IDENTIFIER + this.slowModeTimer) + " " + (this.subMode == 0 ? "" : SUBS_IDENTIFIER + this.subMode);
    }

    @Override
    public String getRaw() {
        return this.rawLine;
    }
}


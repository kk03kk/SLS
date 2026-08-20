/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.mode;

import com.gikk.twirk.types.mode.DefaultModeBuilder;
import com.gikk.twirk.types.mode.Mode;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;

public interface ModeBuilder {
    public static ModeBuilder getDefault() {
        return new DefaultModeBuilder();
    }

    public Mode build(TwitchMessage var1);
}


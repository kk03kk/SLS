/*
 * Decompiled with CFR 0.152.
 */
package com.gikk.twirk.types.roomstate;

import com.gikk.twirk.types.roomstate.DefaultRoomstateBuilder;
import com.gikk.twirk.types.roomstate.Roomstate;
import com.gikk.twirk.types.twitchMessage.TwitchMessage;

public interface RoomstateBuilder {
    public static RoomstateBuilder getDefault() {
        return new DefaultRoomstateBuilder();
    }

    public Roomstate build(TwitchMessage var1);
}


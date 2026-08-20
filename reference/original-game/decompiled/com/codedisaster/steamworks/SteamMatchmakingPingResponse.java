/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamInterface;
import com.codedisaster.steamworks.SteamMatchmakingGameServerItem;

public abstract class SteamMatchmakingPingResponse
extends SteamInterface {
    protected SteamMatchmakingPingResponse() {
        super(-1L);
        this.callback = SteamMatchmakingPingResponse.createProxy(this);
    }

    public abstract void serverResponded(SteamMatchmakingGameServerItem var1);

    public abstract void serverFailedToRespond();

    private static native long createProxy(SteamMatchmakingPingResponse var0);
}


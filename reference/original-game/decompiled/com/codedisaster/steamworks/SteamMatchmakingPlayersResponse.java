/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamInterface;

public abstract class SteamMatchmakingPlayersResponse
extends SteamInterface {
    protected SteamMatchmakingPlayersResponse() {
        super(-1L);
        this.callback = SteamMatchmakingPlayersResponse.createProxy(this);
    }

    public abstract void addPlayerToList(String var1, int var2, float var3);

    public abstract void playersFailedToRespond();

    public abstract void playersRefreshComplete();

    private static native long createProxy(SteamMatchmakingPlayersResponse var0);
}


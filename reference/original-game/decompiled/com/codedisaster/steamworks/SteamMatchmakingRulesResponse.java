/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamInterface;

public abstract class SteamMatchmakingRulesResponse
extends SteamInterface {
    protected SteamMatchmakingRulesResponse() {
        super(-1L);
        this.callback = SteamMatchmakingRulesResponse.createProxy(this);
    }

    public abstract void rulesResponded(String var1, String var2);

    public abstract void rulesFailedToRespond();

    public abstract void rulesRefreshComplete();

    private static native long createProxy(SteamMatchmakingRulesResponse var0);
}


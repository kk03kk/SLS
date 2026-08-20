/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamNativeHandle;

public class SteamServerListRequest
extends SteamNativeHandle {
    SteamServerListRequest(long handle) {
        super(handle);
    }

    public boolean isValid() {
        return this.handle != 0L;
    }
}


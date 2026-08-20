/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamNativeHandle;

public class SteamUGCQuery
extends SteamNativeHandle {
    public SteamUGCQuery(long handle) {
        super(handle);
    }

    public boolean isValid() {
        return this.handle != -1L;
    }
}


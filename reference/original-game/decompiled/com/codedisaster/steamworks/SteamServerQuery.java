/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamNativeIntHandle;

public class SteamServerQuery
extends SteamNativeIntHandle {
    public static final SteamServerQuery INVALID = new SteamServerQuery(-1);

    SteamServerQuery(int handle) {
        super(handle);
    }
}


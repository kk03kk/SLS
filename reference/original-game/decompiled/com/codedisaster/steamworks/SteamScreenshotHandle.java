/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamNativeIntHandle;

public class SteamScreenshotHandle
extends SteamNativeIntHandle {
    public static final SteamScreenshotHandle INVALID = new SteamScreenshotHandle(0);

    SteamScreenshotHandle(int handle) {
        super(handle);
    }
}


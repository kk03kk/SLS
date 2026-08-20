/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamScreenshotHandle;

public interface SteamScreenshotsCallback {
    public void onScreenshotReady(SteamScreenshotHandle var1, SteamResult var2);

    public void onScreenshotRequested();
}


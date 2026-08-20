/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamCallbackAdapter;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamScreenshotHandle;
import com.codedisaster.steamworks.SteamScreenshotsCallback;

public class SteamScreenshotsCallbackAdapter
extends SteamCallbackAdapter<SteamScreenshotsCallback> {
    SteamScreenshotsCallbackAdapter(SteamScreenshotsCallback callback) {
        super(callback);
    }

    void onScreenshotReady(int local, int result) {
        ((SteamScreenshotsCallback)this.callback).onScreenshotReady(new SteamScreenshotHandle(local), SteamResult.byValue(result));
    }

    void onScreenshotRequested() {
        ((SteamScreenshotsCallback)this.callback).onScreenshotRequested();
    }
}


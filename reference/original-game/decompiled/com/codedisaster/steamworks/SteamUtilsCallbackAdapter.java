/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamAPIWarningMessageHook;
import com.codedisaster.steamworks.SteamCallbackAdapter;
import com.codedisaster.steamworks.SteamUtilsCallback;

class SteamUtilsCallbackAdapter
extends SteamCallbackAdapter<SteamUtilsCallback> {
    private SteamAPIWarningMessageHook messageHook;

    SteamUtilsCallbackAdapter(SteamUtilsCallback callback) {
        super(callback);
    }

    void setWarningMessageHook(SteamAPIWarningMessageHook messageHook) {
        this.messageHook = messageHook;
    }

    void onWarningMessage(int severity, String message) {
        if (this.messageHook != null) {
            this.messageHook.onWarningMessage(severity, message);
        }
    }

    void onSteamShutdown() {
        ((SteamUtilsCallback)this.callback).onSteamShutdown();
    }
}


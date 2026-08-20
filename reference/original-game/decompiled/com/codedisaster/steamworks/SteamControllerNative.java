/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamControllerAnalogActionData;
import com.codedisaster.steamworks.SteamControllerDigitalActionData;

final class SteamControllerNative {
    SteamControllerNative() {
    }

    static native boolean init();

    static native boolean shutdown();

    static native void runFrame();

    static native int getConnectedControllers(long[] var0);

    static native boolean showBindingPanel(long var0);

    static native long getActionSetHandle(String var0);

    static native void activateActionSet(long var0, long var2);

    static native long getCurrentActionSet(long var0);

    static native long getDigitalActionHandle(String var0);

    static native void getDigitalActionData(long var0, long var2, SteamControllerDigitalActionData var4);

    static native int getDigitalActionOrigins(long var0, long var2, long var4, int[] var6);

    static native long getAnalogActionHandle(String var0);

    static native void getAnalogActionData(long var0, long var2, SteamControllerAnalogActionData var4);

    static native int getAnalogActionOrigins(long var0, long var2, long var4, int[] var6);

    static native void stopAnalogActionMomentum(long var0, long var2);

    static native void triggerHapticPulse(long var0, int var2, int var3);

    static native void triggerRepeatedHapticPulse(long var0, int var2, int var3, int var4, int var5, int var6);

    static native void triggerVibration(long var0, short var2, short var3);

    static native void setLEDColor(long var0, byte var2, byte var3, byte var4, int var5);

    static native int getGamepadIndexForController(long var0);

    static native long getControllerForGamepadIndex(int var0);

    static native void getMotionData(long var0, float[] var2);

    static native String getStringForActionOrigin(int var0);

    static native String getGlyphForActionOrigin(int var0);

    static native int getInputTypeForHandle(long var0);
}


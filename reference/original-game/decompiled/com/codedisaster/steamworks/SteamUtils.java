/*
 * Decompiled with CFR 0.152.
 */
package com.codedisaster.steamworks;

import com.codedisaster.steamworks.SteamAPICall;
import com.codedisaster.steamworks.SteamAPIWarningMessageHook;
import com.codedisaster.steamworks.SteamException;
import com.codedisaster.steamworks.SteamInterface;
import com.codedisaster.steamworks.SteamUniverse;
import com.codedisaster.steamworks.SteamUtilsCallback;
import com.codedisaster.steamworks.SteamUtilsCallbackAdapter;
import com.codedisaster.steamworks.SteamUtilsNative;
import java.nio.ByteBuffer;

public class SteamUtils
extends SteamInterface {
    private final SteamUtilsCallbackAdapter callbackAdapter;

    public SteamUtils(SteamUtilsCallback callback) {
        this.callbackAdapter = new SteamUtilsCallbackAdapter(callback);
        this.setCallback(SteamUtilsNative.createCallback(this.callbackAdapter));
    }

    public int getSecondsSinceAppActive() {
        return SteamUtilsNative.getSecondsSinceAppActive();
    }

    public int getSecondsSinceComputerActive() {
        return SteamUtilsNative.getSecondsSinceComputerActive();
    }

    public SteamUniverse getConnectedUniverse() {
        return SteamUniverse.byValue(SteamUtilsNative.getConnectedUniverse());
    }

    public int getServerRealTime() {
        return SteamUtilsNative.getServerRealTime();
    }

    public int getImageWidth(int image) {
        return SteamUtilsNative.getImageWidth(image);
    }

    public int getImageHeight(int image) {
        return SteamUtilsNative.getImageHeight(image);
    }

    public boolean getImageSize(int image, int[] size) {
        return SteamUtilsNative.getImageSize(image, size);
    }

    public boolean getImageRGBA(int image, ByteBuffer dest) throws SteamException {
        this.checkBuffer(dest);
        return SteamUtilsNative.getImageRGBA(image, dest, dest.position(), dest.remaining());
    }

    public int getAppID() {
        return SteamUtilsNative.getAppID();
    }

    public void setOverlayNotificationPosition(NotificationPosition position) {
        SteamUtilsNative.setOverlayNotificationPosition(position.ordinal());
    }

    public boolean isAPICallCompleted(SteamAPICall handle, boolean[] result) {
        return SteamUtilsNative.isAPICallCompleted(handle.handle, result);
    }

    public SteamAPICallFailure getAPICallFailureReason(SteamAPICall handle) {
        return SteamAPICallFailure.byValue(SteamUtilsNative.getAPICallFailureReason(handle.handle));
    }

    public void setWarningMessageHook(SteamAPIWarningMessageHook messageHook) {
        this.callbackAdapter.setWarningMessageHook(messageHook);
        SteamUtilsNative.enableWarningMessageHook(this.callback, messageHook != null);
    }

    public boolean isOverlayEnabled() {
        return SteamUtilsNative.isOverlayEnabled();
    }

    public boolean isSteamRunningOnSteamDeck() {
        return SteamUtilsNative.isSteamRunningOnSteamDeck();
    }

    public boolean showFloatingGamepadTextInput(FloatingGamepadTextInputMode keyboardMode, int textFieldXPosition, int textFieldYPosition, int textFieldWidth, int textFieldHeight) {
        return SteamUtilsNative.showFloatingGamepadTextInput(keyboardMode.ordinal(), textFieldXPosition, textFieldYPosition, textFieldWidth, textFieldHeight);
    }

    public boolean dismissFloatingGamepadTextInput() {
        return SteamUtilsNative.dismissFloatingGamepadTextInput();
    }

    public static enum FloatingGamepadTextInputMode {
        ModeSingleLine,
        ModeMultipleLines,
        ModeEmail,
        ModeNumeric;

    }

    public static enum NotificationPosition {
        TopLeft,
        TopRight,
        BottomLeft,
        BottomRight;

    }

    public static enum SteamAPICallFailure {
        None(-1),
        SteamGone(0),
        NetworkFailure(1),
        InvalidHandle(2),
        MismatchedCallback(3);

        private final int code;
        private static final SteamAPICallFailure[] values;

        private SteamAPICallFailure(int code) {
            this.code = code;
        }

        static SteamAPICallFailure byValue(int code) {
            for (SteamAPICallFailure value : values) {
                if (value.code != code) continue;
                return value;
            }
            return None;
        }

        static {
            values = SteamAPICallFailure.values();
        }
    }
}


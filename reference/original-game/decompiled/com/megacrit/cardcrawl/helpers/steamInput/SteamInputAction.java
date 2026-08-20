/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers.steamInput;

import com.codedisaster.steamworks.SteamController;
import com.codedisaster.steamworks.SteamControllerDigitalActionData;
import com.codedisaster.steamworks.SteamControllerDigitalActionHandle;
import com.codedisaster.steamworks.SteamControllerHandle;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.controller.CInputAction;
import com.megacrit.cardcrawl.helpers.steamInput.SteamInputHelper;

public class SteamInputAction {
    public SteamController controller;
    public SteamControllerHandle controllerHandle;
    public SteamControllerDigitalActionHandle actionHandle;
    public SteamControllerDigitalActionData actionData = new SteamControllerDigitalActionData();
    private CInputAction ref;

    public SteamInputAction(SteamControllerDigitalActionHandle handle, CInputAction actionRef) {
        this.actionHandle = handle;
        this.ref = actionRef;
    }

    public void init(SteamController controller, SteamControllerHandle controllerHandle) {
        this.controller = controller;
        this.controllerHandle = controllerHandle;
    }

    public void update() {
        if (this.controller == null || this.actionHandle == null) {
            return;
        }
        this.controller.getDigitalActionData(SteamInputHelper.handle, this.actionHandle, this.actionData);
        if (this.actionData.getActive() && this.actionData.getState()) {
            if (!this.ref.pressed) {
                this.ref.pressed = true;
                this.ref.justPressed = true;
            } else {
                if (Settings.CONTROLLER_ENABLED && !Settings.isControllerMode) {
                    Settings.isControllerMode = true;
                }
                this.ref.justPressed = false;
            }
        } else if (this.ref.pressed) {
            this.ref.pressed = false;
            this.ref.justPressed = false;
        }
    }
}


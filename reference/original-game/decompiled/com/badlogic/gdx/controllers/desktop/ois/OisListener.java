/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.controllers.desktop.ois;

import com.badlogic.gdx.controllers.desktop.ois.OisJoystick;

public interface OisListener {
    public void buttonPressed(OisJoystick var1, int var2);

    public void buttonReleased(OisJoystick var1, int var2);

    public void axisMoved(OisJoystick var1, int var2, float var3);

    public void povMoved(OisJoystick var1, int var2, OisJoystick.OisPov var3);

    public void xSliderMoved(OisJoystick var1, int var2, boolean var3);

    public void ySliderMoved(OisJoystick var1, int var2, boolean var3);
}


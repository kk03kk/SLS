/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.controllers;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;

public interface ControllerListener {
    public void connected(Controller var1);

    public void disconnected(Controller var1);

    public boolean buttonDown(Controller var1, int var2);

    public boolean buttonUp(Controller var1, int var2);

    public boolean axisMoved(Controller var1, int var2, float var3);

    public boolean povMoved(Controller var1, int var2, PovDirection var3);

    public boolean xSliderMoved(Controller var1, int var2, boolean var3);

    public boolean ySliderMoved(Controller var1, int var2, boolean var3);

    public boolean accelerometerMoved(Controller var1, int var2, Vector3 var3);
}


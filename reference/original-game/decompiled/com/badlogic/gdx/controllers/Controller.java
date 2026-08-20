/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.controllers;

import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;

public interface Controller {
    public boolean getButton(int var1);

    public float getAxis(int var1);

    public PovDirection getPov(int var1);

    public boolean getSliderX(int var1);

    public boolean getSliderY(int var1);

    public Vector3 getAccelerometer(int var1);

    public void setAccelerometerSensitivity(float var1);

    public String getName();

    public void addListener(ControllerListener var1);

    public void removeListener(ControllerListener var1);
}


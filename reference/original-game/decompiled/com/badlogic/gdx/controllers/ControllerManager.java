/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.controllers;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.utils.Array;

public interface ControllerManager {
    public Array<Controller> getControllers();

    public void addListener(ControllerListener var1);

    public void removeListener(ControllerListener var1);

    public Array<ControllerListener> getListeners();

    public void clearListeners();
}


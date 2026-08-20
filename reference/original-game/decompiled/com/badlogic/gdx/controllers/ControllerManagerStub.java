/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.controllers;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.ControllerManager;
import com.badlogic.gdx.utils.Array;

public class ControllerManagerStub
implements ControllerManager {
    Array<Controller> controllers = new Array();

    @Override
    public Array<Controller> getControllers() {
        return this.controllers;
    }

    @Override
    public void addListener(ControllerListener listener) {
    }

    @Override
    public void removeListener(ControllerListener listener) {
    }

    @Override
    public void clearListeners() {
    }

    @Override
    public Array<ControllerListener> getListeners() {
        return new Array<ControllerListener>();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.controllers.desktop;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.ControllerManager;
import com.badlogic.gdx.controllers.desktop.OisControllers;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class DesktopControllerManager
implements ControllerManager {
    final Array<Controller> controllers = new Array();
    final Array<ControllerListener> listeners = new Array();

    public DesktopControllerManager() {
        new SharedLibraryLoader().load("gdx-controllers-desktop");
        new OisControllers(this);
    }

    @Override
    public Array<Controller> getControllers() {
        return this.controllers;
    }

    @Override
    public void addListener(ControllerListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(ControllerListener listener) {
        this.listeners.removeValue(listener, true);
    }

    @Override
    public void clearListeners() {
        this.listeners.clear();
    }

    @Override
    public Array<ControllerListener> getListeners() {
        return this.listeners;
    }
}


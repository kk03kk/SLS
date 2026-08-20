/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.controllers;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.ControllerManager;
import com.badlogic.gdx.controllers.ControllerManagerStub;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;

public class Controllers {
    private static final String TAG = "Controllers";
    static final ObjectMap<Application, ControllerManager> managers = new ObjectMap();

    public static Array<Controller> getControllers() {
        Controllers.initialize();
        return Controllers.getManager().getControllers();
    }

    public static void addListener(ControllerListener listener) {
        Controllers.initialize();
        Controllers.getManager().addListener(listener);
    }

    public static void removeListener(ControllerListener listener) {
        Controllers.initialize();
        Controllers.getManager().removeListener(listener);
    }

    public static void clearListeners() {
        Controllers.initialize();
        Controllers.getManager().clearListeners();
    }

    public static Array<ControllerListener> getListeners() {
        Controllers.initialize();
        return Controllers.getManager().getListeners();
    }

    private static ControllerManager getManager() {
        return managers.get(Gdx.app);
    }

    private static void initialize() {
        if (managers.containsKey(Gdx.app)) {
            return;
        }
        String className = null;
        Application.ApplicationType type = Gdx.app.getType();
        ControllerManager manager = null;
        if (type == Application.ApplicationType.Android) {
            if (Gdx.app.getVersion() >= 12) {
                className = "com.badlogic.gdx.controllers.android.AndroidControllers";
            } else {
                Gdx.app.log(TAG, "No controller manager is available for Android versions < API level 12");
                manager = new ControllerManagerStub();
            }
        } else if (type == Application.ApplicationType.Desktop) {
            className = Gdx.graphics.getType() == Graphics.GraphicsType.LWJGL3 ? "com.badlogic.gdx.controllers.lwjgl3.Lwjgl3ControllerManager" : "com.badlogic.gdx.controllers.desktop.DesktopControllerManager";
        } else if (type == Application.ApplicationType.WebGL) {
            className = "com.badlogic.gdx.controllers.gwt.GwtControllers";
        } else {
            Gdx.app.log(TAG, "No controller manager is available for: " + (Object)((Object)Gdx.app.getType()));
            manager = new ControllerManagerStub();
        }
        if (manager == null) {
            try {
                Class controllerManagerClass = ClassReflection.forName(className);
                manager = (ControllerManager)ClassReflection.newInstance(controllerManagerClass);
            }
            catch (Throwable ex) {
                throw new GdxRuntimeException("Error creating controller manager: " + className, ex);
            }
        }
        managers.put(Gdx.app, manager);
        final Application app = Gdx.app;
        Gdx.app.addLifecycleListener(new LifecycleListener(){

            @Override
            public void resume() {
            }

            @Override
            public void pause() {
            }

            @Override
            public void dispose() {
                managers.remove(app);
                Gdx.app.log(Controllers.TAG, "removed manager for application, " + Controllers.managers.size + " managers active");
            }
        });
        Gdx.app.log(TAG, "added manager for application, " + Controllers.managers.size + " managers active");
    }
}


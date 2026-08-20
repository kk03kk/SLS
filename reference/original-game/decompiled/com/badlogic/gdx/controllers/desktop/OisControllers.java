/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.controllers.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.controllers.ControlType;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.controllers.desktop.DesktopControllerManager;
import com.badlogic.gdx.controllers.desktop.ois.Ois;
import com.badlogic.gdx.controllers.desktop.ois.OisJoystick;
import com.badlogic.gdx.controllers.desktop.ois.OisListener;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import java.awt.Component;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import javax.swing.SwingUtilities;

public class OisControllers {
    final DesktopControllerManager manager;
    long hwnd = OisControllers.getWindowHandle();
    Ois ois = new Ois(this.hwnd);
    OisController[] controllers;
    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
    private static final long CHECK_FOR_LOST_WINDOW_HANDLE_INTERVAL = 1000000000L;

    public OisControllers(final DesktopControllerManager manager) {
        this.manager = manager;
        ArrayList<OisJoystick> joysticks = this.ois.getJoysticks();
        this.controllers = new OisController[joysticks.size()];
        int n = joysticks.size();
        for (int i = 0; i < n; ++i) {
            OisJoystick joystick = joysticks.get(i);
            this.controllers[i] = new OisController(joystick);
            manager.controllers.add(this.controllers[i]);
        }
        new Runnable(){
            private long lastCheckForLostWindowHandleTime;

            @Override
            public void run() {
                long now = System.nanoTime();
                if (now - this.lastCheckForLostWindowHandleTime > 1000000000L) {
                    this.lastCheckForLostWindowHandleTime = now;
                    long newWindowHandle = OisControllers.getWindowHandle();
                    if (OisControllers.this.hwnd != newWindowHandle) {
                        OisControllers.this.hwnd = newWindowHandle;
                        OisControllers.this.ois = new Ois(newWindowHandle);
                        ArrayList<OisJoystick> joysticks = OisControllers.this.ois.getJoysticks();
                        OisControllers.this.controllers = new OisController[joysticks.size()];
                        manager.controllers.clear();
                        int n = joysticks.size();
                        for (int i = 0; i < n; ++i) {
                            OisJoystick joystick = joysticks.get(i);
                            OisControllers.this.controllers[i] = new OisController(joystick);
                            manager.controllers.add(OisControllers.this.controllers[i]);
                        }
                    }
                }
                OisControllers.this.ois.update();
                Gdx.app.postRunnable(this);
            }
        }.run();
    }

    public static long getWindowHandle() {
        if (IS_MAC) {
            return 0L;
        }
        try {
            if (Gdx.graphics.getType() == Graphics.GraphicsType.JGLFW) {
                return (Long)Gdx.graphics.getClass().getDeclaredMethod("getWindow", new Class[0]).invoke(null, new Object[0]);
            }
            if (Gdx.graphics.getType() == Graphics.GraphicsType.LWJGL) {
                if (Gdx.app.getClass().getName().equals("com.badlogic.gdx.backends.lwjgl.LwjglCanvas")) {
                    Class<?> canvasClass = Class.forName("com.badlogic.gdx.backends.lwjgl.LwjglCanvas");
                    Object canvas = canvasClass.getDeclaredMethod("getCanvas", new Class[0]).invoke((Object)Gdx.app, new Object[0]);
                    return (Long)OisControllers.invokeMethod(OisControllers.invokeMethod(SwingUtilities.windowForComponent((Component)canvas), "getPeer"), "getHWnd");
                }
                Class<?> displayClass = Class.forName("org.lwjgl.opengl.Display");
                Method getImplementation = displayClass.getDeclaredMethod("getImplementation", new Class[0]);
                getImplementation.setAccessible(true);
                Object display = getImplementation.invoke(null, (Object[])null);
                Field field = display.getClass().getDeclaredField(IS_WINDOWS ? "hwnd" : "parent_window");
                field.setAccessible(true);
                return (Long)field.get(display);
            }
        }
        catch (Exception ex) {
            throw new RuntimeException("Unable to get window handle.", ex);
        }
        return 0L;
    }

    private static Object invokeMethod(Object object, String methodName) throws Exception {
        for (Method m : object.getClass().getMethods()) {
            if (!m.getName().equals(methodName)) continue;
            return m.invoke(object, new Object[0]);
        }
        throw new RuntimeException("Could not find method '" + methodName + "' on class: " + object.getClass());
    }

    class OisController
    implements Controller {
        private final OisJoystick joystick;
        final Array<ControllerListener> listeners = new Array();

        public OisController(OisJoystick joystick) {
            this.joystick = joystick;
            joystick.setListener(new OisListener(){

                @Override
                public void buttonReleased(OisJoystick joystick, int buttonIndex) {
                    int ii;
                    Array<ControllerListener> allListeners = OisControllers.this.manager.listeners;
                    int nn = allListeners.size;
                    for (ii = 0; ii < nn; ++ii) {
                        allListeners.get(ii).buttonUp(OisController.this, buttonIndex);
                    }
                    nn = OisController.this.listeners.size;
                    for (ii = 0; ii < nn; ++ii) {
                        OisController.this.listeners.get(ii).buttonUp(OisController.this, buttonIndex);
                    }
                }

                @Override
                public void buttonPressed(OisJoystick joystick, int buttonIndex) {
                    int ii;
                    Array<ControllerListener> allListeners = OisControllers.this.manager.listeners;
                    int nn = allListeners.size;
                    for (ii = 0; ii < nn; ++ii) {
                        allListeners.get(ii).buttonDown(OisController.this, buttonIndex);
                    }
                    nn = OisController.this.listeners.size;
                    for (ii = 0; ii < nn; ++ii) {
                        OisController.this.listeners.get(ii).buttonDown(OisController.this, buttonIndex);
                    }
                }

                @Override
                public void axisMoved(OisJoystick joystick, int axisIndex, float value) {
                    int ii;
                    Array<ControllerListener> allListeners = OisControllers.this.manager.listeners;
                    int nn = allListeners.size;
                    for (ii = 0; ii < nn; ++ii) {
                        allListeners.get(ii).axisMoved(OisController.this, axisIndex, value);
                    }
                    nn = OisController.this.listeners.size;
                    for (ii = 0; ii < nn; ++ii) {
                        OisController.this.listeners.get(ii).axisMoved(OisController.this, axisIndex, value);
                    }
                }

                @Override
                public void povMoved(OisJoystick joystick, int povIndex, OisJoystick.OisPov ignored) {
                    int ii;
                    PovDirection value = OisController.this.getPov(povIndex);
                    Array<ControllerListener> allListeners = OisControllers.this.manager.listeners;
                    int nn = allListeners.size;
                    for (ii = 0; ii < nn; ++ii) {
                        allListeners.get(ii).povMoved(OisController.this, povIndex, value);
                    }
                    nn = OisController.this.listeners.size;
                    for (ii = 0; ii < nn; ++ii) {
                        OisController.this.listeners.get(ii).povMoved(OisController.this, povIndex, value);
                    }
                }

                @Override
                public void xSliderMoved(OisJoystick joystick, int sliderIndex, boolean value) {
                    int ii;
                    Array<ControllerListener> allListeners = OisControllers.this.manager.listeners;
                    int nn = allListeners.size;
                    for (ii = 0; ii < nn; ++ii) {
                        allListeners.get(ii).xSliderMoved(OisController.this, sliderIndex, value);
                    }
                    nn = OisController.this.listeners.size;
                    for (ii = 0; ii < nn; ++ii) {
                        OisController.this.listeners.get(ii).xSliderMoved(OisController.this, sliderIndex, value);
                    }
                }

                @Override
                public void ySliderMoved(OisJoystick joystick, int sliderIndex, boolean value) {
                    int ii;
                    Array<ControllerListener> allListeners = OisControllers.this.manager.listeners;
                    int nn = allListeners.size;
                    for (ii = 0; ii < nn; ++ii) {
                        allListeners.get(ii).ySliderMoved(OisController.this, sliderIndex, value);
                    }
                    nn = OisController.this.listeners.size;
                    for (ii = 0; ii < nn; ++ii) {
                        OisController.this.listeners.get(ii).ySliderMoved(OisController.this, sliderIndex, value);
                    }
                }
            });
        }

        @Override
        public boolean getButton(int buttonIndex) {
            return this.joystick.isButtonPressed(buttonIndex);
        }

        @Override
        public float getAxis(int axisIndex) {
            return this.joystick.getAxis(axisIndex);
        }

        @Override
        public PovDirection getPov(int povIndex) {
            OisJoystick.OisPov pov = this.joystick.getPov(povIndex);
            switch (pov) {
                case Centered: {
                    return PovDirection.center;
                }
                case East: {
                    return PovDirection.east;
                }
                case North: {
                    return PovDirection.north;
                }
                case NorthEast: {
                    return PovDirection.northEast;
                }
                case NorthWest: {
                    return PovDirection.northWest;
                }
                case South: {
                    return PovDirection.south;
                }
                case SouthEast: {
                    return PovDirection.southEast;
                }
                case SouthWest: {
                    return PovDirection.southWest;
                }
                case West: {
                    return PovDirection.west;
                }
            }
            return null;
        }

        @Override
        public boolean getSliderX(int sliderIndex) {
            return this.joystick.getSliderX(sliderIndex);
        }

        @Override
        public boolean getSliderY(int sliderIndex) {
            return this.joystick.getSliderY(sliderIndex);
        }

        @Override
        public Vector3 getAccelerometer(int accelerometerIndex) {
            throw new GdxRuntimeException("Invalid accelerometer index: " + accelerometerIndex);
        }

        @Override
        public void setAccelerometerSensitivity(float sensitivity) {
        }

        public int getControlCount(ControlType type) {
            switch (type) {
                case button: {
                    return this.joystick.getButtonCount();
                }
                case axis: {
                    return this.joystick.getAxisCount();
                }
                case slider: {
                    return this.joystick.getSliderCount();
                }
                case pov: {
                    return this.joystick.getPovCount();
                }
            }
            return 0;
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
        public String getName() {
            return this.joystick.getName();
        }

        public String toString() {
            return this.joystick.getName();
        }
    }
}


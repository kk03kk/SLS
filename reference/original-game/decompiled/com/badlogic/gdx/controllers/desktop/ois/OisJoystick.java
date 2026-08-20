/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.controllers.desktop.ois;

import com.badlogic.gdx.controllers.desktop.ois.OisListener;

public class OisJoystick {
    private static final int MIN_AXIS = Short.MIN_VALUE;
    private static final int MAX_AXIS = Short.MAX_VALUE;
    private final String name;
    private final long joystickPtr;
    private final boolean[] buttons;
    private final float[] axes;
    private final int[] povs;
    private final boolean[] slidersX;
    private final boolean[] slidersY;
    private OisListener listener;

    public OisJoystick(long joystickPtr, String name) {
        this.joystickPtr = joystickPtr;
        this.name = name;
        this.initialize(this);
        this.buttons = new boolean[this.getButtonCount()];
        this.axes = new float[this.getAxisCount()];
        this.povs = new int[this.getPovCount()];
        this.slidersX = new boolean[this.getSliderCount()];
        this.slidersY = new boolean[this.getSliderCount()];
    }

    public void setListener(OisListener listener) {
        this.listener = listener;
    }

    private void buttonPressed(int buttonIndex) {
        this.buttons[buttonIndex] = true;
        if (this.listener != null) {
            this.listener.buttonPressed(this, buttonIndex);
        }
    }

    private void buttonReleased(int buttonIndex) {
        this.buttons[buttonIndex] = false;
        if (this.listener != null) {
            this.listener.buttonReleased(this, buttonIndex);
        }
    }

    private void axisMoved(int axisIndex, int value) {
        this.axes[axisIndex] = (float)(value - Short.MIN_VALUE << 1) / 65535.0f - 1.0f;
        if (this.listener != null) {
            this.listener.axisMoved(this, axisIndex, this.axes[axisIndex]);
        }
    }

    private void povMoved(int povIndex, int value) {
        this.povs[povIndex] = value;
        if (this.listener != null) {
            this.listener.povMoved(this, povIndex, this.getPov(povIndex));
        }
    }

    private void sliderMoved(int sliderIndex, int x, int y) {
        boolean xChanged = this.slidersX[sliderIndex] != (x == 1);
        boolean yChanged = this.slidersY[sliderIndex] != (y == 1);
        this.slidersX[sliderIndex] = x == 1;
        boolean bl = this.slidersY[sliderIndex] = y == 1;
        if (this.listener != null) {
            if (xChanged) {
                this.listener.xSliderMoved(this, sliderIndex, x == 1);
            }
            if (yChanged) {
                this.listener.ySliderMoved(this, sliderIndex, y == 1);
            }
        }
    }

    public void update() {
        this.update(this.joystickPtr, this);
    }

    public int getAxisCount() {
        return this.getAxesCount(this.joystickPtr);
    }

    public int getButtonCount() {
        return this.getButtonCount(this.joystickPtr);
    }

    public int getPovCount() {
        return this.getPovCount(this.joystickPtr);
    }

    public int getSliderCount() {
        return this.getSliderCount(this.joystickPtr);
    }

    public float getAxis(int axisIndex) {
        if (axisIndex < 0 || axisIndex >= this.axes.length) {
            return 0.0f;
        }
        return this.axes[axisIndex];
    }

    public OisPov getPov(int povIndex) {
        if (povIndex < 0 || povIndex >= this.povs.length) {
            return OisPov.Centered;
        }
        switch (this.povs[povIndex]) {
            case 0: {
                return OisPov.Centered;
            }
            case 1: {
                return OisPov.North;
            }
            case 16: {
                return OisPov.South;
            }
            case 256: {
                return OisPov.East;
            }
            case 4096: {
                return OisPov.West;
            }
            case 257: {
                return OisPov.NorthEast;
            }
            case 272: {
                return OisPov.SouthEast;
            }
            case 4097: {
                return OisPov.NorthWest;
            }
            case 4112: {
                return OisPov.SouthWest;
            }
        }
        throw new RuntimeException("Unexpected POV value reported by OIS: " + this.povs[povIndex]);
    }

    public boolean isButtonPressed(int buttonIndex) {
        if (buttonIndex < 0 || buttonIndex >= this.buttons.length) {
            return false;
        }
        return this.buttons[buttonIndex];
    }

    public boolean getSliderX(int sliderIndex) {
        if (sliderIndex < 0 || sliderIndex >= this.slidersX.length) {
            return false;
        }
        return this.slidersX[sliderIndex];
    }

    public boolean getSliderY(int sliderIndex) {
        if (sliderIndex < 0 || sliderIndex >= this.slidersY.length) {
            return false;
        }
        return this.slidersY[sliderIndex];
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }

    private native void initialize(OisJoystick var1);

    private native void update(long var1, OisJoystick var3);

    private native int getAxesCount(long var1);

    private native int getButtonCount(long var1);

    private native int getPovCount(long var1);

    private native int getSliderCount(long var1);

    public static enum OisPov {
        Centered,
        North,
        South,
        East,
        West,
        NorthEast,
        SouthEast,
        NorthWest,
        SouthWest;

    }
}


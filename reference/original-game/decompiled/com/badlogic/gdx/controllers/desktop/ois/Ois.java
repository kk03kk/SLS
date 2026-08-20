/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.controllers.desktop.ois;

import com.badlogic.gdx.controllers.desktop.ois.OisJoystick;
import java.util.ArrayList;

public class Ois {
    private final long inputManagerPtr;
    private final ArrayList<OisJoystick> joysticks = new ArrayList();

    public Ois(long hwnd) {
        this.inputManagerPtr = this.createInputManager(hwnd);
        String[] names = this.getJoystickNames(this.inputManagerPtr);
        int n = names.length;
        for (int i = 0; i < n; ++i) {
            this.joysticks.add(new OisJoystick(this.createJoystick(this.inputManagerPtr), names[i]));
        }
    }

    public ArrayList<OisJoystick> getJoysticks() {
        return this.joysticks;
    }

    public void update() {
        int n = this.joysticks.size();
        for (int i = 0; i < n; ++i) {
            this.joysticks.get(i).update();
        }
    }

    public int getVersionNumber() {
        return this.getVersionNumber(this.inputManagerPtr);
    }

    public String getVersionName() {
        return this.getVersionName(this.inputManagerPtr);
    }

    public String getInputSystemName() {
        return this.getInputSystemName(this.inputManagerPtr);
    }

    private native long createInputManager(long var1);

    private native String[] getJoystickNames(long var1);

    private native int getVersionNumber(long var1);

    private native String getVersionName(long var1);

    private native String getInputSystemName(long var1);

    private native long createJoystick(long var1);
}


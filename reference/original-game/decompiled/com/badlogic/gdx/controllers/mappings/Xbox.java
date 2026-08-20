/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.controllers.mappings;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class Xbox {
    public static final int A;
    public static final int B;
    public static final int X;
    public static final int Y;
    public static final int GUIDE;
    public static final int L_BUMPER;
    public static final int R_BUMPER;
    public static final int BACK;
    public static final int START;
    public static final int DPAD_UP;
    public static final int DPAD_DOWN;
    public static final int DPAD_LEFT;
    public static final int DPAD_RIGHT;
    public static final int L_TRIGGER;
    public static final int R_TRIGGER;
    public static final int L_STICK_VERTICAL_AXIS;
    public static final int L_STICK_HORIZONTAL_AXIS;
    public static final int R_STICK_VERTICAL_AXIS;
    public static final int R_STICK_HORIZONTAL_AXIS;

    public static boolean isXboxController(Controller controller) {
        return controller.getName().contains("Xbox");
    }

    static {
        if (SharedLibraryLoader.isWindows) {
            A = -1;
            B = -1;
            X = -1;
            Y = -1;
            GUIDE = -1;
            L_BUMPER = -1;
            R_BUMPER = -1;
            BACK = -1;
            START = -1;
            DPAD_UP = -1;
            DPAD_DOWN = -1;
            DPAD_LEFT = -1;
            DPAD_RIGHT = -1;
            L_TRIGGER = -1;
            R_TRIGGER = -1;
            L_STICK_VERTICAL_AXIS = -1;
            L_STICK_HORIZONTAL_AXIS = -1;
            R_STICK_VERTICAL_AXIS = -1;
            R_STICK_HORIZONTAL_AXIS = -1;
        } else if (SharedLibraryLoader.isLinux) {
            A = -1;
            B = -1;
            X = -1;
            Y = -1;
            GUIDE = -1;
            L_BUMPER = -1;
            R_BUMPER = -1;
            BACK = -1;
            START = -1;
            DPAD_UP = -1;
            DPAD_DOWN = -1;
            DPAD_LEFT = -1;
            DPAD_RIGHT = -1;
            L_TRIGGER = -1;
            R_TRIGGER = -1;
            L_STICK_VERTICAL_AXIS = -1;
            L_STICK_HORIZONTAL_AXIS = -1;
            R_STICK_VERTICAL_AXIS = -1;
            R_STICK_HORIZONTAL_AXIS = -1;
        } else if (SharedLibraryLoader.isMac) {
            A = 11;
            B = 12;
            X = 13;
            Y = 14;
            GUIDE = 10;
            L_BUMPER = 8;
            R_BUMPER = 9;
            BACK = 5;
            START = 4;
            DPAD_UP = 0;
            DPAD_DOWN = 1;
            DPAD_LEFT = 2;
            DPAD_RIGHT = 3;
            L_TRIGGER = 0;
            R_TRIGGER = 1;
            L_STICK_VERTICAL_AXIS = 3;
            L_STICK_HORIZONTAL_AXIS = 2;
            R_STICK_VERTICAL_AXIS = 5;
            R_STICK_HORIZONTAL_AXIS = 4;
        } else {
            A = -1;
            B = -1;
            X = -1;
            Y = -1;
            GUIDE = -1;
            L_BUMPER = -1;
            R_BUMPER = -1;
            L_TRIGGER = -1;
            R_TRIGGER = -1;
            BACK = -1;
            START = -1;
            DPAD_UP = -1;
            DPAD_DOWN = -1;
            DPAD_LEFT = -1;
            DPAD_RIGHT = -1;
            L_STICK_VERTICAL_AXIS = -1;
            L_STICK_HORIZONTAL_AXIS = -1;
            R_STICK_VERTICAL_AXIS = -1;
            R_STICK_HORIZONTAL_AXIS = -1;
        }
    }
}


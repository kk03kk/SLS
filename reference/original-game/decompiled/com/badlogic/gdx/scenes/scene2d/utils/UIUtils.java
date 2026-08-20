/*
 * Decompiled with CFR 0.152.
 */
package com.badlogic.gdx.scenes.scene2d.utils;

import com.badlogic.gdx.Gdx;

public class UIUtils {
    public static boolean isMac = System.getProperty("os.name").contains("OS X");
    public static boolean isWindows = System.getProperty("os.name").contains("Windows");
    public static boolean isLinux = System.getProperty("os.name").contains("Linux");

    public static boolean left() {
        return Gdx.input.isButtonPressed(0);
    }

    public static boolean left(int button) {
        return button == 0;
    }

    public static boolean right() {
        return Gdx.input.isButtonPressed(1);
    }

    public static boolean right(int button) {
        return button == 1;
    }

    public static boolean middle() {
        return Gdx.input.isButtonPressed(2);
    }

    public static boolean middle(int button) {
        return button == 2;
    }

    public static boolean shift() {
        return Gdx.input.isKeyPressed(59) || Gdx.input.isKeyPressed(60);
    }

    public static boolean shift(int keycode) {
        return keycode == 59 || keycode == 60;
    }

    public static boolean ctrl() {
        if (isMac) {
            return Gdx.input.isKeyPressed(63);
        }
        return Gdx.input.isKeyPressed(129) || Gdx.input.isKeyPressed(130);
    }

    public static boolean ctrl(int keycode) {
        if (isMac) {
            return keycode == 63;
        }
        return keycode == 129 || keycode == 130;
    }

    public static boolean alt() {
        return Gdx.input.isKeyPressed(57) || Gdx.input.isKeyPressed(58);
    }

    public static boolean alt(int keycode) {
        return keycode == 57 || keycode == 58;
    }
}


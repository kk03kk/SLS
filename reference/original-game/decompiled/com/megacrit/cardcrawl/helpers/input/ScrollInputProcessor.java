/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ScrollInputProcessor
implements InputProcessor {
    private static final Logger logger = LogManager.getLogger(ScrollInputProcessor.class.getName());
    public static String lastPressed = "";
    public static String lastPressed2 = "";
    public static boolean lastPressedSwitch = true;

    public static void logLastPressed(String msg) {
        boolean bl = lastPressedSwitch = !lastPressedSwitch;
        if (lastPressedSwitch) {
            lastPressed = msg;
        } else {
            lastPressed2 = msg;
        }
        if (Settings.isInfo) {
            logger.info(msg);
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!Gdx.input.isButtonPressed(1)) {
            InputHelper.touchDown = true;
        }
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        InputHelper.touchUp = true;
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        InputHelper.isMouseDown = true;
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        if (amount == -1) {
            InputHelper.scrolledUp = true;
        } else if (amount == 1) {
            InputHelper.scrolledDown = true;
        }
        return false;
    }
}


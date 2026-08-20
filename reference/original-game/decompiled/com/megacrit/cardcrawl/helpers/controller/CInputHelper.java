/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputAction;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputListener;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;

public class CInputHelper {
    private static final Logger logger = LogManager.getLogger(CInputHelper.class.getName());
    public static Array<Controller> controllers = null;
    public static Controller controller = null;
    public static ArrayList<CInputAction> actions = new ArrayList();
    public static CInputListener listener = null;
    private static boolean initializedController = false;
    public static ControllerModel model = null;

    public static void loadSettings() {
        CInputActionSet.load();
    }

    public static void initializeIfAble() {
        if (!initializedController && Display.isActive()) {
            initializedController = true;
            logger.info("[CONTROLLER] Utilizing DirectInput");
            try {
                controllers = Controllers.getControllers();
            }
            catch (Exception e) {
                logger.info(e.getMessage());
            }
            if (controllers == null) {
                logger.info("[ERROR] getControllers() returned null");
                return;
            }
            for (int i = 0; i < CInputHelper.controllers.size; ++i) {
                logger.info("CONTROLLER[" + i + "] " + controllers.get(i).getName());
            }
            if (CInputHelper.controllers.size != 0) {
                Settings.isControllerMode = true;
                Settings.isTouchScreen = false;
                listener = new CInputListener();
                controller = controllers.first();
                controller.addListener(listener);
                if (controller.getName().contains("360")) {
                    model = ControllerModel.XBOX_360;
                    ImageMaster.loadControllerImages(ControllerModel.XBOX_360);
                } else if (controller.getName().contains("Xbox One")) {
                    model = ControllerModel.XBOX_ONE;
                    ImageMaster.loadControllerImages(ControllerModel.XBOX_ONE);
                } else {
                    model = ControllerModel.XBOX_360;
                    ImageMaster.loadControllerImages(ControllerModel.XBOX_360);
                }
            } else {
                logger.info("[CONTROLLER] No controllers detected");
            }
        }
    }

    public static void setController(Controller controllerToUse) {
        Settings.isControllerMode = true;
        Settings.isTouchScreen = false;
        controller = controllerToUse;
        controller.addListener(listener);
        Controllers.removeListener(listener);
    }

    public static void setCursor(Hitbox hb) {
        if (hb != null) {
            int y = Settings.HEIGHT - (int)hb.cY;
            if (y < 0) {
                y = 0;
            } else if (y > Settings.HEIGHT) {
                y = Settings.HEIGHT;
            }
            Gdx.input.setCursorPosition((int)hb.cX, y);
        }
    }

    public static void updateLast() {
        for (CInputAction a : actions) {
            a.justPressed = false;
            a.justReleased = false;
        }
    }

    public static boolean listenerPress(int keycode) {
        for (CInputAction a : actions) {
            if (a.keycode != keycode) continue;
            a.justPressed = true;
            a.pressed = true;
            break;
        }
        return false;
    }

    public static boolean listenerRelease(int keycode) {
        for (CInputAction a : actions) {
            if (a.keycode != keycode) continue;
            a.justReleased = true;
            a.pressed = false;
            break;
        }
        return false;
    }

    public static boolean isJustPressed(int keycode) {
        for (CInputAction a : actions) {
            if (a.keycode != keycode) continue;
            return a.justPressed;
        }
        return false;
    }

    public static boolean isJustReleased(int keycode) {
        for (CInputAction a : actions) {
            if (a.keycode != keycode) continue;
            return a.justReleased;
        }
        return false;
    }

    public static void regainInputFocus() {
        CInputListener.remapping = false;
    }

    public static boolean isTopPanelActive() {
        return AbstractDungeon.topPanel.selectPotionMode || AbstractDungeon.player.viewingRelics || !AbstractDungeon.topPanel.potionUi.isHidden;
    }

    public static enum ControllerModel {
        XBOX_360,
        XBOX_ONE,
        PS3,
        PS4,
        STEAM,
        OTHER;

    }
}


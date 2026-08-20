/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers.controller;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import java.util.Map;

public class CInputAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("InputKeyNames");
    public static final Map<String, String> TEXT_CONVERSIONS = CInputAction.uiStrings.TEXT_DICT;
    public int keycode;
    public boolean justPressed = false;
    public boolean pressed = false;
    public boolean justReleased = false;

    public CInputAction(int keycode) {
        this.keycode = keycode;
    }

    public int getKey() {
        return this.keycode;
    }

    public String getKeyString() {
        String keycodeStr = Input.Keys.toString(this.keycode);
        return TEXT_CONVERSIONS.getOrDefault(keycodeStr, keycodeStr);
    }

    public Texture getKeyImg() {
        switch (this.keycode) {
            case 0: {
                return ImageMaster.CONTROLLER_A;
            }
            case 1: {
                return ImageMaster.CONTROLLER_B;
            }
            case 2: {
                return ImageMaster.CONTROLLER_X;
            }
            case 3: {
                return ImageMaster.CONTROLLER_Y;
            }
            case 4: {
                return ImageMaster.CONTROLLER_LB;
            }
            case 5: {
                return ImageMaster.CONTROLLER_RB;
            }
            case 6: {
                return ImageMaster.CONTROLLER_BACK;
            }
            case 7: {
                return ImageMaster.CONTROLLER_START;
            }
            case 8: {
                return ImageMaster.CONTROLLER_LS;
            }
            case 9: {
                return ImageMaster.CONTROLLER_RS;
            }
            case 1000: {
                return ImageMaster.CONTROLLER_LS_DOWN;
            }
            case -1000: {
                return ImageMaster.CONTROLLER_LS_UP;
            }
            case 1001: {
                return ImageMaster.CONTROLLER_LS_RIGHT;
            }
            case -1001: {
                return ImageMaster.CONTROLLER_LS_LEFT;
            }
            case 1003: {
                return ImageMaster.CONTROLLER_RS_RIGHT;
            }
            case -1002: {
                return ImageMaster.CONTROLLER_RS_UP;
            }
            case 1002: {
                return ImageMaster.CONTROLLER_RS_DOWN;
            }
            case -1003: {
                return ImageMaster.CONTROLLER_RS_LEFT;
            }
            case 1004: {
                return ImageMaster.CONTROLLER_LT;
            }
            case -1004: {
                return ImageMaster.CONTROLLER_RT;
            }
            case 2000: {
                return ImageMaster.CONTROLLER_D_DOWN;
            }
            case -2000: {
                return ImageMaster.CONTROLLER_D_UP;
            }
            case 2001: {
                return ImageMaster.CONTROLLER_D_RIGHT;
            }
            case -2001: {
                return ImageMaster.CONTROLLER_D_LEFT;
            }
        }
        return ImageMaster.CONTROLLER_A;
    }

    public boolean isJustPressed() {
        return this.justPressed;
    }

    public void unpress() {
        this.justPressed = false;
    }

    public boolean isJustReleased() {
        return CInputHelper.isJustReleased(this.keycode);
    }

    public void remap(int newKeycode) {
        this.keycode = newKeycode;
    }

    public boolean isPressed() {
        return this.pressed;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers.controller;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.math.Vector3;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.options.RemapInputElement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CInputListener
implements ControllerListener {
    private static final Logger logger = LogManager.getLogger(CInputListener.class.getName());
    private static final float DEADZONE = 0.5f;
    private float[] axisValues = new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    public static boolean remapping = false;
    public static RemapInputElement element = null;

    public static void listenForRemap(RemapInputElement e) {
        element = e;
        remapping = true;
    }

    @Override
    public void connected(Controller controller) {
        logger.info("CONNECTED: " + controller.getName());
    }

    @Override
    public void disconnected(Controller controller) {
        logger.info("DISCONNECTED: " + controller.getName());
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        if (!Settings.CONTROLLER_ENABLED) {
            return false;
        }
        if (remapping && CInputListener.element.cAction != null) {
            if (CInputListener.element.listener.willRemapController(element, CInputListener.element.cAction.keycode, buttonCode)) {
                CInputListener.element.cAction.remap(buttonCode);
            }
            remapping = false;
            CInputListener.element.hasInputFocus = false;
            InputHelper.regainInputFocus();
            CInputHelper.regainInputFocus();
            element = null;
            return false;
        }
        if (!Settings.isControllerMode) {
            Settings.isControllerMode = true;
            Settings.isTouchScreen = false;
        }
        CInputHelper.listenerPress(buttonCode);
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        CInputHelper.listenerRelease(buttonCode);
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        if (!Settings.CONTROLLER_ENABLED) {
            return false;
        }
        if (remapping && CInputListener.element.cAction != null) {
            if (value > 0.5f && this.axisValues[axisCode] < 0.5f) {
                if (CInputListener.element.listener.willRemapController(element, CInputListener.element.cAction.keycode, 1000 + axisCode)) {
                    CInputListener.element.cAction.remap(1000 + axisCode);
                }
                remapping = false;
                CInputListener.element.hasInputFocus = false;
                InputHelper.regainInputFocus();
                CInputHelper.regainInputFocus();
                element = null;
                return false;
            }
            if (value < -0.5f && this.axisValues[axisCode] > -0.5f) {
                if (CInputListener.element.listener.willRemapController(element, CInputListener.element.cAction.keycode, -1000 - axisCode)) {
                    CInputListener.element.cAction.remap(-1000 - axisCode);
                }
                remapping = false;
                CInputListener.element.hasInputFocus = false;
                InputHelper.regainInputFocus();
                CInputHelper.regainInputFocus();
                element = null;
                return false;
            }
        }
        if (value > 0.5f && this.axisValues[axisCode] < 0.5f) {
            CInputHelper.listenerPress(1000 + axisCode);
            if (!Settings.isControllerMode) {
                Settings.isControllerMode = true;
                Settings.isTouchScreen = false;
                logger.info("Entering controller mode: AXIS moved");
            }
        } else if (value < -0.5f && this.axisValues[axisCode] > -0.5f) {
            CInputHelper.listenerPress(-1000 - axisCode);
            if (!Settings.isControllerMode) {
                Settings.isControllerMode = true;
                Settings.isTouchScreen = false;
                logger.info("Entering controller mode: AXIS moved");
            }
        }
        this.axisValues[axisCode] = value;
        return false;
    }

    @Override
    public boolean povMoved(Controller controller, int povCode, PovDirection value) {
        if (!Settings.CONTROLLER_ENABLED) {
            return false;
        }
        if (remapping && CInputListener.element.cAction != null) {
            int code = -2000;
            switch (value) {
                case north: {
                    code = -2000;
                    break;
                }
                case south: {
                    code = 2000;
                    break;
                }
                case northWest: 
                case southWest: 
                case west: {
                    code = -2001;
                    break;
                }
                case northEast: 
                case southEast: 
                case east: {
                    code = 2001;
                    break;
                }
                default: {
                    code = -2000;
                }
            }
            if (CInputListener.element.listener.willRemapController(element, CInputListener.element.cAction.keycode, code)) {
                CInputListener.element.cAction.remap(code);
            }
            remapping = false;
            CInputListener.element.hasInputFocus = false;
            InputHelper.regainInputFocus();
            CInputHelper.regainInputFocus();
            element = null;
            return false;
        }
        if (!Settings.isControllerMode) {
            Settings.isControllerMode = true;
            Settings.isTouchScreen = false;
            logger.info("Entering controller mode: D-Pad pressed");
        }
        switch (value) {
            case north: {
                CInputHelper.listenerPress(-2000);
                break;
            }
            case south: {
                CInputHelper.listenerPress(2000);
                break;
            }
            case northWest: 
            case southWest: 
            case west: {
                CInputHelper.listenerPress(-2001);
                break;
            }
            case northEast: 
            case southEast: 
            case east: {
                CInputHelper.listenerPress(2001);
                break;
            }
        }
        return false;
    }

    @Override
    public boolean xSliderMoved(Controller controller, int sliderCode, boolean value) {
        return false;
    }

    @Override
    public boolean ySliderMoved(Controller controller, int sliderCode, boolean value) {
        return false;
    }

    @Override
    public boolean accelerometerMoved(Controller controller, int accelerometerCode, Vector3 value) {
        return false;
    }
}


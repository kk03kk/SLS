/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers.input;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.helpers.input.ScrollInputProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InputHelper {
    private static final Logger logger = LogManager.getLogger(InputHelper.class.getName());
    public static int mX;
    public static int mY;
    public static boolean isMouseDown;
    public static boolean isMouseDown_R;
    private static boolean isPrevMouseDown;
    private static boolean isPrevMouseDown_R;
    public static boolean justClickedLeft;
    public static boolean justClickedRight;
    public static boolean touchDown;
    public static boolean touchUp;
    public static boolean justReleasedClickLeft;
    public static boolean justReleasedClickRight;
    public static boolean scrolledUp;
    public static boolean scrolledDown;
    public static boolean pressedEscape;
    private static ScrollInputProcessor processor;
    public static int scrollY;
    private static boolean ignoreOneCycle;
    public static final int[] SHORTCUT_MODIFIER_KEYS;

    public static void initialize() {
        processor = new ScrollInputProcessor();
        Gdx.input.setInputProcessor(processor);
        logger.info("Setting input processor to Scroller");
        InputActionSet.load();
    }

    public static void regainInputFocus() {
        Gdx.input.setInputProcessor(processor);
        ignoreOneCycle = true;
    }

    public static void updateFirst() {
        if (ignoreOneCycle) {
            ignoreOneCycle = false;
            return;
        }
        if (!Settings.isTouchScreen) {
            mX = Gdx.input.getX();
            if (mX > Settings.WIDTH) {
                mX = Settings.WIDTH;
            } else if (mX < 0) {
                mX = 0;
            }
            mY = Settings.HEIGHT - Gdx.input.getY();
            if (mY > Settings.HEIGHT) {
                mY = Settings.HEIGHT;
            } else if (mY < 1) {
                mY = 1;
            }
        } else {
            mX = Gdx.input.getX() + Settings.VERT_LETTERBOX_AMT;
            mY = Settings.HEIGHT - Gdx.input.getY() + Settings.HORIZ_LETTERBOX_AMT;
            if (mY < 1) {
                mY = 1;
            }
        }
        isMouseDown = Gdx.input.isButtonPressed(0);
        isMouseDown_R = Gdx.input.isButtonPressed(1);
        if (Gdx.input.getDeltaX() != 0 && AbstractDungeon.player != null && AbstractDungeon.player.isInKeyboardMode) {
            GameCursor.hidden = false;
        }
        if (!isPrevMouseDown && isMouseDown || touchDown) {
            CardCrawlGame.cursor.color.a = 0.7f;
            touchDown = false;
            justClickedLeft = true;
            if (Settings.isControllerMode) {
                InputHelper.leaveControllerMode();
            }
            if (Settings.isDebug) {
                logger.info("Clicked: (" + mX + "," + mY + ")");
            }
        } else if (isPrevMouseDown && !isMouseDown || touchUp) {
            touchUp = false;
            justReleasedClickLeft = true;
        }
        if (!isPrevMouseDown_R && isMouseDown_R) {
            justClickedRight = true;
            if (Settings.isControllerMode) {
                InputHelper.leaveControllerMode();
            }
        } else if (isPrevMouseDown_R && !isMouseDown_R) {
            justReleasedClickRight = true;
        }
        pressedEscape = InputActionSet.cancel.isJustPressed();
        isPrevMouseDown_R = isMouseDown_R;
        isPrevMouseDown = isMouseDown;
    }

    private static void leaveControllerMode() {
        if (Settings.isConsoleBuild) {
            logger.info("ENTERING TOUCH SCREEN MODE");
            Settings.isTouchScreen = true;
        } else {
            logger.info("LEAVING CONTROLLER MODE");
            Settings.isTouchScreen = Settings.TOUCHSCREEN_ENABLED;
        }
        Settings.isControllerMode = false;
        GameCursor.hidden = false;
        if (AbstractDungeon.player != null && AbstractDungeon.isPlayerInDungeon()) {
            AbstractDungeon.player.viewingRelics = false;
            AbstractDungeon.topPanel.selectPotionMode = false;
            AbstractDungeon.player.releaseCard();
        }
    }

    public static void updateLast() {
        justClickedLeft = false;
        justClickedRight = false;
        justReleasedClickLeft = false;
        justReleasedClickRight = false;
        scrolledUp = false;
        scrolledDown = false;
    }

    public static AbstractCard getCardSelectedByHotkey(CardGroup cards) {
        for (int i = 0; i < InputActionSet.selectCardActions.length && i < cards.size(); ++i) {
            if (!InputActionSet.selectCardActions[i].isJustPressed()) continue;
            return cards.group.get(i);
        }
        return null;
    }

    public static boolean isShortcutModifierKeyPressed() {
        for (int keycode : SHORTCUT_MODIFIER_KEYS) {
            if (!Gdx.input.isKeyPressed(keycode)) continue;
            return true;
        }
        return false;
    }

    public static boolean isPasteJustPressed() {
        return InputHelper.isShortcutModifierKeyPressed() && Gdx.input.isKeyJustPressed(50);
    }

    public static boolean didMoveMouse() {
        return Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0;
    }

    public static void moveCursorToNeutralPosition() {
        if (Settings.isTouchScreen && !Settings.isControllerMode) {
            Gdx.input.setCursorPosition(10, Settings.HEIGHT / 2);
            CardCrawlGame.cursor.color.a = 0.0f;
        }
    }

    static {
        isMouseDown = false;
        isMouseDown_R = false;
        isPrevMouseDown = false;
        isPrevMouseDown_R = false;
        justClickedLeft = false;
        justClickedRight = false;
        touchDown = false;
        touchUp = false;
        justReleasedClickLeft = false;
        justReleasedClickRight = false;
        scrolledUp = false;
        scrolledDown = false;
        pressedEscape = false;
        scrollY = 0;
        ignoreOneCycle = false;
        SHORTCUT_MODIFIER_KEYS = new int[]{129, 130, 63};
    }
}


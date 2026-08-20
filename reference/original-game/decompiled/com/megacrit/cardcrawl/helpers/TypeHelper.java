/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.helpers;

import com.badlogic.gdx.InputProcessor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.SeedHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.ui.panels.RenamePopup;
import com.megacrit.cardcrawl.ui.panels.SeedPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TypeHelper
implements InputProcessor {
    private static final Logger logger = LogManager.getLogger(TypeHelper.class.getName());
    private boolean seed;

    public TypeHelper() {
        this.seed = false;
    }

    public TypeHelper(boolean seed) {
        this.seed = seed;
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
        String charStr = String.valueOf(character);
        logger.info(charStr);
        if (charStr.length() != 1) {
            return false;
        }
        if (this.seed) {
            if (SeedPanel.isFull()) {
                return false;
            }
            if (InputHelper.isPasteJustPressed()) {
                return false;
            }
            String converted = SeedHelper.getValidCharacter(charStr, SeedPanel.textField);
            if (converted != null) {
                SeedPanel.textField = SeedPanel.textField + converted;
            }
        } else {
            if (FontHelper.getSmartWidth(FontHelper.cardTitleFont, RenamePopup.textField, 1.0E7f, 0.0f, 0.82f) >= 240.0f * Settings.scale) {
                return false;
            }
            if (Character.isDigit(character) || Character.isLetter(character)) {
                RenamePopup.textField = RenamePopup.textField + charStr;
            }
        }
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}


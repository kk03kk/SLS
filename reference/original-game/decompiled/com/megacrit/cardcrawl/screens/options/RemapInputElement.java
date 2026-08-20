/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.options;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.HitboxListener;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputAction;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputListener;
import com.megacrit.cardcrawl.helpers.input.InputAction;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.options.RemapInputElementListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemapInputElement
implements HitboxListener,
InputProcessor {
    private static final Logger logger = LogManager.getLogger(RemapInputElement.class.getName());
    public Hitbox hb = new Hitbox(ROW_WIDTH, ROW_HEIGHT);
    public InputAction action;
    public CInputAction cAction;
    public boolean hasInputFocus = false;
    public boolean isHidden = false;
    public boolean isHeader = false;
    private String labelText;
    public RemapInputElementListener listener;
    public static final float ROW_HEIGHT = 58.0f * Settings.scale;
    public static final float ROW_WIDTH = 1048.0f * Settings.scale;
    public static final float ROW_RENDER_HEIGHT = 64.0f * Settings.scale;
    private static final float ROW_TEXT_Y_OFFSET = 12.0f * Settings.scale;
    private static final float ROW_TEXT_LEADING_OFFSET = 40.0f * Settings.scale;
    public static final float KEYBOARD_COLUMN_X_OFFSET = Settings.scale * 400.0f;
    public static final float CONTROLLER_COLUMN_X_OFFSET = Settings.scale * 250.0f;
    private static final Color ROW_BG_COLOR = new Color(588124159);
    private static final Color ROW_HOVER_COLOR = new Color(-193);
    private static final Color ROW_SELECT_COLOR = new Color(-1924910337);
    private static final Color TEXT_DEFAULT_COLOR = Settings.CREAM_COLOR;
    private static final Color TEXT_FOCUSED_COLOR = Settings.GREEN_TEXT_COLOR;
    private static final Color TEXT_HOVERED_COLOR = Settings.GOLD_COLOR;

    public RemapInputElement(RemapInputElementListener listener, String label, InputAction action) {
        this(listener, label, action, null);
    }

    public RemapInputElement(RemapInputElementListener listener, String label, InputAction action, CInputAction controllerAction) {
        this.labelText = label;
        this.action = action;
        this.cAction = controllerAction;
        this.listener = listener;
    }

    public void move(float x, float y) {
        this.hb.move(x, y);
    }

    public void update() {
        if (this.isHidden) {
            return;
        }
        this.hb.encapsulatedUpdate(this);
        if (this.hasInputFocus && (InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed())) {
            CInputActionSet.select.unpress();
            logger.info("Lose focus");
            this.hasInputFocus = false;
            InputHelper.regainInputFocus();
            CInputHelper.regainInputFocus();
        }
    }

    public void render(SpriteBatch sb) {
        if (this.isHidden) {
            return;
        }
        sb.setBlendFunction(770, 1);
        sb.setColor(this.getRowBgColor());
        sb.draw(ImageMaster.INPUT_SETTINGS_ROW, this.hb.x, this.hb.y, ROW_WIDTH, ROW_RENDER_HEIGHT);
        sb.setBlendFunction(770, 771);
        sb.setColor(Color.WHITE);
        Color textColor = this.getTextColor();
        float textY = this.hb.cY + ROW_TEXT_Y_OFFSET;
        float textX = this.hb.x + ROW_TEXT_LEADING_OFFSET;
        FontHelper.renderFont(sb, FontHelper.topPanelInfoFont, this.labelText, textX, textY, textColor);
        FontHelper.renderFont(sb, FontHelper.topPanelInfoFont, this.getKeyColumnText(), textX += KEYBOARD_COLUMN_X_OFFSET, textY, textColor);
        textX += CONTROLLER_COLUMN_X_OFFSET;
        if (!this.isHeader) {
            Texture img = this.getControllerColumnImg();
            if (img != null) {
                sb.draw(img, textX - 32.0f, textY - 32.0f - 10.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 0.75f, Settings.scale * 0.75f, 0.0f, 0, 0, 64, 64, false, false);
            }
        } else {
            FontHelper.renderFont(sb, FontHelper.topPanelInfoFont, this.getControllerColumnText(), textX, textY, textColor);
        }
        this.hb.render(sb);
    }

    protected Color getRowBgColor() {
        Color bgColor = ROW_BG_COLOR;
        if (this.hasInputFocus) {
            bgColor = ROW_SELECT_COLOR;
        } else if (this.hb.hovered) {
            bgColor = ROW_HOVER_COLOR;
        }
        return bgColor;
    }

    protected Color getTextColor() {
        Color color = TEXT_DEFAULT_COLOR;
        if (this.hasInputFocus) {
            color = TEXT_FOCUSED_COLOR;
        } else if (this.hb.hovered) {
            color = TEXT_HOVERED_COLOR;
        }
        return color;
    }

    protected String getKeyColumnText() {
        return this.action != null ? this.action.getKeyString() : "";
    }

    protected String getControllerColumnText() {
        return this.cAction != null ? this.cAction.getKeyString() : "";
    }

    protected Texture getControllerColumnImg() {
        Texture retVal = null;
        if (this.cAction != null) {
            retVal = this.cAction.getKeyImg();
        }
        return retVal;
    }

    @Override
    public void hoverStarted(Hitbox hitbox) {
        CardCrawlGame.sound.play("UI_HOVER");
    }

    @Override
    public void startClicking(Hitbox hitbox) {
        CardCrawlGame.sound.play("UI_CLICK_1");
    }

    @Override
    public void clicked(Hitbox hitbox) {
        logger.info("BEGIN REMAPPING...");
        this.listener.didStartRemapping(this);
        Gdx.input.setInputProcessor(this);
        CInputListener.listenForRemap(this);
        this.hasInputFocus = true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (this.action != null && this.listener.willRemap(this, this.action.getKey(), keycode)) {
            this.action.remap(keycode);
        }
        this.hasInputFocus = false;
        InputHelper.regainInputFocus();
        CInputHelper.regainInputFocus();
        return true;
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
        if (amount == -1) {
            InputHelper.scrolledUp = true;
        } else if (amount == 1) {
            InputHelper.scrolledDown = true;
        }
        return false;
    }
}


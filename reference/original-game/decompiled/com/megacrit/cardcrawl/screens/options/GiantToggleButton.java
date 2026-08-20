/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.options;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GiantToggleButton {
    private static final Logger logger = LogManager.getLogger(GiantToggleButton.class.getName());
    public boolean ticked = false;
    public ToggleType type;
    private String label;
    Hitbox hb = new Hitbox(320.0f * Settings.scale, 72.0f * Settings.scale);
    private float x;
    private float y;
    private float scale = Settings.scale;

    public GiantToggleButton(ToggleType type, float x, float y, String label) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.label = label;
        this.hb.move(x + 110.0f * Settings.scale, y);
        this.initialize();
    }

    private void initialize() {
        switch (this.type) {
            case CONTROLLER_ENABLED: {
                this.ticked = Settings.gamePref.getBoolean("Controller Enabled", true);
                break;
            }
            case TOUCHSCREEN_ENABLED: {
                this.ticked = Settings.gamePref.getBoolean("Touchscreen Enabled", false);
                break;
            }
            default: {
                logger.info(this.type.name() + " not found (initialize())");
            }
        }
    }

    public void update() {
        this.hb.update();
        if (this.hb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        }
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            this.hb.clickStarted = true;
            CardCrawlGame.sound.play("UI_CLICK_1");
        } else if (this.hb.clicked || this.hb.hovered && CInputActionSet.select.isJustPressed()) {
            CInputActionSet.select.unpress();
            this.hb.clicked = false;
            this.ticked = !this.ticked;
            this.useEffect();
        }
        this.scale = this.hb.hovered ? Settings.scale * 1.125f : Settings.scale;
    }

    private void useEffect() {
        switch (this.type) {
            case CONTROLLER_ENABLED: {
                Settings.gamePref.putBoolean("Controller Enabled", this.ticked);
                Settings.gamePref.flush();
                Settings.CONTROLLER_ENABLED = this.ticked;
                break;
            }
            case TOUCHSCREEN_ENABLED: {
                Settings.gamePref.putBoolean("Touchscreen Enabled", this.ticked);
                Settings.gamePref.flush();
                Settings.TOUCHSCREEN_ENABLED = this.ticked;
                Settings.isTouchScreen = this.ticked;
                break;
            }
            default: {
                logger.info(this.type.name() + " not found (useEffect())");
            }
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.CHECKBOX, this.x - 32.0f, this.y - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, 0.0f, 0, 0, 64, 64, false, false);
        if (this.ticked) {
            sb.draw(ImageMaster.TICK, this.x - 32.0f, this.y - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.scale, this.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        if (this.hb.hovered) {
            FontHelper.renderFontLeft(sb, FontHelper.panelEndTurnFont, this.label, this.x + 40.0f * Settings.scale, this.y, Settings.GREEN_TEXT_COLOR);
        } else {
            FontHelper.renderFontLeft(sb, FontHelper.panelEndTurnFont, this.label, this.x + 40.0f * Settings.scale, this.y, Settings.CREAM_COLOR);
        }
        this.hb.render(sb);
    }

    public static enum ToggleType {
        CONTROLLER_ENABLED,
        TOUCHSCREEN_ENABLED;

    }
}


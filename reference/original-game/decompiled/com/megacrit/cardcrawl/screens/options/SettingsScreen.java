/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.options;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.options.ConfirmPopup;
import com.megacrit.cardcrawl.screens.options.OptionsPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SettingsScreen {
    private static final Logger logger = LogManager.getLogger(SettingsScreen.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("SettingsScreen");
    public static final String[] TEXT = SettingsScreen.uiStrings.TEXT;
    public OptionsPanel panel = new OptionsPanel();
    public ConfirmPopup exitPopup = new ConfirmPopup(TEXT[0], TEXT[1], ConfirmPopup.ConfirmType.EXIT);
    public ConfirmPopup abandonPopup = new ConfirmPopup(TEXT[0], TEXT[2], ConfirmPopup.ConfirmType.ABANDON_MID_RUN);
    private static final String NOT_SAVED_MSG = TEXT[3];

    public void update() {
        if (!this.exitPopup.shown && !this.abandonPopup.shown) {
            this.panel.update();
        }
        this.exitPopup.update();
        this.abandonPopup.update();
    }

    public void open() {
        this.open(true);
    }

    public void open(boolean animated) {
        AbstractDungeon.player.releaseCard();
        this.panel.refresh();
        if (animated) {
            AbstractDungeon.overlayMenu.cancelButton.show(TEXT[4]);
        } else {
            AbstractDungeon.overlayMenu.cancelButton.showInstantly(TEXT[4]);
        }
        CardCrawlGame.sound.play("UI_CLICK_1");
        AbstractDungeon.isScreenUp = true;
        AbstractDungeon.overlayMenu.showBlackScreen();
        AbstractDungeon.overlayMenu.proceedButton.hide();
        AbstractDungeon.overlayMenu.hideCombatPanels();
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.SETTINGS;
    }

    public void render(SpriteBatch sb) {
        this.panel.render(sb);
        this.exitPopup.render(sb);
        this.abandonPopup.render(sb);
    }

    public void popup(ConfirmPopup.ConfirmType type) {
        if (AbstractDungeon.overlayMenu != null) {
            AbstractDungeon.overlayMenu.cancelButton.hide();
        }
        switch (type) {
            case ABANDON_MID_RUN: {
                this.abandonPopup.show();
                break;
            }
            case EXIT: {
                this.exitPopup.desc = TEXT[1];
                if (AbstractDungeon.player != null && !AbstractDungeon.player.saveFileExists()) {
                    this.exitPopup.desc = NOT_SAVED_MSG;
                }
                this.exitPopup.show();
                break;
            }
            default: {
                logger.info("Unspecified case: " + type.name());
            }
        }
    }
}


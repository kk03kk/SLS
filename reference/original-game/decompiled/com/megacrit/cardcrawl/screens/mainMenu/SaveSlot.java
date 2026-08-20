/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.mainMenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.stats.CharStat;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SaveSlot {
    private static final Logger logger = LogManager.getLogger(SaveSlot.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("SaveSlot");
    public static final String[] TEXT = SaveSlot.uiStrings.TEXT;
    private String name = "";
    private long playtime = 0L;
    private float completionPercentage = 0.0f;
    public boolean emptySlot = false;
    private int index = 0;
    private Texture iconImg = null;
    public Hitbox slotHb;
    public Hitbox deleteHb;
    public Hitbox renameHb;
    public static Color uiColor = null;

    public SaveSlot(int slot) {
        if (slot == CardCrawlGame.saveSlot && !CardCrawlGame.playerName.equals(CardCrawlGame.saveSlotPref.getString(SaveHelper.slotName("PROFILE_NAME", slot), ""))) {
            logger.info("Migrating from legacy save.");
            this.migration(slot);
        }
        this.name = CardCrawlGame.saveSlotPref.getString(SaveHelper.slotName("PROFILE_NAME", slot), "");
        if (this.name.equals("")) {
            this.emptySlot = true;
        }
        if (!this.emptySlot) {
            this.loadInfo(slot);
        }
        this.index = slot;
        this.slotHb = new Hitbox(800.0f * Settings.scale, 260.0f * Settings.scale);
        this.slotHb.move((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f + 280.0f * Settings.scale - (float)this.index * 280.0f * Settings.scale);
        this.renameHb = new Hitbox(90.0f * Settings.scale, 90.0f * Settings.scale);
        this.renameHb.move(1400.0f * Settings.xScale, this.slotHb.cY + 44.0f * Settings.scale);
        this.deleteHb = new Hitbox(90.0f * Settings.scale, 90.0f * Settings.scale);
        this.deleteHb.move(1400.0f * Settings.xScale, this.renameHb.cY - 91.0f * Settings.scale);
        switch (this.index) {
            case 0: {
                this.iconImg = ImageMaster.PROFILE_A;
                break;
            }
            case 1: {
                this.iconImg = ImageMaster.PROFILE_B;
                break;
            }
            case 2: {
                this.iconImg = ImageMaster.PROFILE_C;
                break;
            }
        }
    }

    private void loadInfo(int slot) {
        this.completionPercentage = CardCrawlGame.saveSlotPref.getFloat(SaveHelper.slotName("COMPLETION", slot), 0.0f);
        this.playtime = CardCrawlGame.saveSlotPref.getLong(SaveHelper.slotName("PLAYTIME", slot), 0L);
    }

    private void migration(int slot) {
        CardCrawlGame.saveSlotPref.putString(SaveHelper.slotName("PROFILE_NAME", slot), CardCrawlGame.playerName);
        CardCrawlGame.saveSlotPref.putFloat(SaveHelper.slotName("COMPLETION", slot), UnlockTracker.getCompletionPercentage());
        this.completionPercentage = CardCrawlGame.saveSlotPref.getFloat(SaveHelper.slotName("COMPLETION", slot), 0.0f);
        CardCrawlGame.saveSlotPref.putLong(SaveHelper.slotName("PLAYTIME", slot), UnlockTracker.getTotalPlaytime());
        this.playtime = CardCrawlGame.saveSlotPref.getLong(SaveHelper.slotName("PLAYTIME", slot), 0L);
        CardCrawlGame.saveSlotPref.flush();
    }

    public void update() {
        if (!this.emptySlot) {
            this.deleteHb.update();
            this.renameHb.update();
            if (this.slotHb.hovered && CInputActionSet.topPanel.isJustPressed()) {
                CInputActionSet.topPanel.unpress();
                this.deleteHb.clicked = true;
                this.deleteHb.hovered = true;
            }
            if (this.slotHb.hovered && CInputActionSet.proceed.isJustPressed()) {
                CInputActionSet.proceed.unpress();
                this.renameHb.clicked = true;
                this.renameHb.hovered = true;
            }
        }
        if (!this.deleteHb.hovered && !this.renameHb.hovered) {
            this.slotHb.update();
            if (this.slotHb.hovered && CInputActionSet.select.isJustPressed()) {
                this.slotHb.clicked = true;
            } else if (this.slotHb.justHovered) {
                CardCrawlGame.sound.play("UI_HOVER");
            }
            if (this.slotHb.hovered && InputHelper.justClickedLeft) {
                this.slotHb.clickStarted = true;
                CardCrawlGame.sound.play("UI_CLICK_1");
            } else if (this.slotHb.clicked) {
                this.slotHb.clicked = false;
                CardCrawlGame.saveSlot = this.index;
                if (this.name.equals("")) {
                    CardCrawlGame.mainMenuScreen.saveSlotScreen.openRenamePopup(this.index, true);
                } else {
                    CardCrawlGame.mainMenuScreen.saveSlotScreen.confirm(this.index);
                }
            }
        } else {
            this.slotHb.unhover();
            if (this.deleteHb.justHovered) {
                CardCrawlGame.sound.play("UI_HOVER");
            } else if (this.deleteHb.hovered && InputHelper.justClickedLeft) {
                this.deleteHb.clickStarted = true;
                CardCrawlGame.sound.play("UI_CLICK_1");
            } else if (this.deleteHb.clicked) {
                this.deleteHb.clicked = false;
                CardCrawlGame.mainMenuScreen.saveSlotScreen.openDeletePopup(this.index);
            }
            if (this.renameHb.justHovered) {
                CardCrawlGame.sound.play("UI_HOVER");
            } else if (this.renameHb.hovered && InputHelper.justClickedLeft) {
                this.renameHb.clickStarted = true;
                CardCrawlGame.sound.play("UI_CLICK_1");
            } else if (this.renameHb.clicked) {
                this.renameHb.clicked = false;
                CardCrawlGame.mainMenuScreen.saveSlotScreen.openRenamePopup(this.index, false);
            }
        }
    }

    public void render(SpriteBatch sb) {
        this.renderSlotImage(sb);
        this.renderText(sb);
        if (!this.emptySlot) {
            this.renderDeleteIcon(sb);
            this.renderRenameIcon(sb);
        }
        this.renderHbs(sb);
    }

    private void renderText(SpriteBatch sb) {
        Color c = Settings.GOLD_COLOR.cpy();
        c.a = SaveSlot.uiColor.a;
        if (!this.emptySlot) {
            FontHelper.renderFontLeft(sb, FontHelper.buttonLabelFont, this.name, 740.0f * Settings.xScale, this.slotHb.cY + 26.0f * Settings.scale, c);
            FontHelper.renderFontLeft(sb, FontHelper.charDescFont, TEXT[0] + CharStat.formatHMSM((int)this.playtime), 740.0f * Settings.xScale, this.slotHb.cY - 24.0f * Settings.scale, uiColor);
            if (this.completionPercentage == 100.0f) {
                FontHelper.renderFontCentered(sb, FontHelper.charTitleFont, "100%", 1200.0f * Settings.xScale, this.slotHb.cY + 0.0f * Settings.scale, c);
            } else if (this.completionPercentage == 0.0f) {
                FontHelper.renderFontCentered(sb, FontHelper.charTitleFont, "0%", 1200.0f * Settings.xScale, this.slotHb.cY + 0.0f * Settings.scale, c);
            } else {
                FontHelper.renderFontCentered(sb, FontHelper.charTitleFont, String.format("%.1f", Float.valueOf(this.completionPercentage)) + "%", 1200.0f * Settings.xScale, this.slotHb.cY + 0.0f * Settings.scale, c);
            }
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[1], (float)Settings.WIDTH / 2.0f, this.slotHb.cY, c);
        }
    }

    private void renderSlotImage(SpriteBatch sb) {
        float scale;
        float f = scale = Settings.xScale > Settings.yScale ? Settings.xScale : Settings.yScale;
        if (this.slotHb.hovered) {
            scale *= 1.02f;
        }
        sb.draw(ImageMaster.PROFILE_SLOT, this.slotHb.cX - 400.0f, this.slotHb.cY - 130.0f, 400.0f, 130.0f, 800.0f, 260.0f, scale, scale * 0.9f, 0.0f, 0, 0, 800, 260, false, false);
        sb.draw(this.iconImg, this.slotHb.cX - 290.0f * Settings.xScale - 50.0f, this.slotHb.cY - 50.0f, 50.0f, 50.0f, 100.0f, 100.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 100, 100, false, false);
        if (this.slotHb.hovered) {
            sb.setColor(Settings.HALF_TRANSPARENT_WHITE_COLOR);
            sb.setBlendFunction(770, 1);
            sb.draw(ImageMaster.PROFILE_SLOT, this.slotHb.cX - 400.0f, this.slotHb.cY - 130.0f, 400.0f, 130.0f, 800.0f, 260.0f, scale, scale * 0.9f, 0.0f, 0, 0, 800, 260, false, false);
            sb.setBlendFunction(770, 771);
            sb.setColor(uiColor);
        }
    }

    private void renderHbs(SpriteBatch sb) {
        if (CardCrawlGame.mainMenuScreen.saveSlotScreen.shown) {
            this.slotHb.render(sb);
            this.deleteHb.render(sb);
            this.renameHb.render(sb);
        }
    }

    private void renderDeleteIcon(SpriteBatch sb) {
        float scale = Settings.scale;
        if (this.deleteHb.hovered) {
            scale = Settings.scale * 1.04f;
        }
        sb.draw(ImageMaster.PROFILE_DELETE, this.deleteHb.cX - 50.0f, this.deleteHb.cY - 50.0f, 50.0f, 50.0f, 100.0f, 100.0f, scale, scale, 0.0f, 0, 0, 100, 100, false, false);
        if (this.deleteHb.hovered) {
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.4f));
            sb.setBlendFunction(770, 1);
            sb.draw(ImageMaster.PROFILE_DELETE, this.deleteHb.cX - 50.0f, this.deleteHb.cY - 50.0f, 50.0f, 50.0f, 100.0f, 100.0f, scale, scale, 0.0f, 0, 0, 100, 100, false, false);
            sb.setBlendFunction(770, 771);
            sb.setColor(uiColor);
        }
        if (this.slotHb.hovered && Settings.isControllerMode) {
            sb.draw(CInputActionSet.topPanel.getKeyImg(), this.deleteHb.cX - 32.0f + 82.0f * Settings.scale, this.deleteHb.cY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
    }

    private void renderRenameIcon(SpriteBatch sb) {
        float scale = Settings.scale;
        if (this.renameHb.hovered) {
            scale = Settings.scale * 1.04f;
        }
        sb.draw(ImageMaster.PROFILE_RENAME, this.renameHb.cX - 50.0f, this.renameHb.cY - 50.0f, 50.0f, 50.0f, 100.0f, 100.0f, scale, scale, 0.0f, 0, 0, 100, 100, false, false);
        if (this.renameHb.hovered) {
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.4f));
            sb.setBlendFunction(770, 1);
            sb.draw(ImageMaster.PROFILE_RENAME, this.renameHb.cX - 50.0f, this.renameHb.cY - 50.0f, 50.0f, 50.0f, 100.0f, 100.0f, scale, scale, 0.0f, 0, 0, 100, 100, false, false);
            sb.setBlendFunction(770, 771);
            sb.setColor(uiColor);
        }
        if (this.slotHb.hovered && Settings.isControllerMode) {
            sb.draw(CInputActionSet.proceed.getKeyImg(), this.renameHb.cX - 32.0f + 82.0f * Settings.scale, this.renameHb.cY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
    }

    public void clear() {
        this.name = "";
        this.emptySlot = true;
        this.completionPercentage = 0.0f;
        this.playtime = 0L;
        this.deleteHb.unhover();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String setName) {
        this.name = setName;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.mainMenu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.mainMenu.SaveSlot;
import com.megacrit.cardcrawl.ui.panels.DeleteSaveConfirmPopup;
import com.megacrit.cardcrawl.ui.panels.RenamePopup;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SaveSlotScreen {
    private static final Logger logger = LogManager.getLogger(SaveSlotScreen.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("SaveSlotScreen");
    public static final String[] TEXT = SaveSlotScreen.uiStrings.TEXT;
    public static boolean slotDeleted = false;
    private Color screenColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    public Color uiColor = new Color(1.0f, 0.965f, 0.886f, 0.0f);
    public boolean shown = false;
    public ArrayList<SaveSlot> slots = new ArrayList();
    public MenuCancelButton cancelButton = new MenuCancelButton();
    private RenamePopup renamePopup = new RenamePopup();
    private DeleteSaveConfirmPopup deletePopup = new DeleteSaveConfirmPopup();
    public CurrentPopup curPop = CurrentPopup.NONE;

    public void update() {
        this.deletePopup.update();
        this.renamePopup.update();
        this.updateColors();
        switch (this.curPop) {
            case DELETE: {
                break;
            }
            case NONE: {
                if (this.shown) {
                    this.updateSaveSlots();
                    this.updateControllerInput();
                }
                this.updateCancelButton();
                break;
            }
            case RENAME: {
                break;
            }
        }
    }

    private void updateCancelButton() {
        this.cancelButton.update();
        if (this.cancelButton.hb.clicked || !this.cancelButton.isHidden && InputActionSet.cancel.isJustPressed()) {
            this.cancelButton.hb.clicked = false;
            if (!this.slots.get((int)CardCrawlGame.saveSlot).emptySlot) {
                this.confirm(CardCrawlGame.saveSlot);
            } else {
                for (int i = 0; i < 3; ++i) {
                    if (this.slots.get((int)i).emptySlot) continue;
                    this.confirm(i);
                }
            }
        }
    }

    private void updateControllerInput() {
        if (!Settings.isControllerMode || this.slots.isEmpty()) {
            return;
        }
        boolean anyHovered = false;
        int index = 0;
        for (SaveSlot slot : this.slots) {
            if (slot.slotHb.hovered) {
                anyHovered = true;
                break;
            }
            ++index;
        }
        if (!anyHovered) {
            CInputHelper.setCursor(this.slots.get((int)0).slotHb);
        } else {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                if (--index < 0) {
                    index = 2;
                }
            } else if ((CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) && ++index > 2) {
                index = 0;
            }
            CInputHelper.setCursor(this.slots.get((int)index).slotHb);
        }
    }

    private void updateColors() {
        if (this.shown) {
            this.screenColor.a = MathHelper.fadeLerpSnap(this.screenColor.a, 0.75f);
            this.uiColor.a = MathHelper.fadeLerpSnap(this.uiColor.a, 1.0f);
        } else {
            this.screenColor.a = MathHelper.fadeLerpSnap(this.screenColor.a, 0.0f);
            this.uiColor.a = MathHelper.fadeLerpSnap(this.uiColor.a, 0.0f);
        }
    }

    private void updateSaveSlots() {
        for (SaveSlot slot : this.slots) {
            slot.update();
        }
    }

    public void open(String curName) {
        if (this.slots.isEmpty()) {
            this.slots.add(new SaveSlot(0));
            this.slots.add(new SaveSlot(1));
            this.slots.add(new SaveSlot(2));
            SaveSlot.uiColor = this.uiColor;
        }
        this.shown = true;
        for (SaveSlot s : this.slots) {
            if (s.emptySlot) continue;
            this.cancelButton.show(CharacterSelectScreen.TEXT[5]);
            break;
        }
    }

    public void rename(int slot, String name) {
        this.slots.get(slot).setName(name);
    }

    public void confirm(int slot) {
        this.shown = false;
        CardCrawlGame.saveSlot = slot;
        CardCrawlGame.playerName = this.slots.get(slot).getName();
        CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
        this.cancelButton.hide();
        if (CardCrawlGame.saveSlotPref.getInteger("DEFAULT_SLOT", -1) != slot || slotDeleted) {
            logger.info("Default slot updated: " + slot);
            CardCrawlGame.saveSlotPref.putInteger("DEFAULT_SLOT", slot);
            CardCrawlGame.reloadPrefs();
            CardCrawlGame.saveSlotPref.flush();
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.screenColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        if (this.shown) {
            this.renderSaveSlots(sb);
            this.deletePopup.render(sb);
            this.renamePopup.render(sb);
            if (this.curPop == CurrentPopup.NONE) {
                this.cancelButton.render(sb);
                this.renderSelectSlotMessage(sb);
            }
        }
    }

    private void renderSelectSlotMessage(SpriteBatch sb) {
        boolean showingTip = false;
        for (SaveSlot s : this.slots) {
            if (s.renameHb.hovered) {
                FontHelper.renderFontCentered(sb, FontHelper.topPanelAmountFont, TEXT[1], (float)Settings.WIDTH / 2.0f, 80.0f * Settings.scale, Settings.BLUE_TEXT_COLOR);
                showingTip = true;
                break;
            }
            if (!s.deleteHb.hovered) continue;
            FontHelper.renderFontCentered(sb, FontHelper.topPanelAmountFont, TEXT[2], (float)Settings.WIDTH / 2.0f, 80.0f * Settings.scale, Settings.RED_TEXT_COLOR);
            showingTip = true;
            break;
        }
        if (!showingTip) {
            FontHelper.renderFontCentered(sb, FontHelper.topPanelAmountFont, TEXT[0], (float)Settings.WIDTH / 2.0f, 80.0f * Settings.scale, Settings.CREAM_COLOR);
        }
        if (Settings.isControllerMode) {
            sb.setColor(Color.WHITE);
            sb.draw(CInputActionSet.select.getKeyImg(), (float)Settings.WIDTH / 2.0f - FontHelper.getSmartWidth(FontHelper.topPanelAmountFont, TEXT[0], 99999.0f, 0.0f) / 2.0f - 32.0f - 48.0f * Settings.scale, 80.0f * Settings.scale - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
    }

    private void renderSaveSlots(SpriteBatch sb) {
        sb.setColor(this.uiColor);
        for (SaveSlot slot : this.slots) {
            slot.render(sb);
        }
    }

    public void openDeletePopup(int index) {
        this.deletePopup.open(index);
        this.curPop = CurrentPopup.DELETE;
    }

    public void deleteSlot(int index) {
        CardCrawlGame.saveSlotPref.putString(SaveHelper.slotName("PROFILE_NAME", index), "");
        this.slots.get(index).clear();
    }

    public void openRenamePopup(int index, boolean newSave) {
        this.renamePopup.open(index, newSave);
        this.curPop = CurrentPopup.RENAME;
    }

    public static enum CurrentPopup {
        DELETE,
        RENAME,
        NONE;

    }
}


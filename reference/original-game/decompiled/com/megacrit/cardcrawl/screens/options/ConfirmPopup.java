/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.options;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.helpers.TipTracker;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.screens.DeathScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.SaveSlot;
import com.megacrit.cardcrawl.screens.mainMenu.SaveSlotScreen;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfirmPopup {
    protected static final Logger logger = LogManager.getLogger(ConfirmPopup.class.getName());
    protected static final String[] TEXT = CardCrawlGame.languagePack.getUIString((String)"ConfirmPopup").TEXT;
    public String title;
    public String desc;
    public ConfirmType type;
    public Hitbox yesHb;
    public Hitbox noHb;
    public boolean shown = false;
    protected int slot = -1;
    protected Color screenColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    protected Color uiColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    protected Color headerColor = Settings.GOLD_COLOR.cpy();
    protected Color descriptionColor = Settings.CREAM_COLOR.cpy();
    protected float targetAlpha = 0.0f;
    protected float targetAlpha2 = 0.0f;
    protected static final int CONFIRM_W = 360;
    protected static final int CONFIRM_H = 414;
    protected static final int YES_W = 173;
    protected static final int NO_W = 161;
    protected static final int BUTTON_H = 74;
    protected static final float SCREEN_DARKNESS = 0.75f;

    public ConfirmPopup(ConfirmType type) {
        switch (type) {
            case ABANDON_MAIN_MENU: {
                String[] TMP = CardCrawlGame.languagePack.getUIString((String)"SettingsScreen").TEXT;
                this.type = type;
                this.title = TMP[0];
                this.desc = TMP[2];
                this.initializeButtons();
                break;
            }
        }
    }

    private void initializeButtons() {
        if (Settings.isMobile) {
            this.yesHb = new Hitbox(240.0f * Settings.scale, 110.0f * Settings.scale);
            this.noHb = new Hitbox(240.0f * Settings.scale, 110.0f * Settings.scale);
            this.yesHb.move(810.0f * Settings.xScale, Settings.OPTION_Y - 162.0f * Settings.scale);
            this.noHb.move(1112.0f * Settings.xScale, Settings.OPTION_Y - 162.0f * Settings.scale);
        } else {
            this.yesHb = new Hitbox(160.0f * Settings.scale, 70.0f * Settings.scale);
            this.noHb = new Hitbox(160.0f * Settings.scale, 70.0f * Settings.scale);
            this.yesHb.move(860.0f * Settings.xScale, Settings.OPTION_Y - 118.0f * Settings.scale);
            this.noHb.move(1062.0f * Settings.xScale, Settings.OPTION_Y - 118.0f * Settings.scale);
        }
    }

    public ConfirmPopup() {
        this.initializeButtons();
    }

    public ConfirmPopup(String title, String desc, ConfirmType type) {
        this.type = type;
        this.title = title;
        this.desc = desc;
        this.initializeButtons();
    }

    public void show() {
        if (!this.shown) {
            this.shown = true;
        }
    }

    public void hide() {
        if (this.shown) {
            this.shown = false;
            if (this.type == ConfirmType.ABANDON_MID_RUN || this.type == ConfirmType.ABANDON_MAIN_MENU) {
                CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
                if (AbstractDungeon.overlayMenu != null) {
                    AbstractDungeon.overlayMenu.cancelButton.show(TEXT[0]);
                }
            } else if (AbstractDungeon.overlayMenu != null) {
                AbstractDungeon.overlayMenu.cancelButton.show(TEXT[0]);
            }
        }
    }

    protected void updateTransparency() {
        if (this.shown) {
            this.screenColor.a = MathHelper.fadeLerpSnap(this.screenColor.a, 0.75f);
            this.uiColor.a = MathHelper.fadeLerpSnap(this.uiColor.a, 1.0f);
        } else {
            this.screenColor.a = MathHelper.fadeLerpSnap(this.screenColor.a, 0.0f);
            this.uiColor.a = MathHelper.fadeLerpSnap(this.uiColor.a, 0.0f);
        }
    }

    public void update() {
        this.updateTransparency();
        if (this.shown) {
            this.updateYes();
            this.updateNo();
        }
    }

    protected void updateYes() {
        this.yesHb.update();
        if (this.yesHb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        } else if (InputHelper.justClickedLeft && this.yesHb.hovered) {
            CardCrawlGame.sound.play("UI_CLICK_1");
            this.yesHb.clickStarted = true;
        } else if (this.yesHb.clicked) {
            this.yesHb.clicked = false;
            this.yesButtonEffect();
        }
        if (CInputActionSet.proceed.isJustPressed()) {
            CInputActionSet.proceed.unpress();
            this.yesHb.clicked = true;
        }
    }

    protected void updateNo() {
        this.noHb.update();
        if (this.noHb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        } else if (this.noHb.hovered && InputHelper.justClickedLeft) {
            CardCrawlGame.sound.play("UI_CLICK_1");
            this.noHb.clickStarted = true;
        } else if (this.noHb.clicked) {
            this.noHb.clicked = false;
            this.noButtonEffect();
            this.hide();
        }
        if (CInputActionSet.cancel.isJustPressed() || InputActionSet.cancel.isJustPressed()) {
            CInputActionSet.cancel.unpress();
            this.noButtonEffect();
        }
    }

    protected void noButtonEffect() {
        switch (this.type) {
            case DELETE_SAVE: {
                CardCrawlGame.mainMenuScreen.saveSlotScreen.curPop = SaveSlotScreen.CurrentPopup.NONE;
                this.shown = false;
                break;
            }
            case SKIP_FTUE: {
                TipTracker.disableAllFtues();
                this.hide();
                break;
            }
            case ABANDON_MAIN_MENU: {
                CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.MAIN_MENU;
                this.shown = false;
                this.targetAlpha = 0.0f;
                this.targetAlpha2 = 0.0f;
                if (AbstractDungeon.overlayMenu == null) break;
                AbstractDungeon.overlayMenu.cancelButton.show(CardCrawlGame.languagePack.getUIString((String)"SettingsScreen").TEXT[0]);
                break;
            }
            default: {
                this.hide();
            }
        }
    }

    private void abandonRunFromMainMenu(AbstractPlayer player) {
        AbstractPlayer.PlayerClass pClass = player.chosenClass;
        logger.info("Abandoning run with " + pClass.name());
        SaveFile file = SaveAndContinue.loadSaveFile(pClass);
        if (Settings.isStandardRun()) {
            if (file.floor_num >= 16) {
                CardCrawlGame.playerPref.putInteger(pClass.name() + "_SPIRITS", 1);
                CardCrawlGame.playerPref.flush();
            } else {
                CardCrawlGame.playerPref.putInteger(pClass.name() + "_SPIRITS", 0);
                CardCrawlGame.playerPref.flush();
            }
        }
        SaveAndContinue.deleteSave(player);
        if (!file.is_ascension_mode) {
            StatsScreen.incrementDeath(player.getCharStat());
        }
    }

    protected void yesButtonEffect() {
        switch (this.type) {
            case EXIT: {
                CardCrawlGame.music.fadeAll();
                this.hide();
                AbstractDungeon.getCurrRoom().clearEvent();
                AbstractDungeon.closeCurrentScreen();
                CardCrawlGame.startOver();
                if (RestRoom.lastFireSoundId != 0L) {
                    CardCrawlGame.sound.fadeOut("REST_FIRE_WET", RestRoom.lastFireSoundId);
                }
                if (AbstractDungeon.player.stance.ID.equals("Neutral") || AbstractDungeon.player.stance == null) break;
                AbstractDungeon.player.stance.stopIdleSfx();
                break;
            }
            case ABANDON_MAIN_MENU: {
                AbstractPlayer playerWithSave = CardCrawlGame.characterManager.loadChosenCharacter();
                if (playerWithSave != null) {
                    String statId = Settings.isBeta ? playerWithSave.getWinStreakKey() + "_BETA" : playerWithSave.getWinStreakKey();
                    CardCrawlGame.publisherIntegration.setStat(statId, 0);
                    logger.info("WIN STREAK  " + CardCrawlGame.publisherIntegration.getStat(statId));
                    this.abandonRunFromMainMenu(playerWithSave);
                }
                CardCrawlGame.mainMenuScreen.abandonedRun = true;
                this.hide();
                break;
            }
            case ABANDON_MID_RUN: {
                this.hide();
                AbstractDungeon.closeCurrentScreen();
                AbstractDungeon.player.isDead = true;
                AbstractDungeon.deathScreen = new DeathScreen(AbstractDungeon.getMonsters());
                break;
            }
            case DELETE_SAVE: {
                SaveHelper.deletePrefs(this.slot);
                CardCrawlGame.mainMenuScreen.saveSlotScreen.deleteSlot(this.slot);
                CardCrawlGame.mainMenuScreen.saveSlotScreen.curPop = SaveSlotScreen.CurrentPopup.NONE;
                CardCrawlGame.playerName = "";
                this.shown = false;
                boolean allSlotsEmpty = true;
                for (SaveSlot s : CardCrawlGame.mainMenuScreen.saveSlotScreen.slots) {
                    if (s.emptySlot) continue;
                    allSlotsEmpty = false;
                    break;
                }
                if (!allSlotsEmpty) break;
                CardCrawlGame.mainMenuScreen.saveSlotScreen.cancelButton.hide();
                break;
            }
            case SKIP_FTUE: {
                TipTracker.neverShowAgain("NO_FTUE");
                this.hide();
                break;
            }
        }
    }

    public void render(SpriteBatch sb) {
        this.renderPopupBg(sb);
        this.renderText(sb);
        if (this.shown) {
            this.renderButtons(sb);
        }
        this.renderControllerUi(sb);
    }

    protected void renderPopupBg(SpriteBatch sb) {
        sb.setColor(this.screenColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        sb.setColor(this.uiColor);
        if (!Settings.isMobile) {
            sb.draw(ImageMaster.OPTION_CONFIRM, (float)Settings.WIDTH / 2.0f - 180.0f, Settings.OPTION_Y - 207.0f, 180.0f, 207.0f, 360.0f, 414.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 360, 414, false, false);
        } else {
            sb.draw(ImageMaster.OPTION_CONFIRM, (float)Settings.WIDTH / 2.0f - 180.0f, Settings.OPTION_Y - 207.0f, 180.0f, 207.0f, 360.0f, 414.0f, Settings.scale * 1.5f, Settings.scale * 1.5f, 0.0f, 0, 0, 360, 414, false, false);
        }
    }

    private void renderButtons(SpriteBatch sb) {
        if (Settings.isMobile) {
            sb.draw(ImageMaster.OPTION_YES, (float)Settings.WIDTH / 2.0f - 86.5f - 150.0f * Settings.scale, Settings.OPTION_Y - 37.0f - 170.0f * Settings.scale, 86.5f, 37.0f, 173.0f, 74.0f, Settings.scale * 1.5f, Settings.scale * 1.5f, 0.0f, 0, 0, 173, 74, false, false);
        } else {
            sb.draw(ImageMaster.OPTION_YES, (float)Settings.WIDTH / 2.0f - 86.5f - 100.0f * Settings.scale, Settings.OPTION_Y - 37.0f - 120.0f * Settings.scale, 86.5f, 37.0f, 173.0f, 74.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 173, 74, false, false);
        }
        if (this.yesHb.hovered) {
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, this.uiColor.a * 0.25f));
            sb.setBlendFunction(770, 1);
            if (Settings.isMobile) {
                sb.draw(ImageMaster.OPTION_YES, (float)Settings.WIDTH / 2.0f - 86.5f - 150.0f * Settings.scale, Settings.OPTION_Y - 37.0f - 170.0f * Settings.scale, 86.5f, 37.0f, 173.0f, 74.0f, Settings.scale * 1.5f, Settings.scale * 1.5f, 0.0f, 0, 0, 173, 74, false, false);
            } else {
                sb.draw(ImageMaster.OPTION_YES, (float)Settings.WIDTH / 2.0f - 86.5f - 100.0f * Settings.scale, Settings.OPTION_Y - 37.0f - 120.0f * Settings.scale, 86.5f, 37.0f, 173.0f, 74.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 173, 74, false, false);
            }
            sb.setBlendFunction(770, 771);
            sb.setColor(this.uiColor);
            if (Settings.isMobile) {
                FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[2], (float)Settings.WIDTH / 2.0f - 165.0f * Settings.scale, Settings.OPTION_Y - 162.0f * Settings.scale, this.uiColor, 1.0f);
            } else {
                FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, TEXT[2], (float)Settings.WIDTH / 2.0f - 110.0f * Settings.scale, Settings.OPTION_Y - 118.0f * Settings.scale, this.uiColor, 1.0f);
            }
        } else if (Settings.isMobile) {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[2], (float)Settings.WIDTH / 2.0f - 165.0f * Settings.scale, Settings.OPTION_Y - 162.0f * Settings.scale, this.headerColor, 1.0f);
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, TEXT[2], (float)Settings.WIDTH / 2.0f - 110.0f * Settings.scale, Settings.OPTION_Y - 118.0f * Settings.scale, this.headerColor, 1.0f);
        }
        if (Settings.isMobile) {
            sb.draw(ImageMaster.OPTION_NO, (float)Settings.WIDTH / 2.0f - 80.5f + 160.0f * Settings.scale, Settings.OPTION_Y - 37.0f - 170.0f * Settings.scale, 80.5f, 37.0f, 161.0f, 74.0f, Settings.scale * 1.5f, Settings.scale * 1.5f, 0.0f, 0, 0, 161, 74, false, false);
        } else {
            sb.draw(ImageMaster.OPTION_NO, (float)Settings.WIDTH / 2.0f - 80.5f + 106.0f * Settings.scale, Settings.OPTION_Y - 37.0f - 120.0f * Settings.scale, 80.5f, 37.0f, 161.0f, 74.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 161, 74, false, false);
        }
        if (this.noHb.hovered) {
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, this.uiColor.a * 0.25f));
            sb.setBlendFunction(770, 1);
            if (Settings.isMobile) {
                sb.draw(ImageMaster.OPTION_NO, (float)Settings.WIDTH / 2.0f - 80.5f + 160.0f * Settings.scale, Settings.OPTION_Y - 37.0f - 170.0f * Settings.scale, 80.5f, 37.0f, 161.0f, 74.0f, Settings.scale * 1.5f, Settings.scale * 1.5f, 0.0f, 0, 0, 161, 74, false, false);
            } else {
                sb.draw(ImageMaster.OPTION_NO, (float)Settings.WIDTH / 2.0f - 80.5f + 106.0f * Settings.scale, Settings.OPTION_Y - 37.0f - 120.0f * Settings.scale, 80.5f, 37.0f, 161.0f, 74.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 161, 74, false, false);
            }
            sb.setBlendFunction(770, 771);
            sb.setColor(this.uiColor);
            if (Settings.isMobile) {
                FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[3], (float)Settings.WIDTH / 2.0f + 165.0f * Settings.scale, Settings.OPTION_Y - 162.0f * Settings.scale, this.uiColor, 1.0f);
            } else {
                FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, TEXT[3], (float)Settings.WIDTH / 2.0f + 110.0f * Settings.scale, Settings.OPTION_Y - 118.0f * Settings.scale, this.uiColor, 1.0f);
            }
        } else if (Settings.isMobile) {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, TEXT[3], (float)Settings.WIDTH / 2.0f + 165.0f * Settings.scale, Settings.OPTION_Y - 162.0f * Settings.scale, this.headerColor, 1.0f);
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, TEXT[3], (float)Settings.WIDTH / 2.0f + 110.0f * Settings.scale, Settings.OPTION_Y - 118.0f * Settings.scale, this.headerColor, 1.0f);
        }
        this.yesHb.render(sb);
        this.noHb.render(sb);
    }

    private void renderText(SpriteBatch sb) {
        this.headerColor.a = this.uiColor.a;
        if (Settings.isMobile) {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, this.title, (float)Settings.WIDTH / 2.0f, Settings.OPTION_Y + 200.0f * Settings.scale, this.headerColor, 1.2f);
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, this.title, (float)Settings.WIDTH / 2.0f, Settings.OPTION_Y + 126.0f * Settings.scale, this.headerColor);
        }
        this.descriptionColor.a = this.uiColor.a;
        if (Settings.isMobile) {
            FontHelper.renderWrappedText(sb, FontHelper.panelNameFont, this.desc, (float)Settings.WIDTH / 2.0f, Settings.OPTION_Y + 30.0f * Settings.scale, 380.0f * Settings.scale, this.descriptionColor, 1.0f);
        } else {
            FontHelper.renderWrappedText(sb, FontHelper.tipBodyFont, this.desc, (float)Settings.WIDTH / 2.0f, Settings.OPTION_Y + 20.0f * Settings.scale, 240.0f * Settings.scale, this.descriptionColor, 1.0f);
        }
    }

    private void renderControllerUi(SpriteBatch sb) {
        if (Settings.isControllerMode) {
            sb.draw(CInputActionSet.proceed.getKeyImg(), 770.0f * Settings.xScale - 32.0f, Settings.OPTION_Y - 32.0f - 140.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            sb.draw(CInputActionSet.cancel.getKeyImg(), 1150.0f * Settings.xScale - 32.0f, Settings.OPTION_Y - 32.0f - 140.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
    }

    public static enum ConfirmType {
        EXIT,
        ABANDON_MID_RUN,
        DELETE_SAVE,
        SKIP_FTUE,
        ABANDON_MAIN_MENU;

    }
}


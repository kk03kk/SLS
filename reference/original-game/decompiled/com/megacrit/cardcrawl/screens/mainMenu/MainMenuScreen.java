/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.mainMenu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.credits.CreditsScreen;
import com.megacrit.cardcrawl.cutscenes.NeowNarrationScreen;
import com.megacrit.cardcrawl.daily.DailyScreen;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.TipTracker;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.input.DevInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.scenes.TitleBackground;
import com.megacrit.cardcrawl.screens.CharSelectInfo;
import com.megacrit.cardcrawl.screens.DoorUnlockScreen;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.screens.compendium.CardLibraryScreen;
import com.megacrit.cardcrawl.screens.compendium.PotionViewScreen;
import com.megacrit.cardcrawl.screens.compendium.RelicViewScreen;
import com.megacrit.cardcrawl.screens.custom.CustomModeScreen;
import com.megacrit.cardcrawl.screens.leaderboards.LeaderboardScreen;
import com.megacrit.cardcrawl.screens.mainMenu.EarlyAccessPopup;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuPanelButton;
import com.megacrit.cardcrawl.screens.mainMenu.MenuButton;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.mainMenu.MenuPanelScreen;
import com.megacrit.cardcrawl.screens.mainMenu.PatchNotesScreen;
import com.megacrit.cardcrawl.screens.mainMenu.SaveSlotScreen;
import com.megacrit.cardcrawl.screens.mainMenu.SyncMessage;
import com.megacrit.cardcrawl.screens.options.ConfirmPopup;
import com.megacrit.cardcrawl.screens.options.InputSettingsScreen;
import com.megacrit.cardcrawl.screens.options.OptionsPanel;
import com.megacrit.cardcrawl.screens.runHistory.RunHistoryScreen;
import com.megacrit.cardcrawl.screens.stats.StatsScreen;
import com.megacrit.cardcrawl.ui.buttons.ConfirmButton;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainMenuScreen {
    private static final Logger logger = LogManager.getLogger(MainMenuScreen.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("MainMenuScreen");
    public static final String[] TEXT = MainMenuScreen.uiStrings.TEXT;
    private static final String VERSION_INFO = CardCrawlGame.VERSION_NUM;
    private Hitbox nameEditHb = Settings.isMobile ? new Hitbox(550.0f * Settings.scale, 180.0f * Settings.scale) : new Hitbox(400.0f * Settings.scale, 100.0f * Settings.scale);
    public String newName;
    public boolean darken = false;
    public boolean superDarken = false;
    public Texture saveSlotImg = null;
    public Color screenColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    public static final float OVERLAY_ALPHA = 0.8f;
    private Color overlayColor = new Color(0.0f, 0.0f, 0.0f, 0.0f);
    public boolean fadedOut = false;
    public boolean isFadingOut = false;
    public long windId = 0L;
    private CharSelectInfo charInfo = null;
    public TitleBackground bg = new TitleBackground();
    private EarlyAccessPopup eaPopup = null;
    public CurScreen screen = CurScreen.MAIN_MENU;
    public SaveSlotScreen saveSlotScreen = new SaveSlotScreen();
    public MenuPanelScreen panelScreen = new MenuPanelScreen();
    public StatsScreen statsScreen = new StatsScreen();
    public DailyScreen dailyScreen = new DailyScreen();
    public CardLibraryScreen cardLibraryScreen = new CardLibraryScreen();
    public LeaderboardScreen leaderboardsScreen = new LeaderboardScreen();
    public RelicViewScreen relicScreen = new RelicViewScreen();
    public PotionViewScreen potionScreen = new PotionViewScreen();
    public CreditsScreen creditsScreen = new CreditsScreen();
    public DoorUnlockScreen doorUnlockScreen = new DoorUnlockScreen();
    public NeowNarrationScreen neowNarrateScreen = new NeowNarrationScreen();
    public PatchNotesScreen patchNotesScreen = new PatchNotesScreen();
    public RunHistoryScreen runHistoryScreen;
    public CharacterSelectScreen charSelectScreen = new CharacterSelectScreen();
    public CustomModeScreen customModeScreen = new CustomModeScreen();
    public ConfirmPopup abandonPopup = new ConfirmPopup(ConfirmPopup.ConfirmType.ABANDON_MAIN_MENU);
    public InputSettingsScreen inputSettingsScreen = new InputSettingsScreen();
    public OptionsPanel optionPanel = new OptionsPanel();
    public SyncMessage syncMessage = new SyncMessage();
    public boolean isSettingsUp = false;
    public ConfirmButton confirmButton = new ConfirmButton(TEXT[1]);
    public MenuCancelButton cancelButton = new MenuCancelButton();
    private Hitbox deckHb = new Hitbox(-1000.0f, 0.0f, 400.0f * Settings.scale, 80.0f * Settings.scale);
    public ArrayList<MenuButton> buttons = new ArrayList();
    public boolean abandonedRun = false;

    public MainMenuScreen() {
        this(true);
    }

    public MainMenuScreen(boolean playBgm) {
        CardCrawlGame.publisherIntegration.setRichPresenceDisplayInMenu();
        AbstractDungeon.player = null;
        if (Settings.isDemo && Settings.isShowBuild) {
            TipTracker.reset();
        }
        if (playBgm) {
            CardCrawlGame.music.changeBGM("MENU");
            this.windId = Settings.AMBIANCE_ON ? CardCrawlGame.sound.playAndLoop("WIND") : CardCrawlGame.sound.playAndLoop("WIND", 0.0f);
        }
        UnlockTracker.refresh();
        this.cardLibraryScreen.initialize();
        this.charSelectScreen.initialize();
        this.confirmButton.hide();
        this.updateAmbienceVolume();
        this.nameEditHb.move(200.0f * Settings.scale, (float)Settings.HEIGHT - 50.0f * Settings.scale);
        this.setMainMenuButtons();
        this.runHistoryScreen = new RunHistoryScreen();
    }

    private void setMainMenuButtons() {
        this.buttons.clear();
        int index = 0;
        if (!Settings.isMobile && !Settings.isConsoleBuild) {
            this.buttons.add(new MenuButton(MenuButton.ClickResult.QUIT, index++));
            this.buttons.add(new MenuButton(MenuButton.ClickResult.PATCH_NOTES, index++));
        }
        this.buttons.add(new MenuButton(MenuButton.ClickResult.SETTINGS, index++));
        if (!Settings.isShowBuild && this.statsScreen.statScreenUnlocked()) {
            this.buttons.add(new MenuButton(MenuButton.ClickResult.STAT, index++));
            this.buttons.add(new MenuButton(MenuButton.ClickResult.INFO, index++));
        }
        if (CardCrawlGame.characterManager.anySaveFileExists()) {
            this.buttons.add(new MenuButton(MenuButton.ClickResult.ABANDON_RUN, index++));
            this.buttons.add(new MenuButton(MenuButton.ClickResult.RESUME_GAME, index++));
        } else {
            this.buttons.add(new MenuButton(MenuButton.ClickResult.PLAY, index++));
        }
    }

    public void update() {
        if (this.isFadingOut) {
            InputHelper.justClickedLeft = false;
            InputHelper.justReleasedClickLeft = false;
            InputHelper.justClickedRight = false;
            InputHelper.justReleasedClickRight = false;
        }
        this.abandonPopup.update();
        if (this.abandonedRun) {
            this.abandonedRun = false;
            this.buttons.remove(this.buttons.size() - 1);
            this.buttons.remove(this.buttons.size() - 1);
            this.buttons.add(new MenuButton(MenuButton.ClickResult.PLAY, this.buttons.size()));
        }
        if (Settings.isInfo && DevInputActionSet.deleteSteamCloud.isJustPressed()) {
            CardCrawlGame.publisherIntegration.deleteAllCloudFiles();
        }
        this.syncMessage.update();
        this.cancelButton.update();
        this.updateSettings();
        if (this.screen != CurScreen.SAVE_SLOT) {
            for (MenuButton b : this.buttons) {
                b.update();
            }
        }
        switch (this.screen) {
            case CHAR_SELECT: {
                this.updateCharSelectController();
                this.charSelectScreen.update();
                break;
            }
            case CARD_LIBRARY: {
                this.cardLibraryScreen.update();
                break;
            }
            case CUSTOM: {
                this.customModeScreen.update();
                break;
            }
            case PANEL_MENU: {
                this.updateMenuPanelController();
                this.panelScreen.update();
                break;
            }
            case DAILY: {
                this.dailyScreen.update();
                break;
            }
            case BANNER_DECK_VIEW: {
                break;
            }
            case MAIN_MENU: {
                this.updateMenuButtonController();
                break;
            }
            case LEADERBOARD: {
                this.leaderboardsScreen.update();
                break;
            }
            case RELIC_VIEW: {
                this.relicScreen.update();
                break;
            }
            case POTION_VIEW: {
                this.potionScreen.update();
                break;
            }
            case SAVE_SLOT: {
                break;
            }
            case SETTINGS: {
                break;
            }
            case TRIALS: {
                break;
            }
            case STATS: {
                this.statsScreen.update();
                break;
            }
            case CREDITS: {
                this.creditsScreen.update();
                break;
            }
            case DOOR_UNLOCK: {
                this.doorUnlockScreen.update();
                break;
            }
            case NEOW_SCREEN: {
                this.neowNarrateScreen.update();
                break;
            }
            case PATCH_NOTES: {
                this.patchNotesScreen.update();
                break;
            }
            case RUN_HISTORY: {
                this.runHistoryScreen.update();
                break;
            }
            case INPUT_SETTINGS: {
                this.inputSettingsScreen.update();
                break;
            }
        }
        this.saveSlotScreen.update();
        this.bg.update();
        this.screenColor.a = this.superDarken ? MathHelper.popLerpSnap(this.screenColor.a, 1.0f) : (this.darken ? MathHelper.popLerpSnap(this.screenColor.a, 0.8f) : MathHelper.popLerpSnap(this.screenColor.a, 0.0f));
        if (!this.statsScreen.screenUp) {
            this.updateRenameArea();
        }
        if (this.charInfo != null && this.charInfo.resumeGame) {
            this.deckHb.update();
            if (this.deckHb.justHovered) {
                CardCrawlGame.sound.play("UI_HOVER");
            }
        }
        if (!this.isFadingOut) {
            this.handleInput();
        }
        this.fadeOut();
    }

    private void updateMenuButtonController() {
        if (!Settings.isControllerMode || EarlyAccessPopup.isUp) {
            return;
        }
        boolean anyHovered = false;
        int index = 0;
        for (MenuButton b : this.buttons) {
            if (b.hb.hovered) {
                anyHovered = true;
                break;
            }
            ++index;
        }
        if (anyHovered) {
            if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                if (--index < 0) {
                    index = this.buttons.size() - 1;
                }
                CInputHelper.setCursor(this.buttons.get((int)index).hb);
            } else if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                if (++index > this.buttons.size() - 1) {
                    index = 0;
                }
                CInputHelper.setCursor(this.buttons.get((int)index).hb);
            }
        } else {
            index = this.buttons.size() - 1;
            CInputHelper.setCursor(this.buttons.get((int)index).hb);
        }
    }

    private void updateCharSelectController() {
        if (!Settings.isControllerMode || this.isFadingOut) {
            return;
        }
        boolean anyHovered = false;
        int index = 0;
        for (CharacterOption b : this.charSelectScreen.options) {
            if (b.hb.hovered) {
                anyHovered = true;
                break;
            }
            ++index;
        }
        if (!anyHovered) {
            index = 0;
            CInputHelper.setCursor(this.charSelectScreen.options.get((int)index).hb);
            this.charSelectScreen.options.get((int)index).hb.clicked = true;
        } else {
            if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                if (--index < 0) {
                    index = this.charSelectScreen.options.size() - 1;
                }
                CInputHelper.setCursor(this.charSelectScreen.options.get((int)index).hb);
                this.charSelectScreen.options.get((int)index).hb.clicked = true;
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
                if (++index > this.charSelectScreen.options.size() - 1) {
                    index = 0;
                }
                CInputHelper.setCursor(this.charSelectScreen.options.get((int)index).hb);
                this.charSelectScreen.options.get((int)index).hb.clicked = true;
            }
            if (this.charSelectScreen.options.get((int)index).locked) {
                this.charSelectScreen.confirmButton.hide();
            } else {
                this.charSelectScreen.confirmButton.show();
            }
        }
    }

    private void updateMenuPanelController() {
        if (!Settings.isControllerMode) {
            return;
        }
        boolean anyHovered = false;
        int index = 0;
        for (MainMenuPanelButton b : this.panelScreen.panels) {
            if (b.hb.hovered) {
                anyHovered = true;
                break;
            }
            ++index;
        }
        if (anyHovered) {
            if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                if (--index < 0) {
                    index = this.panelScreen.panels.size() - 1;
                }
                if (this.panelScreen.panels.get((int)index).pColor == MainMenuPanelButton.PanelColor.GRAY) {
                    --index;
                }
                CInputHelper.setCursor(this.panelScreen.panels.get((int)index).hb);
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
                if (++index > this.panelScreen.panels.size() - 1) {
                    index = 0;
                }
                if (this.panelScreen.panels.get((int)index).pColor == MainMenuPanelButton.PanelColor.GRAY) {
                    index = 0;
                }
                CInputHelper.setCursor(this.panelScreen.panels.get((int)index).hb);
            }
        } else {
            index = 0;
            CInputHelper.setCursor(this.panelScreen.panels.get((int)index).hb);
        }
    }

    private void updateSettings() {
        if (this.saveSlotScreen.shown) {
            return;
        }
        if (!EarlyAccessPopup.isUp && InputHelper.pressedEscape && this.screen == CurScreen.MAIN_MENU && !this.isFadingOut) {
            if (!this.isSettingsUp) {
                GameCursor.hidden = false;
                CardCrawlGame.sound.play("END_TURN");
                this.isSettingsUp = true;
                this.darken();
                InputHelper.pressedEscape = false;
                this.statsScreen.hide();
                this.dailyScreen.hide();
                this.cancelButton.hide();
                CardCrawlGame.cancelButton.show(TEXT[2]);
                this.screen = CurScreen.SETTINGS;
                this.panelScreen.panels.clear();
                this.hideMenuButtons();
            } else if (!EarlyAccessPopup.isUp) {
                this.isSettingsUp = false;
                CardCrawlGame.cancelButton.hide();
                this.screen = CurScreen.MAIN_MENU;
                if (this.screen == CurScreen.MAIN_MENU) {
                    this.cancelButton.hide();
                }
            }
        }
        if (this.isSettingsUp) {
            this.optionPanel.update();
        }
        CardCrawlGame.cancelButton.update();
    }

    private void updateRenameArea() {
        if (this.screen == CurScreen.MAIN_MENU) {
            this.nameEditHb.update();
        }
        if (this.screen == CurScreen.MAIN_MENU && (this.nameEditHb.hovered && InputHelper.justClickedLeft || CInputActionSet.map.isJustPressed())) {
            InputHelper.justClickedLeft = false;
            this.nameEditHb.hovered = false;
            this.saveSlotScreen.open(CardCrawlGame.playerName);
            this.screen = CurScreen.SAVE_SLOT;
        }
        if (this.bg.slider <= 0.1f && CardCrawlGame.saveSlotPref.getInteger("DEFAULT_SLOT", -1) == -1 && this.screen == CurScreen.MAIN_MENU && !this.setDefaultSlot()) {
            logger.info("No saves detected, opening Save Slot screen automatically.");
            CardCrawlGame.playerPref.putBoolean("ftuePopupShown", true);
            this.saveSlotScreen.open(CardCrawlGame.playerName);
            this.screen = CurScreen.SAVE_SLOT;
        }
    }

    private boolean setDefaultSlot() {
        if (!CardCrawlGame.playerPref.getString("name", "").equals("")) {
            logger.info("Migration to Save Slot schema detected, setting DEFAULT_SLOT to 0.");
            CardCrawlGame.saveSlot = 0;
            CardCrawlGame.saveSlotPref.putInteger("DEFAULT_SLOT", 0);
            CardCrawlGame.saveSlotPref.flush();
            return true;
        }
        return false;
    }

    private void handleInput() {
        this.confirmButton.update();
    }

    public void fadeOutMusic() {
        CardCrawlGame.music.fadeOutBGM();
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.fadeOut("WIND", this.windId);
        }
    }

    public void render(SpriteBatch sb) {
        this.bg.render(sb);
        this.cancelButton.render(sb);
        this.renderNameEdit(sb);
        for (MenuButton b : this.buttons) {
            b.render(sb);
        }
        this.abandonPopup.render(sb);
        sb.setColor(this.screenColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        if (this.isFadingOut) {
            this.confirmButton.update();
        }
        if (this.screen == CurScreen.CHAR_SELECT) {
            this.charSelectScreen.render(sb);
        }
        sb.setColor(this.overlayColor);
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        this.renderSettings(sb);
        this.confirmButton.render(sb);
        if (CardCrawlGame.displayVersion) {
            FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, VERSION_INFO, 20.0f * Settings.scale - 700.0f * this.bg.slider, 30.0f * Settings.scale, 10000.0f, 32.0f * Settings.scale, new Color(1.0f, 1.0f, 1.0f, 0.3f));
        }
        switch (this.screen) {
            case CARD_LIBRARY: {
                this.cardLibraryScreen.render(sb);
                break;
            }
            case CUSTOM: {
                this.customModeScreen.render(sb);
                break;
            }
            case PANEL_MENU: {
                this.panelScreen.render(sb);
                break;
            }
            case DAILY: {
                this.dailyScreen.render(sb);
                sb.setColor(this.overlayColor);
                sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
                break;
            }
            case BANNER_DECK_VIEW: {
                break;
            }
            case MAIN_MENU: {
                break;
            }
            case RELIC_VIEW: {
                this.relicScreen.render(sb);
                break;
            }
            case POTION_VIEW: {
                this.potionScreen.render(sb);
                break;
            }
            case SAVE_SLOT: {
                break;
            }
            case SETTINGS: {
                break;
            }
            case TRIALS: {
                break;
            }
            case LEADERBOARD: {
                this.leaderboardsScreen.render(sb);
                break;
            }
            case STATS: {
                this.statsScreen.render(sb);
                break;
            }
            case RUN_HISTORY: {
                this.runHistoryScreen.render(sb);
                break;
            }
            case INPUT_SETTINGS: {
                this.inputSettingsScreen.render(sb);
                break;
            }
            case CREDITS: {
                this.creditsScreen.render(sb);
                break;
            }
            case DOOR_UNLOCK: {
                this.doorUnlockScreen.render(sb);
                break;
            }
            case NEOW_SCREEN: {
                this.neowNarrateScreen.render(sb);
                break;
            }
            case PATCH_NOTES: {
                this.patchNotesScreen.render(sb);
                break;
            }
        }
        this.saveSlotScreen.render(sb);
        this.syncMessage.render(sb);
        if (this.eaPopup != null) {
            this.eaPopup.render(sb);
        }
    }

    private void renderSettings(SpriteBatch sb) {
        if (this.isSettingsUp && this.screen == CurScreen.SETTINGS) {
            this.optionPanel.render(sb);
        }
        CardCrawlGame.cancelButton.render(sb);
    }

    private void renderNameEdit(SpriteBatch sb) {
        if (Settings.isMobile) {
            if (!this.nameEditHb.hovered) {
                FontHelper.renderSmartText(sb, FontHelper.cardEnergyFont_L, CardCrawlGame.playerName, 140.0f * Settings.scale - 500.0f * this.bg.slider, (float)Settings.HEIGHT - 30.0f * Settings.scale, 1000.0f, 30.0f * Settings.scale, Color.GOLD, 0.9f);
            } else {
                FontHelper.renderSmartText(sb, FontHelper.cardEnergyFont_L, CardCrawlGame.playerName, 140.0f * Settings.scale - 500.0f * this.bg.slider, (float)Settings.HEIGHT - 30.0f * Settings.scale, 1000.0f, 30.0f * Settings.scale, Settings.GREEN_TEXT_COLOR, 0.9f);
            }
        } else if (!this.nameEditHb.hovered) {
            FontHelper.renderSmartText(sb, FontHelper.cardTitleFont, CardCrawlGame.playerName, 100.0f * Settings.scale - 500.0f * this.bg.slider, (float)Settings.HEIGHT - 24.0f * Settings.scale, 1000.0f, 30.0f * Settings.scale, Color.GOLD, 1.0f);
        } else {
            FontHelper.renderSmartText(sb, FontHelper.cardTitleFont, CardCrawlGame.playerName, 100.0f * Settings.scale - 500.0f * this.bg.slider, (float)Settings.HEIGHT - 24.0f * Settings.scale, 1000.0f, 30.0f * Settings.scale, Settings.GREEN_TEXT_COLOR, 1.0f);
        }
        if (Settings.isTouchScreen || Settings.isMobile) {
            if (!Settings.isMobile) {
                FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, TEXT[5], 100.0f * Settings.scale - 500.0f * this.bg.slider, (float)Settings.HEIGHT - 60.0f * Settings.scale, 1000.0f, 30.0f * Settings.scale, Color.SKY, 1.0f);
            } else {
                FontHelper.renderSmartText(sb, FontHelper.largeDialogOptionFont, TEXT[5], 140.0f * Settings.scale - 500.0f * this.bg.slider, (float)Settings.HEIGHT - 80.0f * Settings.scale, 1000.0f, 30.0f * Settings.scale, Color.SKY);
            }
        } else if (!Settings.isControllerMode) {
            FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, TEXT[3], 100.0f * Settings.scale - 500.0f * this.bg.slider, (float)Settings.HEIGHT - 60.0f * Settings.scale, 1000.0f, 30.0f * Settings.scale, Color.SKY, 1.0f);
        } else {
            sb.draw(CInputActionSet.map.getKeyImg(), -32.0f + 120.0f * Settings.scale - 500.0f * this.bg.slider, -32.0f + (float)Settings.HEIGHT - 78.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 0.8f, Settings.scale * 0.8f, 0.0f, 0, 0, 64, 64, false, false);
            FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, TEXT[4], 150.0f * Settings.scale - 500.0f * this.bg.slider, (float)Settings.HEIGHT - 70.0f * Settings.scale, 1000.0f, 30.0f * Settings.scale, Color.SKY);
        }
        if (Settings.isMobile) {
            sb.draw(CardCrawlGame.getSaveSlotImg(), 70.0f * Settings.scale - 50.0f - 500.0f * this.bg.slider, (float)Settings.HEIGHT - 70.0f * Settings.scale - 50.0f, 50.0f, 50.0f, 100.0f, 100.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 100, 100, false, false);
        } else {
            sb.draw(CardCrawlGame.getSaveSlotImg(), 50.0f * Settings.scale - 50.0f - 500.0f * this.bg.slider, (float)Settings.HEIGHT - 50.0f * Settings.scale - 50.0f, 50.0f, 50.0f, 100.0f, 100.0f, Settings.scale * 0.75f, Settings.scale * 0.75f, 0.0f, 0, 0, 100, 100, false, false);
        }
        this.nameEditHb.render(sb);
    }

    private void fadeOut() {
        if (this.isFadingOut && !this.fadedOut) {
            this.overlayColor.a += Gdx.graphics.getDeltaTime();
            if (this.overlayColor.a > 1.0f) {
                this.overlayColor.a = 1.0f;
                this.fadedOut = true;
                FontHelper.ClearLeaderboardFontTextures();
            }
        } else if (this.overlayColor.a != 0.0f) {
            this.overlayColor.a -= Gdx.graphics.getDeltaTime() * 2.0f;
            if (this.overlayColor.a < 0.0f) {
                this.overlayColor.a = 0.0f;
            }
        }
    }

    public void updateAmbienceVolume() {
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.adjustVolume("WIND", this.windId);
        } else {
            CardCrawlGame.sound.adjustVolume("WIND", this.windId, 0.0f);
        }
    }

    public void muteAmbienceVolume() {
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.adjustVolume("WIND", this.windId, 0.0f);
        }
    }

    public void unmuteAmbienceVolume() {
        CardCrawlGame.sound.adjustVolume("WIND", this.windId);
    }

    public void darken() {
        this.darken = true;
    }

    public void lighten() {
        this.darken = false;
    }

    public void hideMenuButtons() {
        for (MenuButton b : this.buttons) {
            b.hide();
        }
    }

    public static enum CurScreen {
        CHAR_SELECT,
        RELIC_VIEW,
        POTION_VIEW,
        BANNER_DECK_VIEW,
        DAILY,
        TRIALS,
        SETTINGS,
        MAIN_MENU,
        SAVE_SLOT,
        STATS,
        RUN_HISTORY,
        CARD_LIBRARY,
        CREDITS,
        PATCH_NOTES,
        NONE,
        LEADERBOARD,
        ABANDON_CONFIRM,
        PANEL_MENU,
        INPUT_SETTINGS,
        CUSTOM,
        NEOW_SCREEN,
        DOOR_UNLOCK;

    }
}


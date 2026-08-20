/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.charSelect;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.SeedHelper;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.TrialHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.metrics.BotDataUploader;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.ui.buttons.ConfirmButton;
import com.megacrit.cardcrawl.ui.panels.SeedPanel;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;

public class CharacterSelectScreen {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("CharacterSelectScreen");
    public static final String[] TEXT = CharacterSelectScreen.uiStrings.TEXT;
    private static final UIStrings uiStrings2 = CardCrawlGame.languagePack.getUIString("AscensionModeDescriptions");
    public static final String[] A_TEXT = CharacterSelectScreen.uiStrings2.TEXT;
    private static float ASC_LEFT_W;
    private static float ASC_RIGHT_W;
    private SeedPanel seedPanel = new SeedPanel();
    private static final float SEED_X;
    private static final float SEED_Y;
    private static final String CHOOSE_CHAR_MSG;
    public ConfirmButton confirmButton = new ConfirmButton(TEXT[1]);
    public MenuCancelButton cancelButton = new MenuCancelButton();
    public ArrayList<CharacterOption> options = new ArrayList();
    private boolean anySelected = false;
    public Texture bgCharImg = null;
    private Color bgCharColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    private static final float BG_Y_OFFSET_TARGET = 0.0f;
    private float bg_y_offset = 0.0f;
    public boolean isAscensionMode = false;
    private boolean isAscensionModeUnlocked = false;
    private Hitbox ascensionModeHb;
    private Hitbox ascLeftHb;
    private Hitbox ascRightHb;
    private Hitbox seedHb;
    public int ascensionLevel = 0;
    public String ascLevelInfoString = "";

    public void initialize() {
        this.options.add(new CharacterOption(TEXT[2], CardCrawlGame.characterManager.recreateCharacter(AbstractPlayer.PlayerClass.IRONCLAD), ImageMaster.CHAR_SELECT_IRONCLAD, ImageMaster.CHAR_SELECT_BG_IRONCLAD));
        if (!UnlockTracker.isCharacterLocked("The Silent")) {
            this.options.add(new CharacterOption(TEXT[3], CardCrawlGame.characterManager.recreateCharacter(AbstractPlayer.PlayerClass.THE_SILENT), ImageMaster.CHAR_SELECT_SILENT, ImageMaster.CHAR_SELECT_BG_SILENT));
        } else {
            this.options.add(new CharacterOption(CardCrawlGame.characterManager.recreateCharacter(AbstractPlayer.PlayerClass.THE_SILENT)));
        }
        if (!UnlockTracker.isCharacterLocked("Defect")) {
            this.options.add(new CharacterOption(TEXT[4], CardCrawlGame.characterManager.recreateCharacter(AbstractPlayer.PlayerClass.DEFECT), ImageMaster.CHAR_SELECT_DEFECT, ImageMaster.CHAR_SELECT_BG_DEFECT));
        } else {
            this.options.add(new CharacterOption(CardCrawlGame.characterManager.recreateCharacter(AbstractPlayer.PlayerClass.DEFECT)));
        }
        if (!UnlockTracker.isCharacterLocked("Watcher")) {
            this.addCharacterOption(AbstractPlayer.PlayerClass.WATCHER);
        } else {
            this.options.add(new CharacterOption(CardCrawlGame.characterManager.recreateCharacter(AbstractPlayer.PlayerClass.WATCHER)));
        }
        this.positionButtons();
        this.isAscensionMode = Settings.gamePref.getBoolean("Ascension Mode Default", false);
        FontHelper.cardTitleFont.getData().setScale(1.0f);
        ASC_LEFT_W = FontHelper.getSmartWidth(FontHelper.cardTitleFont, TEXT[6], 9999.0f, 0.0f);
        ASC_RIGHT_W = FontHelper.getSmartWidth(FontHelper.cardTitleFont, TEXT[7] + "22", 9999.0f, 0.0f);
        this.ascensionModeHb = new Hitbox(ASC_LEFT_W + 100.0f * Settings.scale, 70.0f * Settings.scale);
        if (!Settings.isMobile) {
            this.ascensionModeHb.move((float)Settings.WIDTH / 2.0f - ASC_LEFT_W / 2.0f - 50.0f * Settings.scale, 70.0f * Settings.scale);
        } else {
            this.ascensionModeHb.move((float)Settings.WIDTH / 2.0f - ASC_LEFT_W / 2.0f - 50.0f * Settings.scale, 100.0f * Settings.scale);
        }
        this.ascLeftHb = new Hitbox(70.0f * Settings.scale, 70.0f * Settings.scale);
        this.ascRightHb = new Hitbox(70.0f * Settings.scale, 70.0f * Settings.scale);
        this.seedHb = new Hitbox(700.0f * Settings.scale, 60.0f * Settings.scale);
        this.seedHb.move(90.0f * Settings.scale, 70.0f * Settings.scale);
    }

    private void addCharacterOption(AbstractPlayer.PlayerClass c) {
        AbstractPlayer p = CardCrawlGame.characterManager.recreateCharacter(c);
        this.options.add(p.getCharacterSelectOption());
    }

    private void positionButtons() {
        int count = this.options.size();
        float offsetX = (float)Settings.WIDTH / 2.0f - 330.0f * Settings.scale;
        for (int i = 0; i < count; ++i) {
            if (Settings.isMobile) {
                this.options.get((int)i).hb.move(offsetX + (float)i * 220.0f * Settings.scale, 230.0f * Settings.yScale);
                continue;
            }
            this.options.get((int)i).hb.move(offsetX + (float)i * 220.0f * Settings.scale, 190.0f * Settings.yScale);
        }
    }

    public void open(boolean isEndless) {
        Settings.isEndless = isEndless;
        Settings.seedSet = false;
        Settings.seed = null;
        Settings.specialSeed = null;
        Settings.isTrial = false;
        CardCrawlGame.trial = null;
        this.cancelButton.show(TEXT[5]);
        CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.CHAR_SELECT;
    }

    private void setRandomSeed() {
        long sourceTime = System.nanoTime();
        Random rng = new Random(sourceTime);
        Settings.seedSourceTimestamp = sourceTime;
        Settings.seed = SeedHelper.generateUnoffensiveSeed(rng);
        Settings.seedSet = false;
    }

    public void update() {
        if (this.ascLeftHb != null) {
            if (!Settings.isMobile) {
                this.ascLeftHb.move((float)Settings.WIDTH / 2.0f + 200.0f * Settings.scale - ASC_RIGHT_W * 0.25f, 70.0f * Settings.scale);
                this.ascRightHb.move((float)Settings.WIDTH / 2.0f + 200.0f * Settings.scale + ASC_RIGHT_W * 1.25f, 70.0f * Settings.scale);
            } else {
                this.ascLeftHb.move((float)Settings.WIDTH / 2.0f + 200.0f * Settings.scale - ASC_RIGHT_W * 0.25f, 100.0f * Settings.scale);
                this.ascRightHb.move((float)Settings.WIDTH / 2.0f + 200.0f * Settings.scale + ASC_RIGHT_W * 1.25f, 100.0f * Settings.scale);
            }
        }
        this.anySelected = false;
        if (!this.seedPanel.shown) {
            for (CharacterOption o : this.options) {
                o.update();
                if (!o.selected) continue;
                this.anySelected = true;
                this.isAscensionModeUnlocked = UnlockTracker.isAscensionUnlocked(o.c);
                if (this.isAscensionModeUnlocked) continue;
                this.isAscensionMode = false;
            }
            this.updateButtons();
            if (InputHelper.justReleasedClickLeft && !this.anySelected) {
                this.confirmButton.isDisabled = true;
                this.confirmButton.hide();
            }
            if (this.anySelected) {
                this.bgCharColor.a = MathHelper.fadeLerpSnap(this.bgCharColor.a, 1.0f);
                this.bg_y_offset = MathHelper.fadeLerpSnap(this.bg_y_offset, -0.0f);
            } else {
                this.bgCharColor.a = MathHelper.fadeLerpSnap(this.bgCharColor.a, 0.0f);
            }
            this.updateAscensionToggle();
            if (this.anySelected) {
                this.seedHb.update();
            }
        }
        this.seedPanel.update();
        if (this.seedHb.hovered && InputHelper.justClickedLeft || CInputActionSet.drawPile.isJustPressed()) {
            InputHelper.justClickedLeft = false;
            this.seedHb.hovered = false;
            this.seedPanel.show();
        }
        CardCrawlGame.mainMenuScreen.superDarken = this.anySelected;
    }

    private void updateAscensionToggle() {
        if (this.isAscensionModeUnlocked) {
            if (this.anySelected) {
                this.ascensionModeHb.update();
                this.ascRightHb.update();
                this.ascLeftHb.update();
            }
            if (InputHelper.justClickedLeft) {
                if (this.ascensionModeHb.hovered) {
                    this.ascensionModeHb.clickStarted = true;
                } else if (this.ascRightHb.hovered) {
                    this.ascRightHb.clickStarted = true;
                } else if (this.ascLeftHb.hovered) {
                    this.ascLeftHb.clickStarted = true;
                }
            }
            if (this.ascensionModeHb.clicked || CInputActionSet.proceed.isJustPressed()) {
                this.ascensionModeHb.clicked = false;
                this.isAscensionMode = !this.isAscensionMode;
                Settings.gamePref.putBoolean("Ascension Mode Default", this.isAscensionMode);
                Settings.gamePref.flush();
            }
            if (this.ascLeftHb.clicked || CInputActionSet.pageLeftViewDeck.isJustPressed()) {
                this.ascLeftHb.clicked = false;
                for (CharacterOption o : this.options) {
                    if (!o.selected) continue;
                    o.decrementAscensionLevel(this.ascensionLevel - 1);
                    break;
                }
            }
            if (this.ascRightHb.clicked || CInputActionSet.pageRightViewExhaust.isJustPressed()) {
                this.ascRightHb.clicked = false;
                for (CharacterOption o : this.options) {
                    if (!o.selected) continue;
                    o.incrementAscensionLevel(this.ascensionLevel + 1);
                    break;
                }
            }
        }
    }

    public void justSelected() {
        this.bg_y_offset = 0.0f;
    }

    public void updateButtons() {
        this.cancelButton.update();
        this.confirmButton.update();
        if (this.cancelButton.hb.clicked || InputHelper.pressedEscape) {
            CardCrawlGame.mainMenuScreen.superDarken = false;
            InputHelper.pressedEscape = false;
            this.cancelButton.hb.clicked = false;
            this.cancelButton.hide();
            CardCrawlGame.mainMenuScreen.panelScreen.refresh();
            for (CharacterOption o : this.options) {
                o.selected = false;
            }
            this.bgCharColor.a = 0.0f;
            this.anySelected = false;
        }
        if (this.confirmButton.hb.clicked) {
            this.confirmButton.hb.clicked = false;
            this.confirmButton.isDisabled = true;
            this.confirmButton.hide();
            if (Settings.seed == null) {
                this.setRandomSeed();
            } else {
                Settings.seedSet = true;
            }
            CardCrawlGame.mainMenuScreen.isFadingOut = true;
            CardCrawlGame.mainMenuScreen.fadeOutMusic();
            Settings.isDailyRun = false;
            boolean isTrialSeed = TrialHelper.isTrialSeed(SeedHelper.getString(Settings.seed));
            if (isTrialSeed) {
                Settings.specialSeed = Settings.seed;
                long sourceTime = System.nanoTime();
                Random rng = new Random(sourceTime);
                Settings.seed = SeedHelper.generateUnoffensiveSeed(rng);
                Settings.isTrial = true;
            }
            ModHelper.setModsFalse();
            AbstractDungeon.generateSeeds();
            AbstractDungeon.isAscensionMode = this.isAscensionMode;
            AbstractDungeon.ascensionLevel = this.isAscensionMode ? this.ascensionLevel : 0;
            this.confirmButton.hb.clicked = false;
            this.confirmButton.hide();
            CharacterOption selected = null;
            for (CharacterOption o : this.options) {
                if (!o.selected) continue;
                selected = o;
            }
            if (selected != null && CardCrawlGame.steelSeries.isEnabled.booleanValue()) {
                CardCrawlGame.steelSeries.event_character_chosen(selected.c.chosenClass);
            }
            if (Settings.isDemo || Settings.isPublisherBuild) {
                BotDataUploader poster = new BotDataUploader();
                poster.setValues(BotDataUploader.GameDataType.DEMO_EMBARK, null, null);
                Thread t = new Thread(poster);
                t.setName("LeaderboardPoster");
                t.run();
            }
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(this.bgCharColor);
        if (this.bgCharImg != null) {
            if (Settings.isSixteenByTen) {
                sb.draw(this.bgCharImg, (float)Settings.WIDTH / 2.0f - 960.0f, (float)Settings.HEIGHT / 2.0f - 600.0f, 960.0f, 600.0f, 1920.0f, 1200.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 1920, 1200, false, false);
            } else if (Settings.isFourByThree) {
                sb.draw(this.bgCharImg, (float)Settings.WIDTH / 2.0f - 960.0f, (float)Settings.HEIGHT / 2.0f - 600.0f + this.bg_y_offset, 960.0f, 600.0f, 1920.0f, 1200.0f, Settings.yScale, Settings.yScale, 0.0f, 0, 0, 1920, 1200, false, false);
            } else if (Settings.isLetterbox) {
                sb.draw(this.bgCharImg, (float)Settings.WIDTH / 2.0f - 960.0f, (float)Settings.HEIGHT / 2.0f - 600.0f + this.bg_y_offset, 960.0f, 600.0f, 1920.0f, 1200.0f, Settings.xScale, Settings.xScale, 0.0f, 0, 0, 1920, 1200, false, false);
            } else {
                sb.draw(this.bgCharImg, (float)Settings.WIDTH / 2.0f - 960.0f, (float)Settings.HEIGHT / 2.0f - 600.0f + this.bg_y_offset, 960.0f, 600.0f, 1920.0f, 1200.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 1920, 1200, false, false);
            }
        }
        this.cancelButton.render(sb);
        this.confirmButton.render(sb);
        this.renderAscensionMode(sb);
        this.renderSeedSettings(sb);
        this.seedPanel.render(sb);
        boolean anythingSelected = false;
        if (!this.seedPanel.shown) {
            for (CharacterOption o : this.options) {
                if (o.selected) {
                    anythingSelected = true;
                }
                o.render(sb);
            }
        }
        if (!this.seedPanel.shown && !anythingSelected) {
            if (!Settings.isMobile) {
                FontHelper.renderFontCentered(sb, FontHelper.losePowerFont, CHOOSE_CHAR_MSG, (float)Settings.WIDTH / 2.0f, 340.0f * Settings.yScale, Settings.CREAM_COLOR, 1.2f);
            } else {
                FontHelper.renderFontCentered(sb, FontHelper.losePowerFont, CHOOSE_CHAR_MSG, (float)Settings.WIDTH / 2.0f, 380.0f * Settings.yScale, Settings.CREAM_COLOR, 1.2f);
            }
        }
    }

    private void renderSeedSettings(SpriteBatch sb) {
        if (!this.anySelected) {
            return;
        }
        Color textColor = Settings.GOLD_COLOR;
        if (this.seedHb.hovered) {
            textColor = Settings.GREEN_TEXT_COLOR;
            TipHelper.renderGenericTip((float)InputHelper.mX + 50.0f * Settings.scale, (float)InputHelper.mY + 100.0f * Settings.scale, TEXT[11], TEXT[12]);
        }
        if (!Settings.isControllerMode) {
            if (Settings.seedSet) {
                FontHelper.renderSmartText(sb, FontHelper.cardTitleFont, TEXT[10], SEED_X, SEED_Y, 9999.0f, 0.0f, textColor);
                FontHelper.renderFontLeftTopAligned(sb, FontHelper.cardTitleFont, SeedHelper.getUserFacingSeedString(), SEED_X - 30.0f * Settings.scale + FontHelper.getSmartWidth(FontHelper.cardTitleFont, TEXT[13], 9999.0f, 0.0f), 90.0f * Settings.scale, Settings.BLUE_TEXT_COLOR);
            } else {
                FontHelper.renderSmartText(sb, FontHelper.cardTitleFont, TEXT[13], SEED_X, SEED_Y, 9999.0f, 0.0f, textColor);
            }
        } else {
            if (Settings.seedSet) {
                FontHelper.renderSmartText(sb, FontHelper.cardTitleFont, TEXT[10], SEED_X + 100.0f * Settings.scale, SEED_Y, 9999.0f, 0.0f, textColor);
                FontHelper.renderFontLeftTopAligned(sb, FontHelper.cardTitleFont, SeedHelper.getUserFacingSeedString(), SEED_X - 30.0f * Settings.scale + FontHelper.getSmartWidth(FontHelper.cardTitleFont, TEXT[13], 9999.0f, 0.0f) + 100.0f * Settings.scale, 90.0f * Settings.scale, Settings.BLUE_TEXT_COLOR);
            } else {
                FontHelper.renderSmartText(sb, FontHelper.cardTitleFont, TEXT[13], SEED_X + 100.0f * Settings.scale, SEED_Y, 9999.0f, 0.0f, textColor);
            }
            sb.draw(ImageMaster.CONTROLLER_LT, 80.0f * Settings.scale - 32.0f, 80.0f * Settings.scale - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        this.seedHb.render(sb);
    }

    private void renderAscensionMode(SpriteBatch sb) {
        if (!this.anySelected) {
            return;
        }
        if (this.isAscensionModeUnlocked) {
            if (!Settings.isMobile) {
                sb.draw(ImageMaster.OPTION_TOGGLE, (float)Settings.WIDTH / 2.0f - ASC_LEFT_W - 16.0f - 30.0f * Settings.scale, this.ascensionModeHb.cY - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 32, 32, false, false);
            } else {
                sb.draw(ImageMaster.CHECKBOX, (float)Settings.WIDTH / 2.0f - ASC_LEFT_W - 36.0f * Settings.scale - 32.0f, this.ascensionModeHb.cY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 0.9f, Settings.scale * 0.9f, 0.0f, 0, 0, 64, 64, false, false);
            }
            if (this.ascensionModeHb.hovered) {
                FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, TEXT[6], (float)Settings.WIDTH / 2.0f - ASC_LEFT_W / 2.0f, this.ascensionModeHb.cY, Settings.GREEN_TEXT_COLOR);
                TipHelper.renderGenericTip((float)InputHelper.mX - 140.0f * Settings.scale, (float)InputHelper.mY + 340.0f * Settings.scale, TEXT[8], TEXT[9]);
            } else {
                FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, TEXT[6], (float)Settings.WIDTH / 2.0f - ASC_LEFT_W / 2.0f, this.ascensionModeHb.cY, Settings.GOLD_COLOR);
            }
            FontHelper.renderFontCentered(sb, FontHelper.cardTitleFont, TEXT[7] + this.ascensionLevel, (float)Settings.WIDTH / 2.0f + ASC_RIGHT_W / 2.0f + 200.0f * Settings.scale, this.ascensionModeHb.cY, Settings.BLUE_TEXT_COLOR);
            if (this.isAscensionMode) {
                sb.setColor(Color.WHITE);
                if (!Settings.isMobile) {
                    sb.draw(ImageMaster.OPTION_TOGGLE_ON, (float)Settings.WIDTH / 2.0f - ASC_LEFT_W - 16.0f - 30.0f * Settings.scale, this.ascensionModeHb.cY - 16.0f, 16.0f, 16.0f, 32.0f, 32.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 32, 32, false, false);
                } else {
                    sb.draw(ImageMaster.TICK, (float)Settings.WIDTH / 2.0f - ASC_LEFT_W - 36.0f * Settings.scale - 32.0f, this.ascensionModeHb.cY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 0.9f, Settings.scale * 0.9f, 0.0f, 0, 0, 64, 64, false, false);
                }
                if (Settings.isMobile) {
                    FontHelper.renderFontCentered(sb, FontHelper.smallDialogOptionFont, this.ascLevelInfoString, (float)Settings.WIDTH / 2.0f, 60.0f * Settings.scale, Settings.CREAM_COLOR);
                } else {
                    FontHelper.renderFontCentered(sb, FontHelper.cardDescFont_N, this.ascLevelInfoString, (float)Settings.WIDTH / 2.0f, 35.0f * Settings.scale, Settings.CREAM_COLOR);
                }
            }
            if (this.ascLeftHb.hovered || Settings.isControllerMode) {
                sb.setColor(Color.WHITE);
            } else {
                sb.setColor(Color.LIGHT_GRAY);
            }
            sb.draw(ImageMaster.CF_LEFT_ARROW, this.ascLeftHb.cX - 24.0f, this.ascLeftHb.cY - 24.0f, 24.0f, 24.0f, 48.0f, 48.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 48, 48, false, false);
            if (this.ascRightHb.hovered || Settings.isControllerMode) {
                sb.setColor(Color.WHITE);
            } else {
                sb.setColor(Color.LIGHT_GRAY);
            }
            sb.draw(ImageMaster.CF_RIGHT_ARROW, this.ascRightHb.cX - 24.0f, this.ascRightHb.cY - 24.0f, 24.0f, 24.0f, 48.0f, 48.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 48, 48, false, false);
            if (Settings.isControllerMode) {
                sb.draw(CInputActionSet.proceed.getKeyImg(), this.ascensionModeHb.cX - 100.0f * Settings.scale - 32.0f, this.ascensionModeHb.cY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
                sb.draw(CInputActionSet.pageLeftViewDeck.getKeyImg(), this.ascLeftHb.cX - 60.0f * Settings.scale - 32.0f, this.ascLeftHb.cY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
                sb.draw(CInputActionSet.pageRightViewExhaust.getKeyImg(), this.ascRightHb.cX + 60.0f * Settings.scale - 32.0f, this.ascRightHb.cY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            }
            this.ascensionModeHb.render(sb);
            this.ascLeftHb.render(sb);
            this.ascRightHb.render(sb);
        }
    }

    public void deselectOtherOptions(CharacterOption characterOption) {
        for (CharacterOption o : this.options) {
            if (o == characterOption) continue;
            o.selected = false;
        }
    }

    static {
        SEED_X = 60.0f * Settings.scale;
        SEED_Y = 90.0f * Settings.scale;
        CHOOSE_CHAR_MSG = TEXT[0];
    }
}


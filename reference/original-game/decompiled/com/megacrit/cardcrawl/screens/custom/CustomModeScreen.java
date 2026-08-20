/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.custom;

import com.badlogic.gdx.Gdx;
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
import com.megacrit.cardcrawl.helpers.SeedHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.RunModStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.screens.custom.CustomMod;
import com.megacrit.cardcrawl.screens.custom.CustomModeCharacterButton;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.screens.mainMenu.MenuCancelButton;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBarListener;
import com.megacrit.cardcrawl.trials.CustomTrial;
import com.megacrit.cardcrawl.ui.buttons.GridSelectConfirmButton;
import com.megacrit.cardcrawl.ui.panels.SeedPanel;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class CustomModeScreen
implements ScrollBarListener {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("CustomModeScreen");
    public static final String[] TEXT = CustomModeScreen.uiStrings.TEXT;
    private final float imageScale;
    private MenuCancelButton cancelButton = new MenuCancelButton();
    public GridSelectConfirmButton confirmButton = new GridSelectConfirmButton(CharacterSelectScreen.TEXT[1]);
    private Hitbox controllerHb;
    public ArrayList<CustomModeCharacterButton> options = new ArrayList();
    private static float ASC_RIGHT_W;
    public static boolean finalActAvailable;
    public boolean isAscensionMode = false;
    private Hitbox ascensionModeHb;
    private Hitbox ascLeftHb;
    private Hitbox ascRightHb;
    public int ascensionLevel = 0;
    private Hitbox seedHb = new Hitbox(400.0f * Settings.scale, 90.0f * Settings.scale);
    private String currentSeed;
    private SeedPanel seedPanel;
    private ArrayList<CustomMod> modList;
    private static final String DAILY_MODS = "Daily Mods";
    private static final String MOD_BLIGHT_CHESTS = "Blight Chests";
    private static final String MOD_ONE_HIT_WONDER = "One Hit Wonder";
    private static final String MOD_PRAISE_SNECKO = "Praise Snecko";
    private static final String MOD_INCEPTION = "Inception";
    private static final String MOD_MY_TRUE_FORM = "My True Form";
    private static final String MOD_STARTER_DECK = "Starter Deck";
    private static final String NEUTRAL_COLOR = "b";
    private static final String POSITIVE_COLOR = "g";
    private static final String NEGATIVE_COLOR = "r";
    public boolean screenUp = false;
    public static float screenX;
    private float ASCENSION_TEXT_Y = 480.0f;
    private boolean grabbedScreen = false;
    private float grabStartY = 0.0f;
    private float targetY = 0.0f;
    private float scrollY = 0.0f;
    private float scrollLowerBound;
    private float scrollUpperBound;
    private ScrollBar scrollBar;

    public CustomModeScreen() {
        screenX = Settings.isMobile ? 240.0f * Settings.xScale : 300.0f * Settings.xScale;
        this.imageScale = Settings.isMobile ? Settings.scale * 1.2f : Settings.scale;
        this.initializeMods();
        this.initializeCharacters();
        this.calculateScrollBounds();
        this.scrollBar = Settings.isMobile ? new ScrollBar(this, (float)Settings.WIDTH - 280.0f * Settings.xScale - ScrollBar.TRACK_W / 2.0f, (float)Settings.HEIGHT / 2.0f, (float)Settings.HEIGHT - 256.0f * Settings.scale, true) : new ScrollBar(this, (float)Settings.WIDTH - 280.0f * Settings.xScale - ScrollBar.TRACK_W / 2.0f, (float)Settings.HEIGHT / 2.0f, (float)Settings.HEIGHT - 256.0f * Settings.scale);
        this.seedPanel = new SeedPanel();
    }

    private void initializeMods() {
        this.modList = new ArrayList();
        this.addMod(DAILY_MODS, NEUTRAL_COLOR, false);
        CustomMod draftMod = this.addDailyMod("Draft", NEUTRAL_COLOR);
        CustomMod sealedMod = this.addDailyMod("SealedDeck", NEUTRAL_COLOR);
        CustomMod endingMod = null;
        if (UnlockTracker.isAchievementUnlocked("THE_ENDING")) {
            endingMod = this.addDailyMod("The Ending", NEUTRAL_COLOR);
        }
        CustomMod endlessMod = this.addDailyMod("Endless", NEUTRAL_COLOR);
        this.addMod(MOD_BLIGHT_CHESTS, NEUTRAL_COLOR, false);
        this.addDailyMod("Hoarder", NEUTRAL_COLOR);
        CustomMod insanityMod = this.addDailyMod("Insanity", NEUTRAL_COLOR);
        this.addDailyMod("Chimera", NEUTRAL_COLOR);
        this.addMod(MOD_PRAISE_SNECKO, NEUTRAL_COLOR, false);
        CustomMod shinyMod = this.addDailyMod("Shiny", NEUTRAL_COLOR);
        this.addDailyMod("Specialized", NEUTRAL_COLOR);
        this.addDailyMod("Vintage", NEUTRAL_COLOR);
        this.addDailyMod("ControlledChaos", NEUTRAL_COLOR);
        this.addMod(MOD_INCEPTION, NEUTRAL_COLOR, false);
        this.addDailyMod("Allstar", POSITIVE_COLOR);
        CustomMod diverseMod = this.addDailyMod("Diverse", POSITIVE_COLOR);
        CustomMod redMod = this.addDailyMod("Red Cards", POSITIVE_COLOR);
        CustomMod greenMod = this.addDailyMod("Green Cards", POSITIVE_COLOR);
        CustomMod blueMod = this.addDailyMod("Blue Cards", POSITIVE_COLOR);
        CustomMod purpleMod = null;
        if (!UnlockTracker.isCharacterLocked("Watcher")) {
            purpleMod = this.addDailyMod("Purple Cards", POSITIVE_COLOR);
        }
        this.addDailyMod("Colorless Cards", POSITIVE_COLOR);
        this.addDailyMod("Heirloom", POSITIVE_COLOR);
        this.addDailyMod("Time Dilation", POSITIVE_COLOR);
        this.addDailyMod("Flight", POSITIVE_COLOR);
        this.addMod(MOD_MY_TRUE_FORM, POSITIVE_COLOR, false);
        this.addDailyMod("DeadlyEvents", NEGATIVE_COLOR);
        this.addDailyMod("Binary", NEGATIVE_COLOR);
        this.addMod(MOD_ONE_HIT_WONDER, NEGATIVE_COLOR, false);
        this.addDailyMod("Cursed Run", NEGATIVE_COLOR);
        this.addDailyMod("Elite Swarm", NEGATIVE_COLOR);
        this.addDailyMod("Lethality", NEGATIVE_COLOR);
        this.addDailyMod("Midas", NEGATIVE_COLOR);
        this.addDailyMod("Night Terrors", NEGATIVE_COLOR);
        this.addDailyMod("Terminal", NEGATIVE_COLOR);
        this.addDailyMod("Uncertain Future", NEGATIVE_COLOR);
        this.addMod(MOD_STARTER_DECK, NEGATIVE_COLOR, false);
        if (endingMod != null) {
            endingMod.setMutualExclusionPair(endlessMod);
        }
        insanityMod.setMutualExclusionPair(shinyMod);
        sealedMod.setMutualExclusionPair(draftMod);
        diverseMod.setMutualExclusionPair(redMod);
        diverseMod.setMutualExclusionPair(greenMod);
        diverseMod.setMutualExclusionPair(blueMod);
        if (purpleMod != null) {
            diverseMod.setMutualExclusionPair(purpleMod);
        }
    }

    private CustomMod addMod(String id, String color, boolean isDailyMod) {
        RunModStrings modString = CardCrawlGame.languagePack.getRunModString(id);
        if (modString != null) {
            CustomMod mod = new CustomMod(id, color, isDailyMod);
            this.modList.add(mod);
            return mod;
        }
        return null;
    }

    private CustomMod addDailyMod(String id, String color) {
        return this.addMod(id, color, true);
    }

    public void open() {
        this.confirmButton.show();
        this.controllerHb = null;
        this.targetY = 0.0f;
        this.screenUp = true;
        Settings.seed = null;
        Settings.specialSeed = null;
        CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.CUSTOM;
        CardCrawlGame.mainMenuScreen.darken();
        this.cancelButton.show(CharacterSelectScreen.TEXT[5]);
        this.confirmButton.isDisabled = false;
        ASC_RIGHT_W = FontHelper.getSmartWidth(FontHelper.charDescFont, TEXT[4] + "22", 9999.0f, 0.0f) * Settings.xScale;
        this.ascensionModeHb = new Hitbox(80.0f * Settings.scale, 80.0f * Settings.scale);
        this.ascensionModeHb.move(screenX + 130.0f * Settings.xScale, this.scrollY + this.ASCENSION_TEXT_Y * Settings.scale);
        this.ascLeftHb = new Hitbox(95.0f * Settings.scale, 95.0f * Settings.scale);
        this.ascRightHb = new Hitbox(95.0f * Settings.scale, 95.0f * Settings.scale);
        this.ascLeftHb.move(screenX + ASC_RIGHT_W * 1.1f + 250.0f * Settings.xScale, this.scrollY + this.ASCENSION_TEXT_Y * Settings.scale);
        this.ascRightHb.move(screenX + ASC_RIGHT_W * 1.1f + 350.0f * Settings.xScale, this.scrollY + this.ASCENSION_TEXT_Y * Settings.scale);
    }

    public void initializeCharacters() {
        this.options.clear();
        this.options.add(new CustomModeCharacterButton(CardCrawlGame.characterManager.setChosenCharacter(AbstractPlayer.PlayerClass.IRONCLAD), false));
        this.options.add(new CustomModeCharacterButton(CardCrawlGame.characterManager.setChosenCharacter(AbstractPlayer.PlayerClass.THE_SILENT), UnlockTracker.isCharacterLocked("The Silent")));
        this.options.add(new CustomModeCharacterButton(CardCrawlGame.characterManager.setChosenCharacter(AbstractPlayer.PlayerClass.DEFECT), UnlockTracker.isCharacterLocked("Defect")));
        this.options.add(new CustomModeCharacterButton(CardCrawlGame.characterManager.setChosenCharacter(AbstractPlayer.PlayerClass.WATCHER), UnlockTracker.isCharacterLocked("Watcher")));
        int count = this.options.size();
        for (int i = 0; i < count; ++i) {
            this.options.get(i).move(screenX + (float)i * 100.0f * Settings.scale - 200.0f * Settings.xScale, this.scrollY - 80.0f * Settings.scale);
        }
        this.options.get((int)0).hb.clicked = true;
    }

    public void update() {
        this.updateControllerInput();
        if (Settings.isControllerMode && this.controllerHb != null) {
            if ((float)Gdx.input.getY() > (float)Settings.HEIGHT * 0.75f) {
                this.targetY += Settings.SCROLL_SPEED;
            } else if ((float)Gdx.input.getY() < (float)Settings.HEIGHT * 0.25f) {
                this.targetY -= Settings.SCROLL_SPEED;
            }
        }
        this.seedPanel.update();
        if (!this.seedPanel.shown) {
            boolean isDraggingScrollBar = this.scrollBar.update();
            if (!isDraggingScrollBar) {
                this.updateScrolling();
            }
            this.updateCharacterButtons();
            this.updateAscension();
            this.updateSeed();
            this.updateMods();
            this.updateEmbarkButton();
            this.updateCancelButton();
        }
        this.currentSeed = SeedHelper.getUserFacingSeedString();
        if (Settings.isControllerMode && this.controllerHb != null) {
            CInputHelper.setCursor(this.controllerHb);
        }
    }

    private void updateCancelButton() {
        this.cancelButton.update();
        if (this.cancelButton.hb.clicked || InputHelper.pressedEscape) {
            InputHelper.pressedEscape = false;
            this.cancelButton.hb.clicked = false;
            this.cancelButton.hide();
            CardCrawlGame.mainMenuScreen.panelScreen.refresh();
        }
    }

    private void updateEmbarkButton() {
        this.confirmButton.update();
        if (this.confirmButton.hb.clicked || CInputActionSet.proceed.isJustPressed()) {
            this.confirmButton.hb.clicked = false;
            for (CustomModeCharacterButton b : this.options) {
                if (!b.selected) continue;
                CardCrawlGame.chosenCharacter = b.c.chosenClass;
                break;
            }
            CardCrawlGame.mainMenuScreen.isFadingOut = true;
            CardCrawlGame.mainMenuScreen.fadeOutMusic();
            Settings.isTrial = true;
            Settings.isDailyRun = false;
            Settings.isEndless = false;
            finalActAvailable = false;
            AbstractDungeon.isAscensionMode = this.isAscensionMode;
            AbstractDungeon.ascensionLevel = !this.isAscensionMode ? 0 : this.ascensionLevel;
            if (this.currentSeed.isEmpty()) {
                long sourceTime = System.nanoTime();
                Random rng = new Random(sourceTime);
                Settings.seed = SeedHelper.generateUnoffensiveSeed(rng);
            }
            AbstractDungeon.generateSeeds();
            CustomTrial trial = new CustomTrial();
            trial.addDailyMods(this.getActiveDailyModIds());
            this.addNonDailyMods(trial, this.getActiveNonDailyMods());
            Settings.isEndless = trial.dailyModIDs().contains("Endless");
            finalActAvailable = trial.dailyModIDs().contains("The Ending");
            CardCrawlGame.trial = trial;
            AbstractPlayer.customMods = CardCrawlGame.trial.dailyModIDs();
        }
    }

    private void updateCharacterButtons() {
        for (int i = 0; i < this.options.size(); ++i) {
            if (Settings.isMobile) {
                this.options.get(i).update(screenX + (float)i * 130.0f * Settings.xScale + 130.0f * Settings.scale, this.scrollY + 640.0f * Settings.scale);
                continue;
            }
            this.options.get(i).update(screenX + (float)i * 100.0f * Settings.xScale + 130.0f * Settings.scale, this.scrollY + 640.0f * Settings.scale);
        }
    }

    private void updateSeed() {
        this.seedHb.move(screenX + 280.0f * Settings.xScale, this.scrollY + 320.0f * Settings.scale);
        this.seedHb.update();
        if (this.seedHb.justHovered) {
            this.playHoverSound();
        }
        if (this.seedHb.hovered && InputHelper.justClickedLeft) {
            this.seedHb.clickStarted = true;
        }
        if (this.seedHb.clicked || CInputActionSet.select.isJustPressed() && this.seedHb.hovered) {
            this.seedHb.clicked = false;
            if (Settings.seed == null) {
                Settings.seed = 0L;
            }
            this.seedPanel.show(MainMenuScreen.CurScreen.CUSTOM);
        }
    }

    private void updateAscension() {
        this.ascLeftHb.moveY(this.scrollY + this.ASCENSION_TEXT_Y * Settings.scale);
        this.ascRightHb.moveY(this.scrollY + this.ASCENSION_TEXT_Y * Settings.scale);
        this.ascensionModeHb.moveY(this.scrollY + this.ASCENSION_TEXT_Y * Settings.scale);
        this.ascensionModeHb.update();
        this.ascLeftHb.update();
        this.ascRightHb.update();
        if (this.ascensionModeHb.justHovered || this.ascRightHb.justHovered || this.ascLeftHb.justHovered) {
            this.playHoverSound();
        }
        if (this.ascensionModeHb.hovered && InputHelper.justClickedLeft) {
            this.playClickStartSound();
            this.ascensionModeHb.clickStarted = true;
        } else if (this.ascLeftHb.hovered && InputHelper.justClickedLeft) {
            this.playClickStartSound();
            this.ascLeftHb.clickStarted = true;
        } else if (this.ascRightHb.hovered && InputHelper.justClickedLeft) {
            this.playClickStartSound();
            this.ascRightHb.clickStarted = true;
        }
        if (this.ascensionModeHb.clicked || CInputActionSet.topPanel.isJustPressed()) {
            CInputActionSet.topPanel.unpress();
            this.playClickFinishSound();
            this.ascensionModeHb.clicked = false;
            boolean bl = this.isAscensionMode = !this.isAscensionMode;
            if (this.isAscensionMode && this.ascensionLevel == 0) {
                this.ascensionLevel = 1;
            }
        } else if (this.ascLeftHb.clicked || CInputActionSet.pageLeftViewDeck.isJustPressed()) {
            this.playClickFinishSound();
            this.ascLeftHb.clicked = false;
            --this.ascensionLevel;
            if (this.ascensionLevel < 1) {
                this.ascensionLevel = 20;
            }
        } else if (this.ascRightHb.clicked || CInputActionSet.pageRightViewExhaust.isJustPressed()) {
            this.playClickFinishSound();
            this.ascRightHb.clicked = false;
            ++this.ascensionLevel;
            if (this.ascensionLevel > 20) {
                this.ascensionLevel = 1;
            }
            this.isAscensionMode = true;
        }
    }

    private void updateMods() {
        float offset = 0.0f;
        for (int i = 0; i < this.modList.size(); ++i) {
            this.modList.get(i).update(this.scrollY + offset);
            offset -= this.modList.get((int)i).height;
        }
    }

    private ArrayList<String> getActiveDailyModIds() {
        ArrayList<String> active = new ArrayList<String>();
        for (CustomMod mod : this.modList) {
            if (!mod.selected || !mod.isDailyMod) continue;
            active.add(mod.ID);
        }
        return active;
    }

    private ArrayList<String> getActiveNonDailyMods() {
        ArrayList<String> active = new ArrayList<String>();
        for (CustomMod mod : this.modList) {
            if (!mod.selected || mod.isDailyMod) continue;
            active.add(mod.ID);
        }
        return active;
    }

    private void addNonDailyMods(CustomTrial trial, ArrayList<String> modIds) {
        Iterator<String> iterator = modIds.iterator();
        while (iterator.hasNext()) {
            String modId;
            switch (modId = iterator.next()) {
                case "Daily Mods": {
                    trial.setRandomDailyMods();
                    break;
                }
                case "One Hit Wonder": {
                    trial.setMaxHpOverride(1);
                    break;
                }
                case "Praise Snecko": {
                    trial.addStarterRelic("Snecko Eye");
                    trial.setShouldKeepStarterRelic(false);
                    break;
                }
                case "Inception": {
                    trial.addStarterRelic("Unceasing Top");
                    trial.setShouldKeepStarterRelic(false);
                    break;
                }
                case "My True Form": {
                    trial.addStarterCards(Arrays.asList("Demon Form", "Wraith Form v2", "Echo Form", "DevaForm"));
                    break;
                }
                case "Starter Deck": {
                    trial.addStarterRelic("Busted Crown");
                    trial.addDailyMod("Binary");
                    break;
                }
                case "Blight Chests": {
                    trial.addDailyMod(MOD_BLIGHT_CHESTS);
                    break;
                }
            }
        }
    }

    private void playClickStartSound() {
        CardCrawlGame.sound.playA("UI_CLICK_1", -0.1f);
    }

    private void playClickFinishSound() {
        CardCrawlGame.sound.playA("UI_CLICK_1", -0.1f);
    }

    private void playHoverSound() {
        CardCrawlGame.sound.playV("UI_HOVER", 0.75f);
    }

    public void render(SpriteBatch sb) {
        this.renderScreen(sb);
        this.scrollBar.render(sb);
        this.cancelButton.render(sb);
        this.confirmButton.render(sb);
        for (CustomModeCharacterButton o : this.options) {
            o.render(sb);
        }
        this.renderAscension(sb);
        this.renderSeed(sb);
        sb.setColor(Color.WHITE);
        for (CustomMod m : this.modList) {
            m.render(sb);
        }
        this.seedPanel.render(sb);
    }

    public void renderScreen(SpriteBatch sb) {
        this.renderTitle(sb, TEXT[0], this.scrollY - 50.0f * Settings.scale);
        this.renderHeader(sb, TEXT[2], this.scrollY - 120.0f * Settings.scale);
        this.renderHeader(sb, TEXT[3], this.scrollY - 290.0f * Settings.scale);
        this.renderHeader(sb, TEXT[7], this.scrollY - 460.0f * Settings.scale);
        this.renderHeader(sb, TEXT[6], this.scrollY - 630.0f * Settings.scale);
    }

    private void renderAscension(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        if (this.ascensionModeHb.hovered) {
            sb.draw(ImageMaster.CHECKBOX, this.ascensionModeHb.cX - 32.0f, this.ascensionModeHb.cY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.imageScale * 1.2f, this.imageScale * 1.2f, 0.0f, 0, 0, 64, 64, false, false);
            sb.setColor(Color.GOLD);
            sb.setBlendFunction(770, 1);
            sb.draw(ImageMaster.CHECKBOX, this.ascensionModeHb.cX - 32.0f, this.ascensionModeHb.cY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.imageScale * 1.2f, this.imageScale * 1.2f, 0.0f, 0, 0, 64, 64, false, false);
            sb.setBlendFunction(770, 771);
        } else {
            sb.draw(ImageMaster.CHECKBOX, this.ascensionModeHb.cX - 32.0f, this.ascensionModeHb.cY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.imageScale, this.imageScale, 0.0f, 0, 0, 64, 64, false, false);
        }
        if (this.ascensionModeHb.hovered) {
            FontHelper.renderFontCentered(sb, FontHelper.charDescFont, TEXT[4] + this.ascensionLevel, screenX + 240.0f * Settings.scale, this.scrollY + this.ASCENSION_TEXT_Y * Settings.scale, Color.CYAN);
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.charDescFont, TEXT[4] + this.ascensionLevel, screenX + 240.0f * Settings.scale, this.scrollY + this.ASCENSION_TEXT_Y * Settings.scale, Settings.BLUE_TEXT_COLOR);
        }
        if (this.isAscensionMode) {
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.TICK, this.ascensionModeHb.cX - 32.0f, this.ascensionModeHb.cY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, this.imageScale, this.imageScale, 0.0f, 0, 0, 64, 64, false, false);
        }
        if (this.ascensionLevel != 0) {
            CardCrawlGame.mainMenuScreen.charSelectScreen.ascLevelInfoString = CharacterSelectScreen.A_TEXT[this.ascensionLevel - 1];
            FontHelper.renderSmartText(sb, FontHelper.charDescFont, CardCrawlGame.mainMenuScreen.charSelectScreen.ascLevelInfoString, screenX + ASC_RIGHT_W * 1.1f + 400.0f * Settings.scale, this.ascensionModeHb.cY + 10.0f * Settings.scale, 9999.0f, 32.0f * Settings.scale, Settings.CREAM_COLOR);
        }
        if (this.ascLeftHb.hovered || Settings.isControllerMode) {
            sb.setColor(Color.WHITE);
        } else {
            sb.setColor(Color.LIGHT_GRAY);
        }
        sb.draw(ImageMaster.CF_LEFT_ARROW, this.ascLeftHb.cX - 24.0f, this.ascLeftHb.cY - 24.0f, 24.0f, 24.0f, 48.0f, 48.0f, this.imageScale, this.imageScale, 0.0f, 0, 0, 48, 48, false, false);
        if (this.ascRightHb.hovered || Settings.isControllerMode) {
            sb.setColor(Color.WHITE);
        } else {
            sb.setColor(Color.LIGHT_GRAY);
        }
        sb.draw(ImageMaster.CF_RIGHT_ARROW, this.ascRightHb.cX - 24.0f, this.ascRightHb.cY - 24.0f, 24.0f, 24.0f, 48.0f, 48.0f, this.imageScale, this.imageScale, 0.0f, 0, 0, 48, 48, false, false);
        if (Settings.isControllerMode) {
            sb.draw(CInputActionSet.topPanel.getKeyImg(), this.ascensionModeHb.cX - 64.0f * Settings.scale - 32.0f, this.ascensionModeHb.cY - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            sb.draw(CInputActionSet.pageLeftViewDeck.getKeyImg(), this.ascLeftHb.cX - 12.0f * Settings.scale - 32.0f, this.ascLeftHb.cY + 40.0f * Settings.scale - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            sb.draw(CInputActionSet.pageRightViewExhaust.getKeyImg(), this.ascRightHb.cX + 12.0f * Settings.scale - 32.0f, this.ascRightHb.cY + 40.0f * Settings.scale - 32.0f, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        this.ascensionModeHb.render(sb);
        this.ascLeftHb.render(sb);
        this.ascRightHb.render(sb);
    }

    private void renderSeed(SpriteBatch sb) {
        if (this.seedHb.hovered) {
            FontHelper.renderSmartText(sb, FontHelper.panelNameFont, TEXT[8] + ": " + this.currentSeed, screenX + 96.0f * Settings.scale, this.seedHb.cY, 9999.0f, 32.0f * Settings.scale, Settings.GREEN_TEXT_COLOR);
        } else {
            FontHelper.renderSmartText(sb, FontHelper.turnNumFont, TEXT[8] + ": " + this.currentSeed, screenX + 96.0f * Settings.scale, this.seedHb.cY, 9999.0f, 32.0f * Settings.scale, Settings.BLUE_TEXT_COLOR);
        }
        this.seedHb.render(sb);
    }

    private void renderHeader(SpriteBatch sb, String text, float y) {
        if (Settings.isMobile) {
            FontHelper.renderSmartText(sb, FontHelper.panelNameFont, text, screenX + 50.0f * Settings.scale, y + 850.0f * Settings.scale, 9999.0f, 32.0f * Settings.scale, Settings.GOLD_COLOR, 1.2f);
        } else {
            FontHelper.renderSmartText(sb, FontHelper.panelNameFont, text, screenX + 50.0f * Settings.scale, y + 850.0f * Settings.scale, 9999.0f, 32.0f * Settings.scale, Settings.GOLD_COLOR);
        }
    }

    private void renderTitle(SpriteBatch sb, String text, float y) {
        FontHelper.renderSmartText(sb, FontHelper.charTitleFont, text, screenX, y + 900.0f * Settings.scale, 9999.0f, 32.0f * Settings.scale, Settings.GOLD_COLOR);
        if (!Settings.usesTrophies) {
            FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, TEXT[1], screenX + FontHelper.getSmartWidth(FontHelper.charTitleFont, text, 9999.0f, 9999.0f) + 18.0f * Settings.scale, y + 888.0f * Settings.scale, 9999.0f, 32.0f * Settings.scale, Settings.RED_TEXT_COLOR);
        } else {
            FontHelper.renderSmartText(sb, FontHelper.tipBodyFont, TEXT[9], screenX + FontHelper.getSmartWidth(FontHelper.charTitleFont, text, 9999.0f, 9999.0f) + 18.0f * Settings.scale, y + 888.0f * Settings.scale, 9999.0f, 32.0f * Settings.scale, Settings.RED_TEXT_COLOR);
        }
    }

    private void updateControllerInput() {
        if (!Settings.isControllerMode) {
            return;
        }
        CSelectionType type = CSelectionType.CHARACTER;
        boolean anyHovered = false;
        int index = 0;
        for (CustomModeCharacterButton b : this.options) {
            if (b.hb.hovered) {
                anyHovered = true;
                break;
            }
            ++index;
        }
        if (!anyHovered && this.ascensionModeHb.hovered) {
            anyHovered = true;
            type = CSelectionType.ASCENSION;
        }
        if (!anyHovered && this.seedHb.hovered) {
            anyHovered = true;
            type = CSelectionType.SEED;
        }
        if (!anyHovered) {
            index = 0;
            for (CustomMod m : this.modList) {
                if (m.hb.hovered) {
                    anyHovered = true;
                    type = CSelectionType.MODIFIERS;
                    break;
                }
                ++index;
            }
        }
        if (!anyHovered && this.controllerHb == null) {
            CInputHelper.setCursor(this.options.get((int)0).hb);
            this.controllerHb = this.options.get((int)0).hb;
        } else {
            switch (type) {
                case CHARACTER: {
                    if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
                        if (++index > this.options.size() - 1) {
                            index = this.options.size() - 1;
                        }
                        CInputHelper.setCursor(this.options.get((int)index).hb);
                        this.controllerHb = this.options.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                        if (--index < 0) {
                            index = 0;
                        }
                        CInputHelper.setCursor(this.options.get((int)index).hb);
                        this.controllerHb = this.options.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                        CInputHelper.setCursor(this.ascensionModeHb);
                        this.controllerHb = this.ascensionModeHb;
                        break;
                    }
                    if (!CInputActionSet.select.isJustPressed()) break;
                    CInputActionSet.select.unpress();
                    this.options.get((int)index).hb.clicked = true;
                    break;
                }
                case ASCENSION: {
                    if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                        CInputHelper.setCursor(this.options.get((int)0).hb);
                        this.controllerHb = this.options.get((int)0).hb;
                        break;
                    }
                    if (!CInputActionSet.down.isJustPressed() && !CInputActionSet.altDown.isJustPressed()) break;
                    CInputHelper.setCursor(this.seedHb);
                    this.controllerHb = this.seedHb;
                    break;
                }
                case SEED: {
                    if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                        CInputHelper.setCursor(this.ascensionModeHb);
                        this.controllerHb = this.ascensionModeHb;
                        break;
                    }
                    if (!CInputActionSet.down.isJustPressed() && !CInputActionSet.altDown.isJustPressed()) break;
                    CInputHelper.setCursor(this.modList.get((int)0).hb);
                    this.controllerHb = this.modList.get((int)0).hb;
                    break;
                }
                case MODIFIERS: {
                    if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                        if (--index < 0) {
                            CInputHelper.setCursor(this.seedHb);
                            this.controllerHb = this.seedHb;
                            break;
                        }
                        CInputHelper.setCursor(this.modList.get((int)index).hb);
                        this.controllerHb = this.modList.get((int)index).hb;
                        break;
                    }
                    if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                        if (++index > this.modList.size() - 1) {
                            index = this.modList.size() - 1;
                        }
                        CInputHelper.setCursor(this.modList.get((int)index).hb);
                        this.controllerHb = this.modList.get((int)index).hb;
                        break;
                    }
                    if (!CInputActionSet.select.isJustPressed()) break;
                    CInputActionSet.select.unpress();
                    this.modList.get((int)index).hb.clicked = true;
                }
            }
        }
    }

    private void updateScrolling() {
        int y = InputHelper.mY;
        if (this.scrollUpperBound > 0.0f) {
            if (!this.grabbedScreen) {
                if (InputHelper.scrolledDown) {
                    this.targetY += Settings.SCROLL_SPEED;
                } else if (InputHelper.scrolledUp) {
                    this.targetY -= Settings.SCROLL_SPEED;
                }
                if (InputHelper.justClickedLeft) {
                    this.grabbedScreen = true;
                    this.grabStartY = (float)y - this.targetY;
                }
            } else if (InputHelper.isMouseDown) {
                this.targetY = (float)y - this.grabStartY;
            } else {
                this.grabbedScreen = false;
            }
        }
        this.scrollY = MathHelper.scrollSnapLerpSpeed(this.scrollY, this.targetY);
        if (this.targetY < this.scrollLowerBound) {
            this.targetY = MathHelper.scrollSnapLerpSpeed(this.targetY, this.scrollLowerBound);
        } else if (this.targetY > this.scrollUpperBound) {
            this.targetY = MathHelper.scrollSnapLerpSpeed(this.targetY, this.scrollUpperBound);
        }
        this.updateBarPosition();
    }

    private void calculateScrollBounds() {
        this.scrollUpperBound = (float)this.modList.size() * 90.0f * Settings.scale + 270.0f * Settings.scale;
        this.scrollLowerBound = 100.0f * Settings.scale;
    }

    @Override
    public void scrolledUsingBar(float newPercent) {
        float newPosition;
        this.scrollY = newPosition = MathHelper.valueFromPercentBetween(this.scrollLowerBound, this.scrollUpperBound, newPercent);
        this.targetY = newPosition;
        this.updateBarPosition();
    }

    private void updateBarPosition() {
        float percent = MathHelper.percentFromValueBetween(this.scrollLowerBound, this.scrollUpperBound, this.scrollY);
        this.scrollBar.parentScrolledToPercent(percent);
    }

    public void deselectOtherOptions(CustomModeCharacterButton characterOption) {
        for (CustomModeCharacterButton o : this.options) {
            if (o == characterOption) continue;
            o.selected = false;
        }
    }

    static {
        finalActAvailable = false;
    }

    private static enum CSelectionType {
        CHARACTER,
        ASCENSION,
        SEED,
        MODIFIERS;

    }
}


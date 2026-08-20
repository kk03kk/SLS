/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.daily.DailyScreen;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.SeedHelper;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.PotionSlot;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import com.megacrit.cardcrawl.screens.stats.CharStat;
import com.megacrit.cardcrawl.ui.panels.PotionPopUp;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.FadeWipeParticle;
import com.megacrit.cardcrawl.vfx.combat.HealPanelEffect;
import com.megacrit.cardcrawl.vfx.combat.PingHpEffect;
import de.robojumper.ststwitch.TwitchConfig;
import de.robojumper.ststwitch.TwitchConnection;
import de.robojumper.ststwitch.TwitchPanel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopPanel {
    private static final TutorialStrings tutorialStrings = CardCrawlGame.languagePack.getTutorialString("Top Panel Tips");
    public static final String[] MSG = TopPanel.tutorialStrings.TEXT;
    public static final String[] LABEL = TopPanel.tutorialStrings.LABEL;
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("TopPanel");
    public static final String[] TEXT = TopPanel.uiStrings.TEXT;
    private static final float TOPBAR_H = Settings.isMobile ? 164.0f * Settings.scale : 128.0f * Settings.scale;
    private static final float TIP_Y = (float)Settings.HEIGHT - 120.0f * Settings.scale;
    private static final float TIP_OFF_X = 140.0f * Settings.scale;
    private static final float ICON_W = 64.0f * Settings.scale;
    private static final float ICON_Y = Settings.isMobile ? (float)Settings.HEIGHT - ICON_W - 12.0f * Settings.scale : (float)Settings.HEIGHT - ICON_W;
    private static final float INFO_TEXT_Y = Settings.isMobile ? (float)Settings.HEIGHT - 36.0f * Settings.scale : (float)Settings.HEIGHT - 24.0f * Settings.scale;
    private String name;
    private String title;
    private static final float NAME_Y = Settings.isMobile ? (float)Settings.HEIGHT - 12.0f * Settings.scale : (float)Settings.HEIGHT - 20.0f * Settings.scale;
    private GlyphLayout gl = new GlyphLayout();
    private float nameX;
    private float titleX;
    private float titleY;
    private static final Color DISABLED_BTN_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.4f);
    private static final float TOP_RIGHT_TIP_X = 1550.0f * Settings.scale;
    private static final float TOP_RIGHT_PAD_X = 10.0f * Settings.scale;
    private static final float SETTINGS_X = (float)Settings.WIDTH - (ICON_W + TOP_RIGHT_PAD_X) * 1.0f;
    private float settingsAngle = 0.0f;
    private static final float DECK_X = (float)Settings.WIDTH - (ICON_W + TOP_RIGHT_PAD_X) * 2.0f;
    private float deckAngle = 0.0f;
    private static final float MAP_X = (float)Settings.WIDTH - (ICON_W + TOP_RIGHT_PAD_X) * 3.0f;
    private float mapAngle = -5.0f;
    private boolean settingsButtonDisabled = true;
    private boolean deckButtonDisabled = true;
    private boolean mapButtonDisabled = true;
    private float rotateTimer = 0.0f;
    private float hpIconX;
    private static final float HP_TIP_W = 150.0f * Settings.scale;
    private static final float HP_NUM_OFFSET_X = 60.0f * Settings.scale;
    private float goldIconX;
    private static final float GOLD_TIP_W = 120.0f * Settings.scale;
    private static final float GOLD_NUM_OFFSET_X = 65.0f * Settings.scale;
    private static Texture potionSelectBox = null;
    public PotionPopUp potionUi = new PotionPopUp();
    private static final float POTION_PITCH_VAR = 0.1f;
    public static float potionX;
    private float flashRedTimer = 0.0f;
    private static final float FLASH_RED_TIME = 1.0f;
    public boolean potionCombine = false;
    public int combinePotionSlot = 0;
    public Hitbox leftScrollHb = new Hitbox(64.0f * Settings.scale, 64.0f * Settings.scale);
    public Hitbox rightScrollHb = new Hitbox(64.0f * Settings.scale, 64.0f * Settings.scale);
    public boolean selectPotionMode = false;
    private TopSection section = TopSection.NONE;
    private String ascensionString = "";
    private static float floorX;
    private static float dailyModX;
    public Hitbox settingsHb;
    public Hitbox deckHb;
    public Hitbox mapHb;
    public Hitbox goldHb;
    public Hitbox hpHb;
    public Hitbox ascensionHb;
    public Hitbox timerHb;
    public Hitbox[] modHbs = new Hitbox[0];
    public Optional<TwitchPanel> twitch;
    private static final Logger logger;

    public TopPanel() {
        this.settingsHb = new Hitbox(ICON_W, ICON_W);
        this.deckHb = new Hitbox(ICON_W, ICON_W);
        this.mapHb = new Hitbox(ICON_W, ICON_W);
        this.ascensionHb = new Hitbox(210.0f * Settings.scale, ICON_W);
        this.timerHb = new Hitbox(140.0f * Settings.scale, ICON_W);
        this.timerHb.move(1610.0f * Settings.scale, ICON_Y + ICON_W / 2.0f);
        this.settingsHb.move(SETTINGS_X + ICON_W / 2.0f, ICON_Y + ICON_W / 2.0f);
        this.deckHb.move(DECK_X + ICON_W / 2.0f, ICON_Y + ICON_W / 2.0f);
        this.mapHb.move(MAP_X + ICON_W / 2.0f, ICON_Y + ICON_W / 2.0f);
        this.leftScrollHb.move(32.0f * Settings.scale, (float)Settings.HEIGHT - 102.0f * Settings.scale);
        this.rightScrollHb.move((float)Settings.WIDTH - 32.0f * Settings.scale, (float)Settings.HEIGHT - 102.0f * Settings.scale);
        if (potionSelectBox == null) {
            potionSelectBox = ImageMaster.loadImage("images/ui/potionPopUp/potionSelectBox.png");
        }
        if (TwitchConfig.configFileExists()) {
            logger.info("Twitch Integration enabled due to presence of 'twitch.properties` file.");
            Optional<TwitchConfig> optTwitchConfig = TwitchConfig.readConfig();
            if (optTwitchConfig.isPresent()) {
                TwitchConfig twitchConfig = optTwitchConfig.get();
                if (twitchConfig.isEnabled()) {
                    try {
                        this.twitch = Optional.of(new TwitchPanel(new TwitchConnection(twitchConfig)));
                    }
                    catch (IOException e) {
                        logger.info("ERROR: ", (Throwable)e);
                        this.twitch = Optional.empty();
                    }
                    logger.info("Started Twitch Panel");
                } else {
                    logger.info("Not starting twitch integration because enabled=" + twitchConfig.isEnabled());
                    this.twitch = Optional.empty();
                }
            } else {
                logger.info("Not starting twitch integration because config is invalid.");
                this.twitch = Optional.empty();
            }
        } else {
            logger.info("Twitch Integration disabled due to missing 'twitch.properties` file.");
            this.twitch = Optional.empty();
        }
    }

    public void setPlayerName() {
        this.name = CardCrawlGame.playerName;
        this.nameX = !Settings.isEndless && !Settings.isFinalActAvailable ? 24.0f * Settings.scale : 88.0f * Settings.scale;
        this.title = AbstractDungeon.player.title;
        this.gl.setText(FontHelper.panelNameFont, this.name);
        this.titleX = Settings.isMobile ? this.nameX + 42.0f * Settings.scale : this.gl.width + this.nameX + 18.0f * Settings.scale;
        this.titleY = (float)Settings.HEIGHT - 26.0f * Settings.scale;
        this.gl.setText(FontHelper.tipBodyFont, this.title);
        this.hpIconX = this.titleX + this.gl.width + 20.0f * Settings.scale;
        this.goldIconX = Settings.isMobile ? this.hpIconX + 172.0f * Settings.scale : this.hpIconX + 162.0f * Settings.scale;
        this.gl.reset();
        potionX = Settings.isMobile ? this.goldIconX + 166.0f * Settings.scale : this.goldIconX + 154.0f * Settings.scale;
        floorX = potionX + 310.0f * Settings.scale;
        dailyModX = floorX + 366.0f * Settings.scale;
        int index = 0;
        for (AbstractPotion tmpPotion : AbstractDungeon.player.potions) {
            tmpPotion.adjustPosition(index);
            ++index;
        }
        this.setupAscensionMode();
        if (ModHelper.enabledMods.size() > 0) {
            this.modHbs = new Hitbox[ModHelper.enabledMods.size()];
            for (int i = 0; i < this.modHbs.length; ++i) {
                this.modHbs[i] = new Hitbox(52.0f * Settings.scale, ICON_W);
                this.modHbs[i].move(dailyModX + (float)i * 52.0f * Settings.scale, ICON_Y + ICON_W / 2.0f);
            }
        }
        this.twitch.ifPresent(twitchPanel -> twitchPanel.setPosition(floorX - 80.0f * Settings.scale, Settings.HEIGHT));
        AbstractDungeon.player.adjustPotionPositions();
        this.adjustHitboxes();
    }

    public void setupAscensionMode() {
        if (!AbstractDungeon.isAscensionMode) {
            return;
        }
        this.ascensionHb.move(floorX + 100.0f * Settings.scale, ICON_Y + ICON_W / 2.0f);
        if (AbstractDungeon.isAscensionMode) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < AbstractDungeon.ascensionLevel; ++i) {
                sb.append(CharacterSelectScreen.A_TEXT[i]);
                if (i == AbstractDungeon.ascensionLevel - 1) continue;
                sb.append(" NL ");
            }
            this.ascensionString = sb.toString();
        }
    }

    public void unhoverHitboxes() {
        this.settingsHb.unhover();
        this.deckHb.unhover();
        this.mapHb.unhover();
        this.goldHb.unhover();
        this.hpHb.unhover();
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            r.hb.unhover();
        }
        this.twitch.ifPresent(TwitchPanel::unhover);
    }

    private void adjustHitboxes() {
        this.hpHb = new Hitbox(HP_TIP_W, ICON_W);
        this.hpHb.move(this.hpIconX + HP_TIP_W / 2.0f, ICON_Y + ICON_W / 2.0f);
        this.goldHb = new Hitbox(GOLD_TIP_W, ICON_W);
        this.goldHb.move(this.goldIconX + GOLD_TIP_W / 2.0f, ICON_Y + ICON_W / 2.0f);
    }

    public void update() {
        if (AbstractDungeon.screen != null && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.UNLOCK && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.NO_INTERACT) {
            AbstractRelic.updateOffsetX();
            this.updateGold();
            this.updatePotions();
            this.updateRelics();
            this.potionUi.update();
            if (Settings.isControllerMode) {
                if (CInputActionSet.topPanel.isJustPressed() && !CardCrawlGame.isPopupOpen && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.SETTINGS && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.FTUE) {
                    CInputActionSet.topPanel.unpress();
                    AbstractDungeon.player.viewingRelics = false;
                    boolean bl = this.selectPotionMode = !this.selectPotionMode;
                    if (!this.selectPotionMode) {
                        Gdx.input.setCursorPosition(0, 0);
                    } else {
                        AbstractDungeon.player.releaseCard();
                        CInputHelper.setCursor(AbstractDungeon.player.potions.get((int)0).hb);
                    }
                } else if ((CInputActionSet.cancel.isJustPressed() || CInputActionSet.pageLeftViewDeck.isJustPressed() && !CardCrawlGame.isPopupOpen) && this.selectPotionMode) {
                    this.selectPotionMode = false;
                    Gdx.input.setCursorPosition(0, 0);
                    CInputActionSet.cancel.unpress();
                }
                if (this.selectPotionMode && this.potionUi.isHidden && !this.potionUi.targetMode) {
                    this.updateControllerInput();
                } else if (AbstractDungeon.player.viewingRelics) {
                    AbstractDungeon.player.updateViewRelicControls();
                }
            }
        }
        if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.NO_INTERACT && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.DOOR_UNLOCK) {
            if (ModHelper.enabledMods.size() > 0) {
                for (Hitbox hb : this.modHbs) {
                    hb.update();
                }
            }
            this.hpHb.update();
            this.goldHb.update();
            this.updateAscensionHover();
            this.updateButtons();
            this.twitch.ifPresent(TwitchPanel::update);
            if (AbstractDungeon.screen != null && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.UNLOCK) {
                this.updateTips();
            }
        }
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT && !AbstractDungeon.isScreenUp && AbstractDungeon.player.currentHealth < AbstractDungeon.player.maxHealth / 4) {
            boolean hasHpEffect = false;
            for (AbstractGameEffect e : AbstractDungeon.topLevelEffects) {
                if (!(e instanceof PingHpEffect)) continue;
                hasHpEffect = true;
                break;
            }
            if (!hasHpEffect) {
                AbstractDungeon.topLevelEffectsQueue.add(new PingHpEffect(this.hpIconX));
            }
        }
    }

    private void updateControllerInput() {
        ArrayList<AbstractPotion> pots = AbstractDungeon.player.potions;
        this.section = TopSection.NONE;
        int index = 0;
        for (AbstractPotion p : AbstractDungeon.player.potions) {
            if (p.hb.hovered) {
                this.section = TopSection.POTIONS;
                break;
            }
            ++index;
        }
        if (this.section == TopSection.NONE) {
            index = 0;
            if (ModHelper.enabledMods.size() > 0) {
                for (Hitbox hb : this.modHbs) {
                    if (hb.hovered) {
                        this.section = TopSection.MODS;
                        break;
                    }
                    ++index;
                }
            }
        }
        if (this.section == TopSection.NONE && this.ascensionHb != null && this.ascensionHb.hovered) {
            this.section = TopSection.ASCENSION;
        }
        if (this.section == TopSection.NONE && this.goldHb.hovered) {
            this.section = TopSection.GOLD;
        }
        if (this.section == TopSection.NONE && this.hpHb.hovered) {
            this.section = TopSection.HP;
        }
        switch (this.section) {
            case HP: {
                if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
                    CInputHelper.setCursor(this.goldHb);
                    break;
                }
                if (!CInputActionSet.down.isJustPressed() && !CInputActionSet.altDown.isJustPressed()) break;
                this.controllerViewRelics();
                break;
            }
            case GOLD: {
                if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                    CInputHelper.setCursor(this.hpHb);
                    break;
                }
                if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
                    CInputHelper.setCursor(pots.get((int)0).hb);
                    break;
                }
                if (!CInputActionSet.down.isJustPressed() && !CInputActionSet.altDown.isJustPressed()) break;
                this.controllerViewRelics();
                break;
            }
            case POTIONS: {
                if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
                    if (++index > pots.size() - 1) {
                        if (AbstractDungeon.isAscensionMode) {
                            CInputHelper.setCursor(this.ascensionHb);
                            break;
                        }
                        if (this.modHbs.length == 0) break;
                        CInputHelper.setCursor(this.modHbs[0]);
                        break;
                    }
                    CInputHelper.setCursor(pots.get((int)index).hb);
                    break;
                }
                if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                    if (--index < 0) {
                        CInputHelper.setCursor(this.goldHb);
                        break;
                    }
                    CInputHelper.setCursor(pots.get((int)index).hb);
                    break;
                }
                if (!CInputActionSet.down.isJustPressed() && !CInputActionSet.altDown.isJustPressed()) break;
                this.controllerViewRelics();
                break;
            }
            case ASCENSION: {
                if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                    CInputHelper.setCursor(pots.get((int)(pots.size() - 1)).hb);
                    break;
                }
                if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
                    if (this.modHbs.length == 0) break;
                    CInputHelper.setCursor(this.modHbs[0]);
                    break;
                }
                if (!CInputActionSet.down.isJustPressed() && !CInputActionSet.altDown.isJustPressed()) break;
                this.controllerViewRelics();
                break;
            }
            case MODS: {
                if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                    if (--index < 0) {
                        if (AbstractDungeon.isAscensionMode) {
                            CInputHelper.setCursor(this.ascensionHb);
                            break;
                        }
                        CInputHelper.setCursor(pots.get((int)(pots.size() - 1)).hb);
                        break;
                    }
                    CInputHelper.setCursor(this.modHbs[index]);
                    break;
                }
                if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
                    if (++index > this.modHbs.length - 1) {
                        index = this.modHbs.length - 1;
                    }
                    CInputHelper.setCursor(this.modHbs[index]);
                    break;
                }
                if (!CInputActionSet.down.isJustPressed() && !CInputActionSet.altDown.isJustPressed()) break;
                this.controllerViewRelics();
                break;
            }
        }
    }

    private void controllerViewRelics() {
        AbstractDungeon.player.viewingRelics = true;
        this.selectPotionMode = false;
        CInputHelper.setCursor(AbstractDungeon.player.relics.get((int)(AbstractRelic.relicPage * AbstractRelic.MAX_RELICS_PER_PAGE)).hb);
    }

    private void updateAscensionHover() {
        this.ascensionHb.update();
        if (this.ascensionHb.hovered && AbstractDungeon.isAscensionMode) {
            TipHelper.renderGenericTip((float)InputHelper.mX + 50.0f * Settings.scale, TIP_Y, CharacterSelectScreen.TEXT[8], this.ascensionString);
        }
    }

    private void updateGold() {
        if (AbstractDungeon.player.gold < AbstractDungeon.player.displayGold) {
            AbstractDungeon.player.displayGold = AbstractDungeon.player.displayGold - AbstractDungeon.player.gold > 99 ? (AbstractDungeon.player.displayGold -= 10) : (AbstractDungeon.player.displayGold - AbstractDungeon.player.gold > 9 ? (AbstractDungeon.player.displayGold -= 3) : --AbstractDungeon.player.displayGold);
        } else if (AbstractDungeon.player.gold > AbstractDungeon.player.displayGold) {
            AbstractDungeon.player.displayGold = AbstractDungeon.player.gold - AbstractDungeon.player.displayGold > 99 ? (AbstractDungeon.player.displayGold += 10) : (AbstractDungeon.player.gold - AbstractDungeon.player.displayGold > 9 ? (AbstractDungeon.player.displayGold += 3) : ++AbstractDungeon.player.displayGold);
        }
    }

    public void flashRed() {
        this.flashRedTimer = 1.0f;
    }

    private void updatePotions() {
        if (this.flashRedTimer != 0.0f) {
            this.flashRedTimer -= Gdx.graphics.getDeltaTime();
            if (this.flashRedTimer < 0.0f) {
                this.flashRedTimer = 0.0f;
            }
        }
        for (AbstractPotion p : AbstractDungeon.player.potions) {
            p.hb.update();
            if (!p.isObtained) continue;
            if (p instanceof PotionSlot) {
                if (p.hb.hovered) {
                    p.scale = Settings.scale * 1.3f;
                    continue;
                }
                p.scale = Settings.scale;
                continue;
            }
            if (p.hb.justHovered) {
                if (MathUtils.randomBoolean()) {
                    CardCrawlGame.sound.play("POTION_1", 0.1f);
                } else {
                    CardCrawlGame.sound.play("POTION_3", 0.1f);
                }
            }
            if (p.hb.hovered) {
                p.scale = Settings.scale * 1.4f;
                if ((AbstractDungeon.player.hoveredCard != null || !InputHelper.justClickedLeft) && !CInputActionSet.select.isJustPressed()) continue;
                CInputActionSet.select.unpress();
                InputHelper.justClickedLeft = false;
                this.potionUi.open(p.slot, p);
                continue;
            }
            p.scale = MathHelper.scaleLerpSnap(p.scale, Settings.scale);
        }
    }

    private void updateRelics() {
        if (AbstractDungeon.player.relics.size() >= AbstractRelic.MAX_RELICS_PER_PAGE + 1 && AbstractRelic.relicPage > 0) {
            this.leftScrollHb.update();
            if (this.leftScrollHb.justHovered) {
                CardCrawlGame.sound.playV("UI_HOVER", 0.75f);
            }
            if (this.leftScrollHb.hovered && InputHelper.justClickedLeft) {
                this.leftScrollHb.clickStarted = true;
                CardCrawlGame.sound.playA("UI_CLICK_1", -0.1f);
            }
            if (this.leftScrollHb.clicked) {
                this.leftScrollHb.clicked = false;
                CardCrawlGame.sound.playA("DECK_OPEN", -0.1f);
                if (AbstractRelic.relicPage > 0) {
                    --AbstractRelic.relicPage;
                    this.adjustRelicHbs();
                }
            }
        }
        if (AbstractDungeon.player.relics.size() - (AbstractRelic.relicPage + 1) * AbstractRelic.MAX_RELICS_PER_PAGE > 0) {
            this.rightScrollHb.update();
            if (this.rightScrollHb.justHovered) {
                CardCrawlGame.sound.playV("UI_HOVER", 0.75f);
            }
            if (this.rightScrollHb.hovered && InputHelper.justClickedLeft) {
                this.rightScrollHb.clickStarted = true;
                CardCrawlGame.sound.playA("UI_CLICK_1", -0.1f);
            }
            if (this.rightScrollHb.clicked) {
                this.rightScrollHb.clicked = false;
                CardCrawlGame.sound.playA("DECK_OPEN", -0.1f);
                if (AbstractRelic.relicPage < AbstractDungeon.player.relics.size() / AbstractRelic.MAX_RELICS_PER_PAGE) {
                    ++AbstractRelic.relicPage;
                    this.adjustRelicHbs();
                }
            }
        }
    }

    public void adjustRelicHbs() {
        if (AbstractDungeon.player == null) {
            return;
        }
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            if (AbstractDungeon.player.relics.size() >= AbstractRelic.MAX_RELICS_PER_PAGE + 1) {
                r.hb.move(r.currentX - (float)(AbstractRelic.relicPage * Settings.WIDTH) + (float)AbstractRelic.relicPage * (AbstractRelic.PAD_X + 36.0f * Settings.scale) + 32.0f * Settings.scale, r.currentY);
                continue;
            }
            r.hb.move(r.currentX - (float)(AbstractRelic.relicPage * Settings.WIDTH) + (float)AbstractRelic.relicPage * (AbstractRelic.PAD_X + 36.0f * Settings.scale), r.currentY);
        }
    }

    public void destroyPotion(int slot) {
        AbstractDungeon.player.potions.set(slot, new PotionSlot(slot));
    }

    private void updateButtons() {
        this.updateSettingsButtonLogic();
        this.updateDeckViewButtonLogic();
        this.updateMapButtonLogic();
        if (this.settingsHb.justHovered || this.deckHb.justHovered || this.mapHb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        }
    }

    private void updateSettingsButtonLogic() {
        this.settingsButtonDisabled = false;
        if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.FTUE) {
            this.settingsHb.update();
        }
        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SETTINGS || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.INPUT_SETTINGS) {
            this.settingsAngle += Gdx.graphics.getDeltaTime() * 300.0f;
            if (this.settingsAngle > 360.0f) {
                this.settingsAngle -= 360.0f;
            }
        } else {
            this.settingsAngle = this.settingsHb.hovered ? MathHelper.angleLerpSnap(this.settingsAngle, -90.0f) : MathHelper.angleLerpSnap(this.settingsAngle, 0.0f);
        }
        if (this.settingsHb.hovered && InputHelper.justClickedLeft || InputHelper.pressedEscape || CInputActionSet.settings.isJustPressed()) {
            if (AbstractDungeon.gridSelectScreen.isJustForConfirming) {
                AbstractDungeon.dynamicBanner.hide();
            }
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DOOR_UNLOCK) {
                InputHelper.pressedEscape = false;
                CInputActionSet.settings.unpress();
                InputHelper.justClickedLeft = false;
                return;
            }
            if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.CARD_REWARD && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.GRID && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.SHOP && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MASTER_DECK_VIEW && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.SETTINGS && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MAP) {
                InputHelper.pressedEscape = false;
            }
            CInputActionSet.settings.unpress();
            if (AbstractDungeon.isScreenUp && (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SETTINGS || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.INPUT_SETTINGS)) {
                CardCrawlGame.sound.play("UI_CLICK_2");
                AbstractDungeon.screenSwap = false;
                if (!(AbstractDungeon.previousScreen != null && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.INPUT_SETTINGS || AbstractDungeon.previousScreen != AbstractDungeon.CurrentScreen.SETTINGS && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.INPUT_SETTINGS)) {
                    AbstractDungeon.previousScreen = null;
                }
                AbstractDungeon.closeCurrentScreen();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.COMBAT_REWARD) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.COMBAT_REWARD;
                AbstractDungeon.dynamicBanner.hide();
                AbstractDungeon.settingsScreen.open();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DEATH) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.DEATH;
                AbstractDungeon.settingsScreen.open();
                AbstractDungeon.deathScreen.hide();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.VICTORY) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.VICTORY;
                AbstractDungeon.settingsScreen.open();
                AbstractDungeon.victoryScreen.hide();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.BOSS_REWARD;
                AbstractDungeon.settingsScreen.open();
                AbstractDungeon.bossRelicScreen.hide();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SHOP) {
                if (!InputHelper.pressedEscape) {
                    AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.SHOP;
                    AbstractDungeon.settingsScreen.open();
                } else {
                    AbstractDungeon.closeCurrentScreen();
                }
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP && !AbstractDungeon.dungeonMapScreen.dismissable) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.MAP;
                AbstractDungeon.settingsScreen.open();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP) {
                if (!InputHelper.pressedEscape) {
                    if (AbstractDungeon.previousScreen == null) {
                        AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.MAP;
                    } else {
                        AbstractDungeon.screenSwap = true;
                    }
                    AbstractDungeon.settingsScreen.open();
                } else {
                    AbstractDungeon.closeCurrentScreen();
                }
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MASTER_DECK_VIEW) {
                if (!InputHelper.pressedEscape) {
                    if (AbstractDungeon.previousScreen == null) {
                        AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.MASTER_DECK_VIEW;
                    } else {
                        AbstractDungeon.screenSwap = true;
                    }
                    AbstractDungeon.settingsScreen.open();
                } else {
                    AbstractDungeon.closeCurrentScreen();
                }
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.CARD_REWARD;
                AbstractDungeon.dynamicBanner.hide();
                AbstractDungeon.settingsScreen.open();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.GRID) {
                if (!InputHelper.pressedEscape) {
                    AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.GRID;
                    AbstractDungeon.gridSelectScreen.hide();
                    AbstractDungeon.settingsScreen.open();
                }
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NEOW_UNLOCK) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.NEOW_UNLOCK;
                AbstractDungeon.dynamicBanner.hide();
                AbstractDungeon.settingsScreen.open();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.HAND_SELECT) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.HAND_SELECT;
                AbstractDungeon.settingsScreen.open();
            } else {
                AbstractDungeon.settingsScreen.open();
            }
            InputHelper.justClickedLeft = false;
        }
    }

    private void updateDeckViewButtonLogic() {
        boolean clickedDeckButton;
        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MASTER_DECK_VIEW) {
            this.rotateTimer += Gdx.graphics.getDeltaTime() * 4.0f;
            this.deckAngle = MathHelper.angleLerpSnap(this.deckAngle, MathUtils.sin(this.rotateTimer) * 15.0f);
        } else {
            this.deckAngle = this.deckHb.hovered ? MathHelper.angleLerpSnap(this.deckAngle, 15.0f) : MathHelper.angleLerpSnap(this.deckAngle, 0.0f);
        }
        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NONE || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.GRID || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.COMBAT_REWARD || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MASTER_DECK_VIEW || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DEATH || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.VICTORY || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SHOP || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SETTINGS || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.INPUT_SETTINGS || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.HAND_SELECT || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD) {
            this.deckButtonDisabled = false;
            this.deckHb.update();
        } else {
            this.deckButtonDisabled = true;
            this.deckHb.hovered = false;
        }
        boolean bl = clickedDeckButton = this.deckHb.hovered && InputHelper.justClickedLeft;
        if ((clickedDeckButton || InputActionSet.masterDeck.isJustPressed() || CInputActionSet.pageLeftViewDeck.isJustPressed()) && !CardCrawlGame.isPopupOpen) {
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.COMBAT_REWARD) {
                AbstractDungeon.closeCurrentScreen();
                AbstractDungeon.deckViewScreen.open();
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.COMBAT_REWARD;
            } else if (!AbstractDungeon.isScreenUp) {
                AbstractDungeon.deckViewScreen.open();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MASTER_DECK_VIEW) {
                AbstractDungeon.screenSwap = false;
                if (AbstractDungeon.previousScreen == AbstractDungeon.CurrentScreen.MASTER_DECK_VIEW) {
                    AbstractDungeon.previousScreen = null;
                }
                AbstractDungeon.closeCurrentScreen();
                CardCrawlGame.sound.play("DECK_CLOSE", 0.05f);
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DEATH) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.DEATH;
                AbstractDungeon.deathScreen.hide();
                AbstractDungeon.deckViewScreen.open();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.BOSS_REWARD;
                AbstractDungeon.bossRelicScreen.hide();
                AbstractDungeon.deckViewScreen.open();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SHOP) {
                AbstractDungeon.overlayMenu.cancelButton.hide();
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.SHOP;
                AbstractDungeon.deckViewScreen.open();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP && !AbstractDungeon.dungeonMapScreen.dismissable) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.MAP;
                AbstractDungeon.deckViewScreen.open();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SETTINGS || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP) {
                if (AbstractDungeon.previousScreen != null) {
                    AbstractDungeon.screenSwap = true;
                }
                AbstractDungeon.closeCurrentScreen();
                AbstractDungeon.deckViewScreen.open();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.INPUT_SETTINGS && clickedDeckButton) {
                if (AbstractDungeon.previousScreen != null) {
                    AbstractDungeon.screenSwap = true;
                }
                AbstractDungeon.closeCurrentScreen();
                AbstractDungeon.deckViewScreen.open();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.CARD_REWARD;
                AbstractDungeon.dynamicBanner.hide();
                AbstractDungeon.deckViewScreen.open();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.GRID) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.GRID;
                AbstractDungeon.gridSelectScreen.hide();
                AbstractDungeon.deckViewScreen.open();
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.HAND_SELECT) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.HAND_SELECT;
                AbstractDungeon.deckViewScreen.open();
            }
            InputHelper.justClickedLeft = false;
        }
    }

    private void updateMapButtonLogic() {
        boolean clickedMapButton;
        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP) {
            this.rotateTimer += Gdx.graphics.getDeltaTime() * 4.0f;
            this.mapAngle = MathHelper.angleLerpSnap(this.mapAngle, MathUtils.sin(this.rotateTimer) * 15.0f);
        } else {
            this.mapAngle = this.mapHb.hovered ? MathHelper.angleLerpSnap(this.mapAngle, 10.0f) : MathHelper.angleLerpSnap(this.mapAngle, -5.0f);
        }
        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NONE || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.GRID || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SETTINGS || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.INPUT_SETTINGS || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DEATH || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.VICTORY || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MASTER_DECK_VIEW || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.COMBAT_REWARD || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SHOP || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.HAND_SELECT || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) {
            this.mapButtonDisabled = false;
            this.mapHb.update();
        } else {
            this.mapButtonDisabled = true;
            this.mapHb.hovered = false;
        }
        boolean bl = clickedMapButton = this.mapHb.hovered && InputHelper.justClickedLeft;
        if (!CardCrawlGame.cardPopup.isOpen && (clickedMapButton || InputActionSet.map.isJustPressed() || CInputActionSet.map.isJustPressed())) {
            for (AbstractGameEffect e : AbstractDungeon.topLevelEffects) {
                if (!(e instanceof FadeWipeParticle)) continue;
                return;
            }
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP && !AbstractDungeon.dungeonMapScreen.dismissable) {
                CardCrawlGame.sound.play("CARD_REJECT");
            } else if (!AbstractDungeon.isScreenUp) {
                AbstractDungeon.dungeonMapScreen.open(false);
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.COMBAT_REWARD) {
                AbstractDungeon.closeCurrentScreen();
                AbstractDungeon.dungeonMapScreen.open(false);
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.COMBAT_REWARD;
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.BOSS_REWARD;
                AbstractDungeon.bossRelicScreen.hide();
                AbstractDungeon.dungeonMapScreen.open(false);
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SHOP) {
                AbstractDungeon.overlayMenu.cancelButton.hide();
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.SHOP;
                AbstractDungeon.dungeonMapScreen.open(false);
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP) {
                AbstractDungeon.screenSwap = false;
                if (AbstractDungeon.previousScreen == AbstractDungeon.CurrentScreen.MAP) {
                    AbstractDungeon.previousScreen = null;
                }
                AbstractDungeon.closeCurrentScreen();
                CardCrawlGame.sound.play("MAP_CLOSE", 0.05f);
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DEATH) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.DEATH;
                AbstractDungeon.deathScreen.hide();
                AbstractDungeon.dungeonMapScreen.open(false);
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SETTINGS || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MASTER_DECK_VIEW) {
                if (AbstractDungeon.dungeonMapScreen.dismissable) {
                    if (AbstractDungeon.previousScreen != null) {
                        AbstractDungeon.screenSwap = true;
                    }
                    AbstractDungeon.closeCurrentScreen();
                    AbstractDungeon.dungeonMapScreen.open(false);
                } else {
                    AbstractDungeon.closeCurrentScreen();
                }
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.INPUT_SETTINGS && clickedMapButton) {
                if (AbstractDungeon.dungeonMapScreen.dismissable) {
                    if (AbstractDungeon.previousScreen != null) {
                        AbstractDungeon.screenSwap = true;
                    }
                    AbstractDungeon.closeCurrentScreen();
                    AbstractDungeon.dungeonMapScreen.open(false);
                }
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.CARD_REWARD;
                AbstractDungeon.dynamicBanner.hide();
                AbstractDungeon.dungeonMapScreen.open(false);
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.GRID) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.GRID;
                AbstractDungeon.gridSelectScreen.hide();
                AbstractDungeon.dungeonMapScreen.open(false);
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.HAND_SELECT) {
                AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.HAND_SELECT;
                AbstractDungeon.dungeonMapScreen.open(false);
            }
            InputHelper.justClickedLeft = false;
        }
    }

    private void updateTips() {
        if (!Settings.hideTopBar) {
            if (this.hpHb.hovered) {
                TipHelper.renderGenericTip((float)InputHelper.mX - TIP_OFF_X, TIP_Y, LABEL[3], MSG[3]);
            } else if (this.goldHb.hovered) {
                TipHelper.renderGenericTip((float)InputHelper.mX - TIP_OFF_X, TIP_Y, LABEL[4], MSG[4]);
            } else {
                this.renderPotionTips();
                if (ModHelper.enabledMods.size() > 0) {
                    this.renderModTips();
                }
            }
        }
    }

    private void renderPotionTips() {
        if (!Settings.hideTopBar && this.potionUi.isHidden) {
            for (AbstractPotion p : AbstractDungeon.player.potions) {
                if (!p.hb.hovered) continue;
                TipHelper.queuePowerTips((float)InputHelper.mX - TIP_OFF_X, TIP_Y, p.tips);
            }
        }
    }

    private void renderModTips() {
        for (int i = 0; i < this.modHbs.length; ++i) {
            if (this.modHbs[i] == null || !this.modHbs[i].hovered || ModHelper.enabledMods.get(i) == null) continue;
            TipHelper.renderGenericTip((float)InputHelper.mX - TIP_OFF_X, TIP_Y, ModHelper.enabledMods.get((int)i).name, ModHelper.enabledMods.get((int)i).description);
        }
    }

    public void render(SpriteBatch sb) {
        if (!Settings.hideTopBar) {
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.TOP_PANEL_BAR, 0.0f, (float)Settings.HEIGHT - TOPBAR_H, (float)Settings.WIDTH, TOPBAR_H);
            if (CardCrawlGame.displayVersion) {
                FontHelper.renderFontRightTopAligned(sb, FontHelper.cardDescFont_N, CardCrawlGame.VERSION_NUM, (float)Settings.WIDTH - 16.0f * Settings.scale, (float)Settings.HEIGHT - TOPBAR_H + 48.0f * Settings.scale, Settings.QUARTER_TRANSPARENT_WHITE_COLOR);
                FontHelper.renderFontRightTopAligned(sb, FontHelper.cardDescFont_N, SeedHelper.getUserFacingSeedString(), (float)Settings.WIDTH - 16.0f * Settings.scale, (float)Settings.HEIGHT - TOPBAR_H + 24.0f * Settings.scale, Settings.QUARTER_TRANSPARENT_WHITE_COLOR);
            }
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.MAP || CardCrawlGame.stopClock) {
                this.timerHb.update();
                sb.draw(ImageMaster.TIMER_ICON, (float)Settings.WIDTH - 380.0f * Settings.scale, ICON_Y, ICON_W, ICON_W);
                if (CardCrawlGame.stopClock) {
                    FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipBodyFont, CharStat.formatHMSM(CardCrawlGame.playtime), (float)Settings.WIDTH - 320.0f * Settings.scale, INFO_TEXT_Y, Settings.GREEN_TEXT_COLOR);
                } else {
                    FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipBodyFont, CharStat.formatHMSM(CardCrawlGame.playtime), (float)Settings.WIDTH - 320.0f * Settings.scale, INFO_TEXT_Y, Settings.GOLD_COLOR);
                }
                if (this.timerHb.hovered) {
                    TipHelper.renderGenericTip(TOP_RIGHT_TIP_X, TIP_Y, LABEL[5], MSG[7]);
                }
                this.timerHb.render(sb);
            }
            this.renderName(sb);
            this.renderHP(sb);
            this.renderGold(sb);
            this.renderDungeonInfo(sb);
            if (ModHelper.enabledMods.size() > 0) {
                this.renderDailyMods(sb);
            }
            this.renderTopRightIcons(sb);
            this.renderRelics(sb);
            AbstractDungeon.player.renderBlights(sb);
            this.renderPotions(sb);
            if (Settings.isControllerMode) {
                this.renderControllerUi(sb);
            }
            this.potionUi.render(sb);
            this.twitch.ifPresent(twitchPanel -> twitchPanel.render(sb));
        }
    }

    private void renderControllerUi(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        if (this.selectPotionMode) {
            sb.setColor(new Color(1.0f, 0.9f, 0.4f, 0.6f + MathUtils.cosDeg(System.currentTimeMillis() / 2L % 360L) / 5.0f));
            float doop = (1.0f + (1.0f + MathUtils.cosDeg(System.currentTimeMillis() / 2L % 360L)) / 50.0f) * Settings.scale;
            switch (this.section) {
                case HP: {
                    sb.draw(potionSelectBox, this.hpHb.cX - 137.0f, (float)Settings.HEIGHT - 53.0f - 34.0f * Settings.scale, 137.0f, 53.0f, 274.0f, 106.0f, doop * 0.8f, doop, 0.0f, 0, 0, 274, 106, false, false);
                    break;
                }
                case GOLD: {
                    sb.draw(potionSelectBox, this.goldHb.cX - 137.0f, (float)Settings.HEIGHT - 53.0f - 34.0f * Settings.scale, 137.0f, 53.0f, 274.0f, 106.0f, doop * 0.7f, doop, 0.0f, 0, 0, 274, 106, false, false);
                    break;
                }
                case POTIONS: {
                    sb.draw(potionSelectBox, potionX - 137.0f + 72.0f * Settings.scale, (float)Settings.HEIGHT - 53.0f - 34.0f * Settings.scale, 137.0f, 53.0f, 100.0f + (float)AbstractDungeon.player.potionSlots * 76.0f * Settings.scale, 106.0f, doop, doop, 0.0f, 0, 0, 274, 106, false, false);
                    break;
                }
                case ASCENSION: {
                    sb.draw(potionSelectBox, this.ascensionHb.cX - 137.0f, (float)Settings.HEIGHT - 53.0f - 34.0f * Settings.scale, 137.0f, 53.0f, 274.0f, 106.0f, doop, doop, 0.0f, 0, 0, 274, 106, false, false);
                    break;
                }
                case MODS: {
                    sb.draw(potionSelectBox, this.modHbs[0].cX - 137.0f - 33.0f * Settings.scale, (float)Settings.HEIGHT - 53.0f - 34.0f * Settings.scale, 137.0f, 53.0f, 340.0f, 106.0f, doop, doop, 0.0f, 0, 0, 274, 106, false, false);
                    break;
                }
            }
            sb.setColor(Color.WHITE);
        } else {
            sb.draw(CInputActionSet.topPanel.getKeyImg(), (float)AbstractDungeon.player.potionSlots * Settings.POTION_W - 32.0f + potionX, (float)Settings.HEIGHT - 32.0f - 32.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 0.7f, Settings.scale * 0.7f, 0.0f, 0, 0, 64, 64, false, false);
        }
        float iconY = -52.0f * Settings.scale;
        sb.draw(CInputActionSet.map.getKeyImg(), (float)Settings.WIDTH - 32.0f - 204.0f * Settings.scale, (float)Settings.HEIGHT - 32.0f + iconY, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 0.6f, Settings.scale * 0.6f, 0.0f, 0, 0, 64, 64, false, false);
        sb.draw(CInputActionSet.pageLeftViewDeck.getKeyImg(), (float)Settings.WIDTH - 32.0f - 136.0f * Settings.scale, (float)Settings.HEIGHT - 32.0f + iconY, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 0.6f, Settings.scale * 0.6f, 0.0f, 0, 0, 64, 64, false, false);
        sb.draw(CInputActionSet.settings.getKeyImg(), (float)Settings.WIDTH - 32.0f - 68.0f * Settings.scale, (float)Settings.HEIGHT - 32.0f + iconY, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 0.6f, Settings.scale * 0.6f, 0.0f, 0, 0, 64, 64, false, false);
    }

    private void renderName(SpriteBatch sb) {
        if (Settings.isEndless) {
            sb.draw(ImageMaster.ENDLESS_ICON, -32.0f + 46.0f * Settings.scale, ICON_Y - 32.0f + 29.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        } else if (Settings.isFinalActAvailable) {
            sb.draw(ImageMaster.KEY_SLOTS_ICON, -32.0f + 46.0f * Settings.scale, ICON_Y - 32.0f + 29.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            if (Settings.hasRubyKey) {
                sb.draw(ImageMaster.RUBY_KEY, -32.0f + 46.0f * Settings.scale, ICON_Y - 32.0f + 29.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            }
            if (Settings.hasEmeraldKey) {
                sb.draw(ImageMaster.EMERALD_KEY, -32.0f + 46.0f * Settings.scale, ICON_Y - 32.0f + 29.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            }
            if (Settings.hasSapphireKey) {
                sb.draw(ImageMaster.SAPPHIRE_KEY, -32.0f + 46.0f * Settings.scale, ICON_Y - 32.0f + 29.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
            }
        }
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.panelNameFont, this.name, this.nameX, NAME_Y, Color.WHITE);
        if (Settings.isMobile) {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipBodyFont, this.title, this.nameX, this.titleY - 30.0f * Settings.scale, Color.LIGHT_GRAY);
        } else {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipBodyFont, this.title, this.titleX, this.titleY, Color.LIGHT_GRAY);
        }
    }

    private void renderHP(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        if (this.hpHb.hovered) {
            sb.draw(ImageMaster.TP_HP, this.hpIconX - 32.0f + 32.0f * Settings.scale, ICON_Y - 32.0f + 32.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 1.2f, Settings.scale * 1.2f, 0.0f, 0, 0, 64, 64, false, false);
        } else {
            sb.draw(ImageMaster.TP_HP, this.hpIconX - 32.0f + 32.0f * Settings.scale, ICON_Y - 32.0f + 32.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.topPanelInfoFont, AbstractDungeon.player.currentHealth + "/" + AbstractDungeon.player.maxHealth, this.hpIconX + HP_NUM_OFFSET_X, INFO_TEXT_Y, Color.SALMON);
        this.hpHb.render(sb);
    }

    private void renderPotions(SpriteBatch sb) {
        if (this.flashRedTimer != 0.0f) {
            sb.setColor(new Color(1.0f, 0.0f, 0.0f, this.flashRedTimer / 2.0f));
            sb.draw(ImageMaster.WHITE_SQUARE_IMG, potionX - 40.0f * Settings.scale, (float)Settings.HEIGHT - 64.0f * Settings.scale, (float)AbstractDungeon.player.potionSlots * 64.0f * Settings.scale, 64.0f * Settings.scale);
        }
        for (AbstractPotion p : AbstractDungeon.player.potions) {
            if (p.isObtained) {
                p.renderOutline(sb);
                p.render(sb);
                if (p.hb.hovered) {
                    p.renderShiny(sb);
                }
            }
            p.hb.render(sb);
        }
    }

    private void renderRelics(SpriteBatch sb) {
        AbstractDungeon.player.renderRelics(sb);
        sb.setColor(Color.WHITE);
        if (AbstractDungeon.player.relics.size() >= AbstractRelic.MAX_RELICS_PER_PAGE + 1 && AbstractRelic.relicPage > 0) {
            this.leftScrollHb.render(sb);
            sb.draw(ImageMaster.CF_LEFT_ARROW, this.leftScrollHb.cX - 24.0f, this.leftScrollHb.cY - 24.0f, 24.0f, 24.0f, 48.0f, 48.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 48, 48, false, false);
        }
        if (AbstractDungeon.player.relics.size() - (AbstractRelic.relicPage + 1) * AbstractRelic.MAX_RELICS_PER_PAGE > 0) {
            this.rightScrollHb.render(sb);
            sb.draw(ImageMaster.CF_RIGHT_ARROW, this.rightScrollHb.cX - 24.0f, this.rightScrollHb.cY - 24.0f, 24.0f, 24.0f, 48.0f, 48.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 48, 48, false, false);
        }
    }

    private void renderGold(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        if (this.goldHb.hovered) {
            sb.draw(ImageMaster.TP_GOLD, this.goldIconX - 32.0f + 32.0f * Settings.scale, ICON_Y - 32.0f + 32.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 1.2f, Settings.scale * 1.2f, 0.0f, 0, 0, 64, 64, false, false);
        } else {
            sb.draw(ImageMaster.TP_GOLD, this.goldIconX - 32.0f + 32.0f * Settings.scale, ICON_Y - 32.0f + 32.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        if (AbstractDungeon.player.displayGold == AbstractDungeon.player.gold) {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.topPanelInfoFont, Integer.toString(AbstractDungeon.player.displayGold), this.goldIconX + GOLD_NUM_OFFSET_X, INFO_TEXT_Y, Settings.GOLD_COLOR);
        } else if (AbstractDungeon.player.displayGold > AbstractDungeon.player.gold) {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.topPanelInfoFont, Integer.toString(AbstractDungeon.player.displayGold), this.goldIconX + GOLD_NUM_OFFSET_X, INFO_TEXT_Y, Settings.RED_TEXT_COLOR);
        } else {
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.topPanelInfoFont, Integer.toString(AbstractDungeon.player.displayGold), this.goldIconX + GOLD_NUM_OFFSET_X, INFO_TEXT_Y, Settings.GREEN_TEXT_COLOR);
        }
        this.goldHb.render(sb);
    }

    private void renderDungeonInfo(SpriteBatch sb) {
        if (AbstractDungeon.floorNum > 0) {
            sb.draw(ImageMaster.TP_FLOOR, floorX, ICON_Y, ICON_W, ICON_W);
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.topPanelInfoFont, Integer.toString(AbstractDungeon.floorNum), floorX + 60.0f * Settings.scale, INFO_TEXT_Y, Settings.CREAM_COLOR);
        }
        if (AbstractDungeon.isAscensionMode) {
            sb.draw(ImageMaster.TP_ASCENSION, floorX + 106.0f * Settings.scale, ICON_Y, ICON_W, ICON_W);
            if (AbstractDungeon.ascensionLevel == 20) {
                FontHelper.renderFontLeftTopAligned(sb, FontHelper.topPanelInfoFont, Integer.toString(AbstractDungeon.ascensionLevel), floorX + 166.0f * Settings.scale, INFO_TEXT_Y, Settings.GOLD_COLOR);
            } else {
                FontHelper.renderFontLeftTopAligned(sb, FontHelper.topPanelInfoFont, Integer.toString(AbstractDungeon.ascensionLevel), floorX + 166.0f * Settings.scale, INFO_TEXT_Y, Settings.RED_TEXT_COLOR);
            }
        }
        if (this.ascensionHb != null) {
            this.ascensionHb.render(sb);
        }
    }

    private void renderDailyMods(SpriteBatch sb) {
        if (ModHelper.enabledMods.size() > 0) {
            float offsetX = 0.0f;
            sb.setColor(Color.WHITE);
            for (int i = 0; i < ModHelper.enabledMods.size(); ++i) {
                if (ModHelper.enabledMods.get(i) == null) continue;
                if (this.modHbs[i].hovered) {
                    sb.draw(ModHelper.enabledMods.get((int)i).img, dailyModX - 32.0f + offsetX, ICON_Y - 32.0f + 32.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 1.3f, Settings.scale * 1.3f, 0.0f, 0, 0, 64, 64, false, false);
                } else {
                    sb.draw(ModHelper.enabledMods.get((int)i).img, dailyModX - 32.0f + offsetX, ICON_Y - 32.0f + 32.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
                }
                offsetX += 52.0f * Settings.scale;
            }
            FontHelper.renderFontRightTopAligned(sb, FontHelper.tipBodyFont, DailyScreen.TEXT[13], dailyModX - 30.0f * Settings.scale, INFO_TEXT_Y + 3.0f * Settings.scale, Settings.GOLD_COLOR);
            for (Hitbox hb : this.modHbs) {
                hb.render(sb);
            }
        }
    }

    public static String getOrdinalNaming(int i) {
        return i % 100 == 11 || i % 100 == 12 || i % 100 == 13 ? "th" : (new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"})[i % 10];
    }

    private void renderTopRightIcons(SpriteBatch sb) {
        if (this.settingsButtonDisabled) {
            sb.setColor(DISABLED_BTN_COLOR);
        } else if (this.settingsHb.hovered && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.SETTINGS && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.INPUT_SETTINGS) {
            TipHelper.renderGenericTip(TOP_RIGHT_TIP_X, TIP_Y, LABEL[0] + " (" + InputActionSet.cancel.getKeyString() + ")", MSG[0]);
        }
        this.renderSettingsIcon(sb);
        Color tmpColor = Color.WHITE.cpy();
        if (this.deckButtonDisabled) {
            sb.setColor(DISABLED_BTN_COLOR);
            tmpColor = DISABLED_BTN_COLOR;
        } else if (this.deckHb.hovered) {
            sb.setColor(Color.CYAN);
            if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MASTER_DECK_VIEW) {
                TipHelper.renderGenericTip(TOP_RIGHT_TIP_X, TIP_Y, LABEL[1] + " (" + InputActionSet.masterDeck.getKeyString() + ")", MSG[1]);
            }
        } else {
            sb.setColor(Color.WHITE);
        }
        this.renderDeckIcon(sb);
        FontHelper.renderFontRightTopAligned(sb, FontHelper.topPanelAmountFont, Integer.toString(AbstractDungeon.player.masterDeck.size()), DECK_X + 58.0f * Settings.scale, ICON_Y + 25.0f * Settings.scale, tmpColor);
        if (this.mapButtonDisabled) {
            sb.setColor(DISABLED_BTN_COLOR);
        } else if (this.mapHb.hovered) {
            sb.setColor(Color.CYAN);
            if (AbstractDungeon.screen != AbstractDungeon.CurrentScreen.MASTER_DECK_VIEW) {
                TipHelper.renderGenericTip(TOP_RIGHT_TIP_X, TIP_Y, LABEL[2] + " (" + InputActionSet.map.getKeyString() + ")", MSG[2]);
            }
        } else {
            sb.setColor(Color.WHITE);
        }
        this.renderMapIcon(sb);
    }

    private void renderSettingsIcon(SpriteBatch sb) {
        sb.draw(ImageMaster.SETTINGS_ICON, SETTINGS_X - 32.0f + 32.0f * Settings.scale, ICON_Y - 32.0f + 32.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, this.settingsAngle, 0, 0, 64, 64, false, false);
        if (this.settingsHb.hovered) {
            sb.setBlendFunction(770, 1);
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.25f));
            sb.draw(ImageMaster.SETTINGS_ICON, SETTINGS_X - 32.0f + 32.0f * Settings.scale, ICON_Y - 32.0f + 32.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, this.settingsAngle, 0, 0, 64, 64, false, false);
            sb.setBlendFunction(770, 771);
        }
        this.settingsHb.render(sb);
    }

    private void renderDeckIcon(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.DECK_ICON, DECK_X - 32.0f + 32.0f * Settings.scale, ICON_Y - 32.0f + 32.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, this.deckAngle, 0, 0, 64, 64, false, false);
        if (this.deckHb.hovered) {
            sb.setBlendFunction(770, 1);
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.25f));
            sb.draw(ImageMaster.DECK_ICON, DECK_X - 32.0f + 32.0f * Settings.scale, ICON_Y - 32.0f + 32.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, this.deckAngle, 0, 0, 64, 64, false, false);
            sb.setBlendFunction(770, 771);
        }
        this.deckHb.render(sb);
    }

    private void renderMapIcon(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.MAP_ICON, MAP_X - 32.0f + 32.0f * Settings.scale, ICON_Y - 32.0f + 32.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, this.mapAngle, 0, 0, 64, 64, false, false);
        if (this.mapHb.hovered) {
            sb.setBlendFunction(770, 1);
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.25f));
            sb.draw(ImageMaster.MAP_ICON, MAP_X - 32.0f + 32.0f * Settings.scale, ICON_Y - 32.0f + 32.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, this.mapAngle, 0, 0, 64, 64, false, false);
            sb.setBlendFunction(770, 771);
        }
        this.mapHb.render(sb);
    }

    public void panelHealEffect() {
        AbstractDungeon.topLevelEffectsQueue.add(new HealPanelEffect(this.hpIconX));
    }

    static {
        logger = LogManager.getLogger(TopPanel.class.getName());
    }

    private static enum TopSection {
        HP,
        GOLD,
        POTIONS,
        ASCENSION,
        MODS,
        NONE;

    }
}


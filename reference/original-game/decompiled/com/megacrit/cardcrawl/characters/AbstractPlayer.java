/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.characters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Bezier;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.GameActionManager;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.defect.AnimateOrbAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.actions.defect.EvokeOrbAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.blue.Strike_Blue;
import com.megacrit.cardcrawl.cards.blue.Zap;
import com.megacrit.cardcrawl.cards.green.Defend_Green;
import com.megacrit.cardcrawl.cards.green.Strike_Green;
import com.megacrit.cardcrawl.cards.green.Survivor;
import com.megacrit.cardcrawl.cards.purple.Defend_Watcher;
import com.megacrit.cardcrawl.cards.purple.Eruption;
import com.megacrit.cardcrawl.cards.red.Bash;
import com.megacrit.cardcrawl.cards.red.Defend_Red;
import com.megacrit.cardcrawl.cards.red.Strike_Red;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.EnergyManager;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.TipTracker;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.controller.CInputHelper;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.Dark;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import com.megacrit.cardcrawl.orbs.Plasma;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.PotionSlot;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.PlatedArmorPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.BottledFlame;
import com.megacrit.cardcrawl.relics.BottledLightning;
import com.megacrit.cardcrawl.relics.BottledTornado;
import com.megacrit.cardcrawl.relics.LizardTail;
import com.megacrit.cardcrawl.relics.SlaversCollar;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.CampfireUI;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import com.megacrit.cardcrawl.rooms.RestRoom;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import com.megacrit.cardcrawl.screens.CharSelectInfo;
import com.megacrit.cardcrawl.screens.DeathScreen;
import com.megacrit.cardcrawl.screens.charSelect.CharacterOption;
import com.megacrit.cardcrawl.screens.stats.CharStat;
import com.megacrit.cardcrawl.stances.AbstractStance;
import com.megacrit.cardcrawl.stances.NeutralStance;
import com.megacrit.cardcrawl.ui.FtueTip;
import com.megacrit.cardcrawl.ui.MultiPageFtue;
import com.megacrit.cardcrawl.ui.buttons.PeekButton;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.ThoughtBubble;
import com.megacrit.cardcrawl.vfx.cardManip.CardDisappearEffect;
import com.megacrit.cardcrawl.vfx.combat.BlockedWordEffect;
import com.megacrit.cardcrawl.vfx.combat.HbBlockBrokenEffect;
import com.megacrit.cardcrawl.vfx.combat.StrikeEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractPlayer
extends AbstractCreature {
    private static final Logger logger = LogManager.getLogger(AbstractPlayer.class.getName());
    private static final TutorialStrings tutorialStrings = CardCrawlGame.languagePack.getTutorialString("Player Tips");
    public static final String[] MSG = AbstractPlayer.tutorialStrings.TEXT;
    public static final String[] LABEL = AbstractPlayer.tutorialStrings.LABEL;
    public PlayerClass chosenClass;
    public int gameHandSize;
    public int masterHandSize;
    public int startingMaxHP;
    public CardGroup masterDeck = new CardGroup(CardGroup.CardGroupType.MASTER_DECK);
    public CardGroup drawPile = new CardGroup(CardGroup.CardGroupType.DRAW_PILE);
    public CardGroup hand = new CardGroup(CardGroup.CardGroupType.HAND);
    public CardGroup discardPile = new CardGroup(CardGroup.CardGroupType.DISCARD_PILE);
    public CardGroup exhaustPile = new CardGroup(CardGroup.CardGroupType.EXHAUST_PILE);
    public CardGroup limbo = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
    public ArrayList<AbstractRelic> relics = new ArrayList();
    public ArrayList<AbstractBlight> blights = new ArrayList();
    public int potionSlots = 3;
    public ArrayList<AbstractPotion> potions = new ArrayList();
    public EnergyManager energy;
    public boolean isEndingTurn = false;
    public boolean viewingRelics = false;
    public boolean inspectMode = false;
    public Hitbox inspectHb = null;
    public static int poisonKillCount = 0;
    public int damagedThisCombat = 0;
    public String title;
    public ArrayList<AbstractOrb> orbs = new ArrayList();
    public int masterMaxOrbs;
    public int maxOrbs;
    public AbstractStance stance = new NeutralStance();
    @Deprecated
    public int cardsPlayedThisTurn = 0;
    private boolean isHoveringCard = false;
    public boolean isHoveringDropZone = false;
    private float hoverStartLine = 0.0f;
    private boolean passedHesitationLine = false;
    public AbstractCard hoveredCard = null;
    public AbstractCard toHover = null;
    public AbstractCard cardInUse = null;
    public boolean isDraggingCard = false;
    private boolean isUsingClickDragControl = false;
    private float clickDragTimer = 0.0f;
    public boolean inSingleTargetMode = false;
    private AbstractMonster hoveredMonster = null;
    public float hoverEnemyWaitTimer = 0.0f;
    private static final float HOVER_ENEMY_WAIT_TIME = 1.0f;
    public boolean isInKeyboardMode = false;
    private boolean skipMouseModeOnce = false;
    private int keyboardCardIndex = -1;
    public static ArrayList<String> customMods = null;
    private int touchscreenInspectCount = 0;
    public Texture img;
    public Texture shoulderImg;
    public Texture shoulder2Img;
    public Texture corpseImg;
    private static final Color ARROW_COLOR = new Color(1.0f, 0.2f, 0.3f, 1.0f);
    private float arrowScale;
    private float arrowScaleTimer = 0.0f;
    private float arrowX;
    private float arrowY;
    private static final float ARROW_TARGET_SCALE = 1.2f;
    private static final int TARGET_ARROW_W = 256;
    public static final float HOVER_CARD_Y_POSITION = 210.0f * Settings.scale;
    public boolean endTurnQueued = false;
    private static final int SEGMENTS = 20;
    private Vector2[] points = new Vector2[20];
    private Vector2 controlPoint = new Vector2();
    private Vector2 arrowTmp = new Vector2();
    private Vector2 startArrowVector = new Vector2();
    private Vector2 endArrowVector = new Vector2();
    private boolean renderCorpse = false;
    public static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("AbstractPlayer");

    public AbstractPlayer(String name, PlayerClass setClass) {
        int i;
        this.name = name;
        this.title = this.getTitle(setClass);
        this.drawX = (float)Settings.WIDTH * 0.25f;
        this.drawY = AbstractDungeon.floorY;
        this.chosenClass = setClass;
        this.isPlayer = true;
        this.initializeStarterRelics(setClass);
        this.loadPrefs();
        if (AbstractDungeon.ascensionLevel >= 11) {
            --this.potionSlots;
        }
        this.potions.clear();
        for (i = 0; i < this.potionSlots; ++i) {
            this.potions.add(new PotionSlot(i));
        }
        for (i = 0; i < this.points.length; ++i) {
            this.points[i] = new Vector2();
        }
    }

    public abstract String getPortraitImageName();

    public abstract ArrayList<String> getStartingDeck();

    public abstract ArrayList<String> getStartingRelics();

    public abstract CharSelectInfo getLoadout();

    public abstract String getTitle(PlayerClass var1);

    public abstract AbstractCard.CardColor getCardColor();

    public abstract Color getCardRenderColor();

    public abstract String getAchievementKey();

    public abstract ArrayList<AbstractCard> getCardPool(ArrayList<AbstractCard> var1);

    public abstract AbstractCard getStartCardForEvent();

    public abstract Color getCardTrailColor();

    public abstract String getLeaderboardCharacterName();

    public abstract Texture getEnergyImage();

    public abstract int getAscensionMaxHPLoss();

    public abstract BitmapFont getEnergyNumFont();

    public abstract void renderOrb(SpriteBatch var1, boolean var2, float var3, float var4);

    public abstract void updateOrb(int var1);

    public abstract Prefs getPrefs();

    public abstract void loadPrefs();

    public abstract CharStat getCharStat();

    public abstract int getUnlockedCardCount();

    public abstract int getSeenCardCount();

    public abstract int getCardCount();

    public abstract boolean saveFileExists();

    public abstract String getWinStreakKey();

    public abstract String getLeaderboardWinStreakKey();

    public abstract void renderStatScreen(SpriteBatch var1, float var2, float var3);

    public abstract void doCharSelectScreenSelectEffect();

    public abstract String getCustomModeCharacterButtonSoundKey();

    public abstract Texture getCustomModeCharacterButtonImage();

    public abstract CharacterStrings getCharacterString();

    public abstract String getLocalizedCharacterName();

    public abstract void refreshCharStat();

    public abstract AbstractPlayer newInstance();

    public abstract TextureAtlas.AtlasRegion getOrb();

    public abstract String getSpireHeartText();

    public abstract Color getSlashAttackColor();

    public abstract AbstractGameAction.AttackEffect[] getSpireHeartSlashEffect();

    public abstract String getVampireText();

    public String getSaveFilePath() {
        return SaveAndContinue.getPlayerSavePath(this.chosenClass);
    }

    public void dispose() {
        if (this.atlas != null) {
            this.atlas.dispose();
        }
        if (this.img != null) {
            this.img.dispose();
        }
        if (this.shoulderImg != null) {
            this.shoulderImg.dispose();
        }
        if (this.shoulder2Img != null) {
            this.shoulder2Img.dispose();
        }
        if (this.corpseImg != null) {
            this.corpseImg.dispose();
        }
    }

    public void adjustPotionPositions() {
        for (int i = 0; i < this.potions.size() - 1; ++i) {
            this.potions.get(i).adjustPosition(i);
        }
    }

    protected void initializeClass(String imgUrl, String shoulder2ImgUrl, String shouldImgUrl, String corpseImgUrl, CharSelectInfo info, float hb_x, float hb_y, float hb_w, float hb_h, EnergyManager energy) {
        if (imgUrl != null) {
            this.img = ImageMaster.loadImage(imgUrl);
        }
        if (this.img != null) {
            this.atlas = null;
        }
        this.shoulderImg = ImageMaster.loadImage(shouldImgUrl);
        this.shoulder2Img = ImageMaster.loadImage(shoulder2ImgUrl);
        this.corpseImg = ImageMaster.loadImage(corpseImgUrl);
        if (Settings.isMobile) {
            hb_w *= 1.17f;
        }
        this.startingMaxHP = this.maxHealth = info.maxHp;
        this.currentHealth = info.currentHp;
        this.masterMaxOrbs = info.maxOrbs;
        this.energy = energy;
        this.gameHandSize = this.masterHandSize = info.cardDraw;
        this.displayGold = this.gold = info.gold;
        this.hb_h = hb_h * Settings.xScale;
        this.hb_w = hb_w * Settings.scale;
        this.hb_x = hb_x * Settings.scale;
        this.hb_y = hb_y * Settings.scale;
        this.hb = new Hitbox(this.hb_w, this.hb_h);
        this.healthHb = new Hitbox(this.hb.width, 72.0f * Settings.scale);
        this.refreshHitboxLocation();
    }

    public void initializeStarterDeck() {
        ArrayList<String> cards = this.getStartingDeck();
        boolean addBaseCards = true;
        if (ModHelper.isModEnabled("Draft") || ModHelper.isModEnabled("Chimera") || ModHelper.isModEnabled("SealedDeck") || ModHelper.isModEnabled("Shiny") || ModHelper.isModEnabled("Insanity")) {
            addBaseCards = false;
        }
        if (ModHelper.isModEnabled("Chimera")) {
            this.masterDeck.addToTop(new Bash());
            this.masterDeck.addToTop(new Survivor());
            this.masterDeck.addToTop(new Zap());
            this.masterDeck.addToTop(new Eruption());
            this.masterDeck.addToTop(new Strike_Red());
            this.masterDeck.addToTop(new Strike_Green());
            this.masterDeck.addToTop(new Strike_Blue());
            this.masterDeck.addToTop(new Defend_Red());
            this.masterDeck.addToTop(new Defend_Green());
            this.masterDeck.addToTop(new Defend_Watcher());
        }
        if (ModHelper.isModEnabled("Insanity")) {
            for (int i = 0; i < 50; ++i) {
                this.masterDeck.addToTop(AbstractDungeon.returnRandomCard().makeCopy());
            }
        }
        if (ModHelper.isModEnabled("Shiny")) {
            CardGroup group = AbstractDungeon.getEachRare();
            for (AbstractCard c : group.group) {
                this.masterDeck.addToTop(c);
            }
        }
        if (addBaseCards) {
            for (String s : cards) {
                this.masterDeck.addToTop(CardLibrary.getCard(this.chosenClass, s).makeCopy());
            }
        }
        for (AbstractCard c : this.masterDeck.group) {
            UnlockTracker.markCardAsSeen(c.cardID);
        }
    }

    protected void initializeStarterRelics(PlayerClass chosenClass) {
        ArrayList<String> relics = this.getStartingRelics();
        if (ModHelper.isModEnabled("Cursed Run")) {
            relics.clear();
            relics.add("Cursed Key");
            relics.add("Darkstone Periapt");
            relics.add("Du-Vu Doll");
        }
        if (ModHelper.isModEnabled("ControlledChaos")) {
            relics.add("Frozen Eye");
        }
        int index = 0;
        for (String s : relics) {
            RelicLibrary.getRelic(s).makeCopy().instantObtain(this, index, false);
            ++index;
        }
        AbstractDungeon.relicsToRemoveOnStart.addAll(relics);
    }

    public void combatUpdate() {
        if (this.cardInUse != null) {
            this.cardInUse.update();
        }
        this.limbo.update();
        this.exhaustPile.update();
        for (AbstractPower p : this.powers) {
            p.updateParticles();
        }
        for (AbstractOrb o : this.orbs) {
            o.update();
        }
        this.stance.update();
    }

    public void update() {
        this.updateControllerInput();
        this.hb.update();
        this.updateHealthBar();
        this.updatePowers();
        this.healthHb.update();
        this.updateReticle();
        this.tint.update();
        if (AbstractDungeon.getCurrRoom().phase != AbstractRoom.RoomPhase.EVENT) {
            for (AbstractOrb o : this.orbs) {
                o.updateAnimation();
            }
        }
        this.updateEscapeAnimation();
    }

    /*
     * WARNING - void declaration
     */
    private void updateControllerInput() {
        ArrayList<Hitbox> hbs;
        if (!Settings.isControllerMode || AbstractDungeon.topPanel.selectPotionMode || !AbstractDungeon.topPanel.potionUi.isHidden) {
            return;
        }
        boolean anyHovered = false;
        boolean orbHovered = false;
        int orbIndex = 0;
        for (AbstractOrb abstractOrb : this.orbs) {
            if (abstractOrb.hb.hovered) {
                orbHovered = true;
                break;
            }
            ++orbIndex;
        }
        if (orbHovered && (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed())) {
            CInputActionSet.up.unpress();
            CInputActionSet.altUp.unpress();
            this.inspectMode = false;
            this.inspectHb = null;
            orbHovered = false;
            this.viewingRelics = true;
            if (!this.blights.isEmpty()) {
                CInputHelper.setCursor(this.blights.get((int)0).hb);
            } else {
                CInputHelper.setCursor(this.relics.get((int)0).hb);
            }
        } else if (orbHovered && (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed())) {
            if (++orbIndex > this.orbs.size() - 1) {
                orbIndex = 0;
            }
            this.inspectHb = this.orbs.get((int)orbIndex).hb;
            Gdx.input.setCursorPosition((int)this.orbs.get((int)orbIndex).hb.cX, Settings.HEIGHT - (int)this.orbs.get((int)orbIndex).hb.cY);
        } else if (orbHovered && (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed())) {
            if (--orbIndex < 0) {
                orbIndex = this.orbs.size() - 1;
            }
            this.inspectHb = this.orbs.get((int)orbIndex).hb;
            Gdx.input.setCursorPosition((int)this.orbs.get((int)orbIndex).hb.cX, Settings.HEIGHT - (int)this.orbs.get((int)orbIndex).hb.cY);
        } else if (this.inspectMode && (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed())) {
            if (orbHovered) {
                this.inspectHb = this.hb;
                CInputHelper.setCursor(this.inspectHb);
            } else {
                this.inspectMode = false;
                this.inspectHb = null;
                if (!this.hand.isEmpty() && !AbstractDungeon.actionManager.turnHasEnded) {
                    this.hoveredCard = this.hand.group.get(0);
                    this.hoverCardInHand(this.hoveredCard);
                    this.keyboardCardIndex = 0;
                }
            }
        } else if (!this.inspectMode && (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT && !AbstractDungeon.isScreenUp) {
            if ((float)Gdx.input.getX() < (float)Settings.WIDTH / 2.0f) {
                this.inspectHb = this.hb;
            } else if (!AbstractDungeon.getMonsters().monsters.isEmpty()) {
                hbs = new ArrayList();
                for (AbstractMonster abstractMonster : AbstractDungeon.getMonsters().monsters) {
                    if (abstractMonster.isDying || abstractMonster.isEscaping) continue;
                    hbs.add(abstractMonster.hb);
                }
                this.inspectHb = !hbs.isEmpty() ? (Hitbox)hbs.get(0) : this.hb;
            } else {
                this.inspectHb = this.hb;
            }
            CInputHelper.setCursor(this.inspectHb);
            this.inspectMode = true;
            this.releaseCard();
        } else if (this.inspectMode && (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            hbs = new ArrayList<Hitbox>();
            hbs.add(this.hb);
            for (AbstractMonster abstractMonster : AbstractDungeon.getMonsters().monsters) {
                if (abstractMonster.isDying || abstractMonster.isEscaping) continue;
                hbs.add(abstractMonster.hb);
            }
            boolean bl = false;
            for (Hitbox h : hbs) {
                void var5_9;
                h.update();
                if (h.hovered) {
                    anyHovered = true;
                    break;
                }
                ++var5_9;
            }
            if (!anyHovered) {
                CInputHelper.setCursor((Hitbox)hbs.get(0));
                this.inspectHb = (Hitbox)hbs.get(0);
            } else {
                int n;
                void var5_10;
                if (++var5_10 > hbs.size() - 1) {
                    n = 0;
                }
                CInputHelper.setCursor((Hitbox)hbs.get(n));
                this.inspectHb = (Hitbox)hbs.get(n);
            }
            this.inspectMode = true;
            this.releaseCard();
        } else if (this.inspectMode && (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            hbs = new ArrayList();
            hbs.add(this.hb);
            for (AbstractMonster abstractMonster : AbstractDungeon.getMonsters().monsters) {
                if (abstractMonster.isDying || abstractMonster.isEscaping) continue;
                hbs.add(abstractMonster.hb);
            }
            boolean bl = false;
            for (Hitbox h : hbs) {
                void var5_14;
                if (h.hovered) {
                    anyHovered = true;
                    break;
                }
                ++var5_14;
            }
            if (!anyHovered) {
                CInputHelper.setCursor((Hitbox)hbs.get(hbs.size() - 1));
                this.inspectHb = (Hitbox)hbs.get(hbs.size() - 1);
            } else {
                int n;
                void var5_15;
                if (--var5_15 < 0) {
                    n = hbs.size() - 1;
                }
                CInputHelper.setCursor((Hitbox)hbs.get(n));
                this.inspectHb = (Hitbox)hbs.get(n);
            }
            this.inspectMode = true;
            this.releaseCard();
        } else if (this.inspectMode && (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            CInputActionSet.up.unpress();
            CInputActionSet.altUp.unpress();
            if (!orbHovered && !this.orbs.isEmpty()) {
                CInputHelper.setCursor(this.orbs.get((int)0).hb);
                this.inspectHb = this.orbs.get((int)0).hb;
            } else {
                this.inspectMode = false;
                this.inspectHb = null;
                this.viewingRelics = true;
                if (!this.blights.isEmpty()) {
                    CInputHelper.setCursor(this.blights.get((int)0).hb);
                } else {
                    CInputHelper.setCursor(this.relics.get((int)0).hb);
                }
            }
        }
    }

    public void updateViewRelicControls() {
        int index = 0;
        boolean anyHovered = false;
        RHoverType type = RHoverType.RELIC;
        for (AbstractRelic r : this.relics) {
            if (r.hb.hovered) {
                anyHovered = true;
                break;
            }
            ++index;
        }
        if (!anyHovered) {
            index = 0;
            for (AbstractBlight b : this.blights) {
                if (b.hb.hovered) {
                    anyHovered = true;
                    type = RHoverType.BLIGHT;
                    break;
                }
                ++index;
            }
        }
        if (!anyHovered) {
            CInputHelper.setCursor(this.relics.get((int)0).hb);
        } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
            --index;
            if (type == RHoverType.RELIC) {
                if (index < AbstractRelic.relicPage * AbstractRelic.MAX_RELICS_PER_PAGE) {
                    if (--AbstractRelic.relicPage < 0) {
                        if (this.relics.size() <= AbstractRelic.MAX_RELICS_PER_PAGE) {
                            AbstractRelic.relicPage = 0;
                        } else {
                            AbstractRelic.relicPage = this.relics.size() / AbstractRelic.MAX_RELICS_PER_PAGE;
                            AbstractDungeon.topPanel.adjustRelicHbs();
                        }
                        index = this.relics.size() - 1;
                    } else {
                        index = (AbstractRelic.relicPage + 1) * AbstractRelic.MAX_RELICS_PER_PAGE - 1;
                        AbstractDungeon.topPanel.adjustRelicHbs();
                    }
                }
                CInputHelper.setCursor(this.relics.get((int)index).hb);
            } else {
                if (index < 0) {
                    index = this.blights.size() - 1;
                }
                CInputHelper.setCursor(this.blights.get((int)index).hb);
            }
        } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
            ++index;
            if (type == RHoverType.RELIC) {
                if (index > this.relics.size() - 1) {
                    index = 0;
                } else if (index > (AbstractRelic.relicPage + 1) * AbstractRelic.MAX_RELICS_PER_PAGE - 1) {
                    AbstractDungeon.topPanel.adjustRelicHbs();
                    index = ++AbstractRelic.relicPage * AbstractRelic.MAX_RELICS_PER_PAGE;
                }
                CInputHelper.setCursor(this.relics.get((int)index).hb);
            } else {
                if (index > this.blights.size() - 1) {
                    index = 0;
                }
                CInputHelper.setCursor(this.blights.get((int)index).hb);
            }
        } else if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
            CInputActionSet.up.unpress();
            if (type == RHoverType.RELIC) {
                this.viewingRelics = false;
                AbstractDungeon.topPanel.selectPotionMode = true;
                CInputHelper.setCursor(this.potions.get((int)0).hb);
            } else {
                CInputHelper.setCursor(this.relics.get((int)0).hb);
            }
        } else if (CInputActionSet.cancel.isJustPressed()) {
            this.viewingRelics = false;
            Gdx.input.setCursorPosition(10, Settings.HEIGHT / 2);
        } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
            if (type == RHoverType.RELIC) {
                if (this.blights.isEmpty()) {
                    CInputActionSet.down.unpress();
                    CInputActionSet.altDown.unpress();
                    this.viewingRelics = false;
                    this.inspectMode = true;
                    this.inspectHb = !this.orbs.isEmpty() ? this.orbs.get((int)0).hb : this.hb;
                    CInputHelper.setCursor(this.inspectHb);
                } else {
                    CInputHelper.setCursor(this.blights.get((int)0).hb);
                }
            } else {
                CInputActionSet.down.unpress();
                CInputActionSet.altDown.unpress();
                this.viewingRelics = false;
                this.inspectMode = true;
                this.inspectHb = !this.orbs.isEmpty() ? this.orbs.get((int)0).hb : this.hb;
                CInputHelper.setCursor(this.inspectHb);
            }
        }
    }

    @Override
    public void loseGold(int goldAmount) {
        if (AbstractDungeon.getCurrRoom() instanceof ShopRoom) {
            for (AbstractRelic r : this.relics) {
                r.onSpendGold();
            }
        }
        if (!(AbstractDungeon.getCurrRoom() instanceof ShopRoom) && AbstractDungeon.getCurrRoom().phase != AbstractRoom.RoomPhase.COMBAT) {
            CardCrawlGame.sound.play("EVENT_PURCHASE");
        }
        if (goldAmount > 0) {
            this.gold -= goldAmount;
            if (this.gold < 0) {
                this.gold = 0;
            }
            for (AbstractRelic r : this.relics) {
                r.onLoseGold();
            }
        } else {
            logger.info("NEGATIVE MONEY???");
        }
    }

    @Override
    public void gainGold(int amount) {
        if (this.hasRelic("Ectoplasm")) {
            this.getRelic("Ectoplasm").flash();
            return;
        }
        if (amount <= 0) {
            logger.info("NEGATIVE MONEY???");
        } else {
            CardCrawlGame.goldGained += amount;
            this.gold += amount;
            for (AbstractRelic r : this.relics) {
                r.onGainGold();
            }
        }
    }

    public void playDeathAnimation() {
        this.img = this.corpseImg;
        this.renderCorpse = true;
    }

    public boolean isCursed() {
        boolean cursed = false;
        for (AbstractCard c : this.masterDeck.group) {
            if (c.type != AbstractCard.CardType.CURSE || c.cardID == "Necronomicurse" || c.cardID == "CurseOfTheBell" || c.cardID == "AscendersBane") continue;
            cursed = true;
        }
        return cursed;
    }

    /*
     * WARNING - void declaration
     */
    public void updateInput() {
        if (this.viewingRelics) {
            return;
        }
        if (this.hoverEnemyWaitTimer > 0.0f) {
            this.hoverEnemyWaitTimer -= Gdx.graphics.getDeltaTime();
        }
        if (this.inSingleTargetMode) {
            this.updateSingleTargetInput();
        } else {
            int y = InputHelper.mY;
            boolean hMonster = false;
            for (AbstractMonster abstractMonster : AbstractDungeon.getCurrRoom().monsters.monsters) {
                abstractMonster.hb.update();
                if (!abstractMonster.hb.hovered || abstractMonster.isDying || abstractMonster.isEscaping || abstractMonster.currentHealth <= 0) continue;
                hMonster = true;
                break;
            }
            boolean tmp = this.isHoveringDropZone;
            if (!Settings.isTouchScreen) {
                this.isHoveringDropZone = ((float)y > this.hoverStartLine || (float)y > 300.0f * Settings.scale) && (float)y < Settings.CARD_DROP_END_Y || hMonster || Settings.isControllerMode;
            } else {
                boolean bl = this.isHoveringDropZone = (float)y > 350.0f * Settings.scale && (float)y < Settings.CARD_DROP_END_Y || hMonster || Settings.isControllerMode;
            }
            if (!tmp && this.isHoveringDropZone && this.isDraggingCard) {
                this.hoveredCard.flash(Color.SKY.cpy());
                if (this.hoveredCard.showEvokeValue) {
                    if (this.hoveredCard.showEvokeOrbCount == 0) {
                        for (AbstractOrb o : this.orbs) {
                            o.showEvokeValue();
                        }
                    } else {
                        int n = this.hoveredCard.showEvokeOrbCount;
                        int emptyCount = 0;
                        for (AbstractOrb o : this.orbs) {
                            if (!(o instanceof EmptyOrbSlot)) continue;
                            ++emptyCount;
                        }
                        int n2 = n - emptyCount;
                        if (n2 > 0) {
                            for (AbstractOrb o : this.orbs) {
                                void var4_9;
                                o.showEvokeValue();
                                if (--var4_9 > 0) continue;
                                break;
                            }
                        }
                    }
                }
            }
            if (this.isDraggingCard && this.isHoveringDropZone && this.hoveredCard != null) {
                this.passedHesitationLine = true;
            }
            if (this.isDraggingCard && (float)y < 50.0f * Settings.scale && this.passedHesitationLine) {
                if (Settings.isTouchScreen) {
                    InputHelper.moveCursorToNeutralPosition();
                }
                this.releaseCard();
                CardCrawlGame.sound.play("UI_CLICK_1");
            }
            this.updateFullKeyboardCardSelection();
            if (this.isInKeyboardMode && !AbstractDungeon.topPanel.selectPotionMode && AbstractDungeon.topPanel.potionUi.isHidden && !AbstractDungeon.topPanel.potionUi.targetMode && !AbstractDungeon.isScreenUp) {
                if (this.toHover != null) {
                    this.releaseCard();
                    this.hoveredCard = this.toHover;
                    this.toHover = null;
                    this.isHoveringCard = true;
                    this.hoveredCard.current_y = HOVER_CARD_Y_POSITION;
                    this.hoveredCard.target_y = HOVER_CARD_Y_POSITION;
                    this.hoveredCard.setAngle(0.0f, true);
                    this.hand.hoverCardPush(this.hoveredCard);
                }
            } else if (this.hoveredCard == null && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NONE && !AbstractDungeon.topPanel.selectPotionMode) {
                this.isHoveringCard = false;
                if (this.toHover != null) {
                    this.hoveredCard = this.toHover;
                    this.toHover = null;
                } else {
                    this.hoveredCard = this.hand.getHoveredCard();
                }
                if (this.hoveredCard != null) {
                    this.isHoveringCard = true;
                    this.hoveredCard.current_y = HOVER_CARD_Y_POSITION;
                    this.hoveredCard.target_y = HOVER_CARD_Y_POSITION;
                    this.hoveredCard.setAngle(0.0f, true);
                    this.hand.hoverCardPush(this.hoveredCard);
                }
            }
            if (this.hoveredCard != null) {
                this.hoveredCard.drawScale = 1.0f;
                this.hoveredCard.targetDrawScale = 1.0f;
            }
            if (!this.isDraggingCard && this.hasRelic("Necronomicon")) {
                this.getRelic("Necronomicon").stopPulse();
            }
            if (!this.endTurnQueued) {
                if (!AbstractDungeon.actionManager.turnHasEnded && this.clickAndDragCards()) {
                    return;
                }
                if (!this.isInKeyboardMode && this.isHoveringCard && this.hoveredCard != null && !this.hoveredCard.isHoveredInHand(1.0f)) {
                    void var4_11;
                    boolean bl = false;
                    while (var4_11 < this.hand.group.size()) {
                        if (this.hand.group.get((int)var4_11) == this.hoveredCard && var4_11 != false && this.hand.group.get((int)(var4_11 - true)).isHoveredInHand(1.0f)) {
                            this.toHover = this.hand.group.get((int)(var4_11 - true));
                            break;
                        }
                        ++var4_11;
                    }
                    this.releaseCard();
                }
                if (this.hoveredCard != null) {
                    this.hoveredCard.updateHoverLogic();
                }
            } else if (AbstractDungeon.actionManager.cardQueue.isEmpty() && !AbstractDungeon.actionManager.hasControl) {
                this.endTurnQueued = false;
                this.isEndingTurn = true;
            }
        }
    }

    private void updateSingleTargetInput() {
        if (Settings.isTouchScreen && !Settings.isControllerMode && !this.isUsingClickDragControl && !InputHelper.isMouseDown) {
            Gdx.input.setCursorPosition((int)MathUtils.lerp(Gdx.input.getX(), (float)Settings.WIDTH / 2.0f, Gdx.graphics.getDeltaTime() * 10.0f), (int)MathUtils.lerp(Gdx.input.getY(), (float)Settings.HEIGHT * 1.1f, Gdx.graphics.getDeltaTime() * 4.0f));
        }
        if (this.isInKeyboardMode) {
            if (InputActionSet.releaseCard.isJustPressed() || CInputActionSet.cancel.isJustPressed()) {
                AbstractCard card = this.hoveredCard;
                this.inSingleTargetMode = false;
                this.hoveredMonster = null;
                this.hoverCardInHand(card);
            } else {
                this.updateTargetArrowWithKeyboard(false);
            }
        } else {
            this.hoveredMonster = null;
            for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
                m.hb.update();
                if (!m.hb.hovered || m.isDying || m.isEscaping || m.currentHealth <= 0) continue;
                this.hoveredMonster = m;
                break;
            }
        }
        if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead() || InputHelper.justClickedRight || (float)InputHelper.mY < 50.0f * Settings.scale || (float)InputHelper.mY < this.hoverStartLine - 400.0f * Settings.scale) {
            if (Settings.isTouchScreen) {
                InputHelper.moveCursorToNeutralPosition();
            }
            this.releaseCard();
            CardCrawlGame.sound.play("UI_CLICK_2");
            this.isUsingClickDragControl = false;
            this.inSingleTargetMode = false;
            GameCursor.hidden = false;
            this.hoveredMonster = null;
            return;
        }
        AbstractCard cardFromHotkey = InputHelper.getCardSelectedByHotkey(this.hand);
        if (cardFromHotkey != null && !this.isCardQueued(cardFromHotkey)) {
            boolean isSameCard = cardFromHotkey == this.hoveredCard;
            this.releaseCard();
            this.hoveredMonster = null;
            if (isSameCard) {
                GameCursor.hidden = false;
            } else {
                this.hoveredCard = cardFromHotkey;
                this.hoveredCard.setAngle(0.0f, false);
                this.isUsingClickDragControl = true;
                this.isDraggingCard = true;
            }
        }
        if (InputHelper.justClickedLeft || InputActionSet.confirm.isJustPressed() || CInputActionSet.select.isJustPressed()) {
            InputHelper.justClickedLeft = false;
            if (this.hoveredMonster == null) {
                CardCrawlGame.sound.play("UI_CLICK_1");
                return;
            }
            if (this.hoveredCard.canUse(this, this.hoveredMonster)) {
                this.playCard();
            } else {
                AbstractDungeon.effectList.add(new ThoughtBubble(this.dialogX, this.dialogY, 3.0f, this.hoveredCard.cantUseMessage, true));
                this.energyTip(this.hoveredCard);
                this.releaseCard();
            }
            this.isUsingClickDragControl = false;
            this.inSingleTargetMode = false;
            GameCursor.hidden = false;
            this.hoveredMonster = null;
            return;
        }
        if (!this.isUsingClickDragControl && InputHelper.justReleasedClickLeft && this.hoveredMonster != null) {
            if (this.hoveredCard.canUse(this, this.hoveredMonster)) {
                this.playCard();
            } else {
                AbstractDungeon.effectList.add(new ThoughtBubble(this.dialogX, this.dialogY, 3.0f, this.hoveredCard.cantUseMessage, true));
                this.energyTip(this.hoveredCard);
                this.releaseCard();
            }
            this.inSingleTargetMode = false;
            GameCursor.hidden = false;
            this.hoveredMonster = null;
            return;
        }
    }

    private boolean isCardQueued(AbstractCard card) {
        for (CardQueueItem item : AbstractDungeon.actionManager.cardQueue) {
            if (item.card != card) continue;
            return true;
        }
        return false;
    }

    private void energyTip(AbstractCard cardToCheck) {
        int availableEnergy = EnergyPanel.totalCount;
        if (cardToCheck.cost > availableEnergy && !TipTracker.tips.get("ENERGY_USE_TIP").booleanValue() && ++TipTracker.energyUseCounter >= 2) {
            AbstractDungeon.ftue = new FtueTip(LABEL[1], MSG[1], 330.0f * Settings.scale, 400.0f * Settings.scale, FtueTip.TipType.ENERGY);
            TipTracker.neverShowAgain("ENERGY_USE_TIP");
        }
    }

    private boolean updateFullKeyboardCardSelection() {
        AbstractCard card;
        if (Settings.isControllerMode || InputActionSet.left.isJustPressed() || InputActionSet.right.isJustPressed() || InputActionSet.confirm.isJustPressed()) {
            this.isInKeyboardMode = true;
            this.skipMouseModeOnce = true;
            GameCursor.hidden = true;
        }
        if (this.isInKeyboardMode && InputHelper.didMoveMouse()) {
            if (this.skipMouseModeOnce) {
                this.skipMouseModeOnce = false;
            } else {
                this.isInKeyboardMode = false;
                GameCursor.hidden = false;
            }
        }
        if (!this.isInKeyboardMode || this.hand.isEmpty() || this.inspectMode) {
            return false;
        }
        if (this.keyboardCardIndex == -2) {
            if (InputActionSet.left.isJustPressed() || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                this.keyboardCardIndex = this.hand.size() - 1;
            } else if (InputActionSet.right.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
                this.keyboardCardIndex = 0;
            }
            return false;
        }
        if (InputActionSet.left.isJustPressed() || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
            --this.keyboardCardIndex;
        } else if (InputActionSet.right.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
            ++this.keyboardCardIndex;
        }
        this.keyboardCardIndex = (this.keyboardCardIndex + this.hand.size()) % this.hand.size();
        if (!AbstractDungeon.topPanel.selectPotionMode && AbstractDungeon.topPanel.potionUi.isHidden && !AbstractDungeon.topPanel.potionUi.targetMode && (card = this.hand.group.get(this.keyboardCardIndex)) != this.hoveredCard && Math.abs(card.current_x - card.target_x) < 400.0f * Settings.scale) {
            this.hoverCardInHand(card);
            return true;
        }
        return false;
    }

    private void hoverCardInHand(AbstractCard card) {
        this.toHover = card;
        if (Settings.isControllerMode && AbstractDungeon.actionManager.turnHasEnded) {
            this.toHover = null;
        }
        if (card != null && !this.inspectMode) {
            Gdx.input.setCursorPosition((int)card.hb.cX, (int)((float)Settings.HEIGHT - HOVER_CARD_Y_POSITION));
        }
    }

    private void updateTargetArrowWithKeyboard(boolean autoTargetFirst) {
        int directionIndex = 0;
        if (autoTargetFirst) {
            ++directionIndex;
        }
        if (InputActionSet.left.isJustPressed() || CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
            --directionIndex;
        }
        if (InputActionSet.right.isJustPressed() || CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
            ++directionIndex;
        }
        if (directionIndex != 0) {
            ArrayList<AbstractMonster> prefiltered = AbstractDungeon.getCurrRoom().monsters.monsters;
            ArrayList<AbstractMonster> sortedMonsters = new ArrayList<AbstractMonster>(AbstractDungeon.getCurrRoom().monsters.monsters);
            for (AbstractMonster mons : prefiltered) {
                if (!mons.isDying) continue;
                sortedMonsters.remove(mons);
            }
            sortedMonsters.sort(AbstractMonster.sortByHitbox);
            if (sortedMonsters.isEmpty()) {
                return;
            }
            for (AbstractMonster m : sortedMonsters) {
                if (!m.hb.hovered) continue;
                this.hoveredMonster = m;
                break;
            }
            AbstractMonster newTarget = null;
            if (this.hoveredMonster == null) {
                newTarget = directionIndex == 1 ? sortedMonsters.get(0) : sortedMonsters.get(sortedMonsters.size() - 1);
            } else {
                int currentTargetIndex = sortedMonsters.indexOf(this.hoveredMonster);
                int newTargetIndex = currentTargetIndex + directionIndex;
                newTargetIndex = (newTargetIndex + sortedMonsters.size()) % sortedMonsters.size();
                newTarget = sortedMonsters.get(newTargetIndex);
            }
            if (newTarget != null) {
                Hitbox target = newTarget.hb;
                Gdx.input.setCursorPosition((int)target.cX, Settings.HEIGHT - (int)target.cY);
                this.hoveredMonster = newTarget;
                this.isUsingClickDragControl = true;
                this.isDraggingCard = true;
            }
            if (this.hoveredMonster.halfDead) {
                this.hoveredMonster = null;
            }
        }
    }

    private void renderCardHotKeyText(SpriteBatch sb) {
        int index = 0;
        for (AbstractCard card : this.hand.group) {
            if (index < InputActionSet.selectCardActions.length) {
                float width = AbstractCard.IMG_WIDTH * card.drawScale / 2.0f;
                float height = AbstractCard.IMG_HEIGHT * card.drawScale / 2.0f;
                float topOfCard = card.current_y + height;
                float textSpacing = 50.0f * Settings.scale;
                float textY = topOfCard + textSpacing;
                float sin = (float)Math.sin((double)(card.angle / 180.0f) * Math.PI);
                float xOffset = sin * width;
                FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, InputActionSet.selectCardActions[index].getKeyString(), card.current_x - xOffset, textY, Settings.CREAM_COLOR);
            }
            ++index;
        }
    }

    private boolean clickAndDragCards() {
        boolean simulateRightClickDrop = false;
        AbstractCard cardFromHotkey = InputHelper.getCardSelectedByHotkey(this.hand);
        if (cardFromHotkey != null && !this.isCardQueued(cardFromHotkey)) {
            if (this.isDraggingCard) {
                simulateRightClickDrop = cardFromHotkey == this.hoveredCard;
                CardCrawlGame.sound.play("UI_CLICK_2");
                this.releaseCard();
            }
            if (!simulateRightClickDrop) {
                this.manuallySelectCard(cardFromHotkey);
            }
        }
        if (CInputActionSet.select.isJustPressed() && this.hoveredCard != null && !this.isCardQueued(this.hoveredCard) && !this.isDraggingCard) {
            this.manuallySelectCard(this.hoveredCard);
            if (this.hoveredCard.target == AbstractCard.CardTarget.ENEMY) {
                this.updateTargetArrowWithKeyboard(true);
            } else {
                InputHelper.moveCursorToNeutralPosition();
            }
            return true;
        }
        if (InputHelper.justClickedLeft && this.isHoveringCard && !this.isDraggingCard) {
            this.hoverStartLine = (float)InputHelper.mY + 140.0f * Settings.scale;
            InputHelper.justClickedLeft = false;
            if (this.hoveredCard != null) {
                CardCrawlGame.sound.play("CARD_OBTAIN");
                this.isDraggingCard = true;
                this.passedHesitationLine = false;
                this.hoveredCard.targetDrawScale = 0.7f;
                if (Settings.isTouchScreen && !Settings.isControllerMode && this.touchscreenInspectCount == 0) {
                    this.hoveredCard.current_y = AbstractCard.IMG_HEIGHT / 2.0f;
                    this.hoveredCard.target_y = AbstractCard.IMG_HEIGHT / 2.0f;
                    Gdx.input.setCursorPosition((int)this.hoveredCard.current_x, (int)((float)Settings.HEIGHT - AbstractCard.IMG_HEIGHT / 2.0f));
                    this.touchscreenInspectCount = 0;
                }
                return true;
            }
        }
        this.clickDragTimer = InputHelper.isMouseDown ? (this.clickDragTimer += Gdx.graphics.getDeltaTime()) : 0.0f;
        if ((InputHelper.justClickedLeft || InputActionSet.confirm.isJustPressed() || CInputActionSet.select.isJustPressed()) && this.isUsingClickDragControl) {
            if (InputHelper.justClickedRight || simulateRightClickDrop) {
                CardCrawlGame.sound.play("UI_CLICK_2");
                this.releaseCard();
                return true;
            }
            InputHelper.justClickedLeft = false;
            if (this.isHoveringDropZone && this.hoveredCard.canUse(this, null) && this.hoveredCard.target != AbstractCard.CardTarget.ENEMY && this.hoveredCard.target != AbstractCard.CardTarget.SELF_AND_ENEMY) {
                this.playCard();
            } else {
                CardCrawlGame.sound.play("CARD_OBTAIN");
                this.releaseCard();
            }
            this.isUsingClickDragControl = false;
            return true;
        }
        if (this.isInKeyboardMode) {
            if (InputActionSet.releaseCard.isJustPressed() || CInputActionSet.cancel.isJustPressed()) {
                this.hoverCardInHand(this.hoveredCard);
            } else if ((InputActionSet.confirm.isJustPressed() || CInputActionSet.select.isJustPressed()) && this.hoveredCard != null) {
                this.manuallySelectCard(this.hoveredCard);
                if (this.hoveredCard.target == AbstractCard.CardTarget.ENEMY) {
                    this.updateTargetArrowWithKeyboard(true);
                } else {
                    Gdx.input.setCursorPosition(10, Settings.HEIGHT / 2);
                }
            }
        }
        if (this.isDraggingCard && (InputHelper.isMouseDown || this.isUsingClickDragControl)) {
            if (InputHelper.justClickedRight || simulateRightClickDrop) {
                CardCrawlGame.sound.play("UI_CLICK_2");
                this.releaseCard();
                return true;
            }
            if (Settings.isTouchScreen && !Settings.isControllerMode) {
                this.hoveredCard.target_x = InputHelper.mX;
                this.hoveredCard.target_y = (float)InputHelper.mY + 270.0f * Settings.scale;
            } else {
                this.hoveredCard.target_x = InputHelper.mX;
                this.hoveredCard.target_y = InputHelper.mY;
            }
            if (!this.hoveredCard.hasEnoughEnergy() && this.isHoveringDropZone) {
                AbstractDungeon.effectList.add(new ThoughtBubble(this.dialogX, this.dialogY, 3.0f, this.hoveredCard.cantUseMessage, true));
                this.energyTip(this.hoveredCard);
                this.releaseCard();
                CardCrawlGame.sound.play("CARD_REJECT");
                return true;
            }
            if (this.isHoveringDropZone && (this.hoveredCard.target == AbstractCard.CardTarget.ENEMY || this.hoveredCard.target == AbstractCard.CardTarget.SELF_AND_ENEMY)) {
                this.inSingleTargetMode = true;
                this.arrowX = InputHelper.mX;
                this.arrowY = InputHelper.mY;
                GameCursor.hidden = true;
                this.hoveredCard.untip();
                this.hand.refreshHandLayout();
                if (Settings.isTouchScreen && !Settings.isControllerMode) {
                    this.hoveredCard.target_y = 260.0f * Settings.scale;
                    this.hoveredCard.target_x = (float)Settings.WIDTH / 2.0f;
                    this.hoveredCard.targetDrawScale = 1.0f;
                } else {
                    this.hoveredCard.target_y = AbstractCard.IMG_HEIGHT * 0.75f / 2.5f;
                    this.hoveredCard.target_x = (float)Settings.WIDTH / 2.0f;
                }
                this.isDraggingCard = false;
            }
            return true;
        }
        if (this.isDraggingCard && InputHelper.justReleasedClickLeft && (!Settings.isTouchScreen || Settings.isControllerMode)) {
            if (this.isHoveringDropZone) {
                if (this.hoveredCard.target == AbstractCard.CardTarget.ENEMY || this.hoveredCard.target == AbstractCard.CardTarget.SELF_AND_ENEMY) {
                    this.inSingleTargetMode = true;
                    this.arrowX = InputHelper.mX;
                    this.arrowY = InputHelper.mY;
                    GameCursor.hidden = true;
                    this.hoveredCard.untip();
                    this.hand.refreshHandLayout();
                    this.hoveredCard.target_y = AbstractCard.IMG_HEIGHT * 0.75f / 2.5f;
                    this.hoveredCard.target_x = (float)Settings.WIDTH / 2.0f;
                    this.isDraggingCard = false;
                    return true;
                }
                if (this.hoveredCard.canUse(this, null)) {
                    this.playCard();
                    return true;
                }
                AbstractDungeon.effectList.add(new ThoughtBubble(this.dialogX, this.dialogY, 3.0f, this.hoveredCard.cantUseMessage, true));
                this.energyTip(this.hoveredCard);
                this.releaseCard();
                return true;
            }
            if (this.clickDragTimer < 0.4f) {
                this.isUsingClickDragControl = true;
                return true;
            }
            if (AbstractDungeon.actionManager.currentAction == null) {
                this.releaseCard();
                CardCrawlGame.sound.play("CARD_OBTAIN");
                return true;
            }
        } else if (Settings.isTouchScreen && !Settings.isControllerMode && InputHelper.justReleasedClickLeft && this.hoveredCard != null) {
            ++this.touchscreenInspectCount;
            if (this.isHoveringDropZone && this.hoveredCard.hasEnoughEnergy() && this.hoveredCard.canUse(this, null)) {
                this.playCard();
                return true;
            }
            if (this.touchscreenInspectCount > 1) {
                AbstractCard newHoveredCard = null;
                for (AbstractCard c : this.hand.group) {
                    c.updateHoverLogic();
                    if (!c.hb.hovered || c == this.hoveredCard) continue;
                    newHoveredCard = c;
                    break;
                }
                this.releaseCard();
                if (newHoveredCard == null) {
                    InputHelper.moveCursorToNeutralPosition();
                } else {
                    newHoveredCard.current_y = AbstractCard.IMG_HEIGHT / 2.0f;
                    newHoveredCard.target_y = AbstractCard.IMG_HEIGHT / 2.0f;
                    newHoveredCard.angle = 0.0f;
                    Gdx.input.setCursorPosition((int)newHoveredCard.current_x, (int)((float)Settings.HEIGHT - AbstractCard.IMG_HEIGHT / 2.0f));
                    this.touchscreenInspectCount = 1;
                }
            }
        }
        return false;
    }

    private void manuallySelectCard(AbstractCard card) {
        block3: {
            block4: {
                this.hoveredCard = card;
                this.hoveredCard.setAngle(0.0f, false);
                this.isUsingClickDragControl = true;
                this.isDraggingCard = true;
                this.hoveredCard.flash(Color.SKY.cpy());
                if (!this.hoveredCard.showEvokeValue) break block3;
                if (this.hoveredCard.showEvokeOrbCount != 0) break block4;
                for (AbstractOrb o : this.orbs) {
                    o.showEvokeValue();
                }
                break block3;
            }
            int tmpShowCount = this.hoveredCard.showEvokeOrbCount;
            int emptyCount = 0;
            for (AbstractOrb o : this.orbs) {
                if (!(o instanceof EmptyOrbSlot)) continue;
                ++emptyCount;
            }
            if ((tmpShowCount -= emptyCount) <= 0) break block3;
            for (AbstractOrb o : this.orbs) {
                o.showEvokeValue();
                if (--tmpShowCount > 0) continue;
                break;
            }
        }
    }

    private void playCard() {
        InputHelper.justClickedLeft = false;
        this.hoverEnemyWaitTimer = 1.0f;
        this.hoveredCard.unhover();
        if (!this.queueContains(this.hoveredCard)) {
            if (this.hoveredCard.target == AbstractCard.CardTarget.ENEMY || this.hoveredCard.target == AbstractCard.CardTarget.SELF_AND_ENEMY) {
                if (this.hasPower("Surrounded")) {
                    this.flipHorizontal = this.hoveredMonster.drawX < this.drawX;
                }
                AbstractDungeon.actionManager.cardQueue.add(new CardQueueItem(this.hoveredCard, this.hoveredMonster));
            } else {
                AbstractDungeon.actionManager.cardQueue.add(new CardQueueItem(this.hoveredCard, null));
            }
        }
        this.isUsingClickDragControl = false;
        this.hoveredCard = null;
        this.isDraggingCard = false;
    }

    private boolean queueContains(AbstractCard card) {
        for (CardQueueItem i : AbstractDungeon.actionManager.cardQueue) {
            if (i.card != card) continue;
            return true;
        }
        return false;
    }

    public void releaseCard() {
        for (AbstractOrb o : this.orbs) {
            o.hideEvokeValues();
        }
        this.passedHesitationLine = false;
        InputHelper.justClickedLeft = false;
        InputHelper.justReleasedClickLeft = false;
        InputHelper.isMouseDown = false;
        this.inSingleTargetMode = false;
        if (!this.isInKeyboardMode) {
            GameCursor.hidden = false;
        }
        this.isUsingClickDragControl = false;
        this.isHoveringDropZone = false;
        this.isDraggingCard = false;
        this.isHoveringCard = false;
        if (this.hoveredCard != null) {
            if (this.hoveredCard.canUse(this, null)) {
                this.hoveredCard.beginGlowing();
            }
            this.hoveredCard.untip();
            this.hoveredCard.hoverTimer = 0.25f;
            this.hoveredCard.unhover();
        }
        this.hoveredCard = null;
        this.hand.refreshHandLayout();
        this.touchscreenInspectCount = 0;
    }

    public void onCardDrawOrDiscard() {
        for (AbstractPower p : this.powers) {
            p.onDrawOrDiscard();
        }
        for (AbstractRelic r : this.relics) {
            r.onDrawOrDiscard();
        }
        if (this.hasPower("Corruption")) {
            for (AbstractCard c : this.hand.group) {
                if (c.type != AbstractCard.CardType.SKILL || c.costForTurn == 0) continue;
                c.modifyCostForCombat(-9);
            }
        }
        this.hand.applyPowers();
        this.hand.glowCheck();
    }

    public void useCard(AbstractCard c, AbstractMonster monster, int energyOnUse) {
        if (c.type == AbstractCard.CardType.ATTACK) {
            this.useFastAttackAnimation();
        }
        c.calculateCardDamage(monster);
        if (c.cost == -1 && EnergyPanel.totalCount < energyOnUse && !c.ignoreEnergyOnUse) {
            c.energyOnUse = EnergyPanel.totalCount;
        }
        if (c.cost == -1 && c.isInAutoplay) {
            c.freeToPlayOnce = true;
        }
        c.use(this, monster);
        AbstractDungeon.actionManager.addToBottom(new UseCardAction(c, monster));
        if (!c.dontTriggerOnUseCard) {
            this.hand.triggerOnOtherCardPlayed(c);
        }
        this.hand.removeCard(c);
        this.cardInUse = c;
        c.target_x = Settings.WIDTH / 2;
        c.target_y = Settings.HEIGHT / 2;
        if (!(c.costForTurn <= 0 || c.freeToPlay() || c.isInAutoplay || this.hasPower("Corruption") && c.type == AbstractCard.CardType.SKILL)) {
            this.energy.use(c.costForTurn);
        }
        if (!this.hand.canUseAnyCard() && !this.endTurnQueued) {
            AbstractDungeon.overlayMenu.endTurnButton.isGlowing = true;
        }
    }

    @Override
    public void damage(DamageInfo info) {
        int damageAmount = info.output;
        boolean hadBlock = true;
        if (this.currentBlock == 0) {
            hadBlock = false;
        }
        if (damageAmount < 0) {
            damageAmount = 0;
        }
        if (damageAmount > 1 && this.hasPower("IntangiblePlayer")) {
            damageAmount = 1;
        }
        damageAmount = this.decrementBlock(info, damageAmount);
        if (info.owner == this) {
            for (AbstractRelic abstractRelic : this.relics) {
                damageAmount = abstractRelic.onAttackToChangeDamage(info, damageAmount);
            }
        }
        if (info.owner != null) {
            for (AbstractPower abstractPower : info.owner.powers) {
                damageAmount = abstractPower.onAttackToChangeDamage(info, damageAmount);
            }
        }
        for (AbstractRelic abstractRelic : this.relics) {
            damageAmount = abstractRelic.onAttackedToChangeDamage(info, damageAmount);
        }
        for (AbstractPower abstractPower : this.powers) {
            damageAmount = abstractPower.onAttackedToChangeDamage(info, damageAmount);
        }
        if (info.owner == this) {
            for (AbstractRelic abstractRelic : this.relics) {
                abstractRelic.onAttack(info, damageAmount, this);
            }
        }
        if (info.owner != null) {
            for (AbstractPower abstractPower : info.owner.powers) {
                abstractPower.onAttack(info, damageAmount, this);
            }
            for (AbstractPower abstractPower : this.powers) {
                damageAmount = abstractPower.onAttacked(info, damageAmount);
            }
            for (AbstractRelic abstractRelic : this.relics) {
                damageAmount = abstractRelic.onAttacked(info, damageAmount);
            }
        } else {
            logger.info("NO OWNER, DON'T TRIGGER POWERS");
        }
        for (AbstractRelic abstractRelic : this.relics) {
            damageAmount = abstractRelic.onLoseHpLast(damageAmount);
        }
        this.lastDamageTaken = Math.min(damageAmount, this.currentHealth);
        if (damageAmount > 0) {
            for (AbstractPower abstractPower : this.powers) {
                damageAmount = abstractPower.onLoseHp(damageAmount);
            }
            for (AbstractRelic abstractRelic : this.relics) {
                abstractRelic.onLoseHp(damageAmount);
            }
            for (AbstractPower abstractPower : this.powers) {
                abstractPower.wasHPLost(info, damageAmount);
            }
            for (AbstractRelic abstractRelic : this.relics) {
                abstractRelic.wasHPLost(damageAmount);
            }
            if (info.owner != null) {
                for (AbstractPower abstractPower : info.owner.powers) {
                    abstractPower.onInflictDamage(info, damageAmount, this);
                }
            }
            if (info.owner != this) {
                this.useStaggerAnimation();
            }
            if (info.type == DamageInfo.DamageType.HP_LOSS) {
                GameActionManager.hpLossThisCombat += damageAmount;
            }
            GameActionManager.damageReceivedThisTurn += damageAmount;
            GameActionManager.damageReceivedThisCombat += damageAmount;
            this.currentHealth -= damageAmount;
            if (damageAmount > 0 && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
                this.updateCardsOnDamage();
                ++this.damagedThisCombat;
            }
            AbstractDungeon.effectList.add(new StrikeEffect((AbstractCreature)this, this.hb.cX, this.hb.cY, damageAmount));
            if (this.currentHealth < 0) {
                this.currentHealth = 0;
            } else if (this.currentHealth < this.maxHealth / 4) {
                AbstractDungeon.topLevelEffects.add(new BorderFlashEffect(new Color(1.0f, 0.1f, 0.05f, 0.0f)));
            }
            this.healthBarUpdatedEvent();
            if ((float)this.currentHealth <= (float)this.maxHealth / 2.0f && !this.isBloodied) {
                this.isBloodied = true;
                for (AbstractRelic abstractRelic : this.relics) {
                    if (abstractRelic == null) continue;
                    abstractRelic.onBloodied();
                }
            }
            if (this.currentHealth < 1) {
                if (!this.hasRelic("Mark of the Bloom")) {
                    if (this.hasPotion("FairyPotion")) {
                        for (AbstractPotion abstractPotion : this.potions) {
                            if (!abstractPotion.ID.equals("FairyPotion")) continue;
                            abstractPotion.flash();
                            this.currentHealth = 0;
                            abstractPotion.use(this);
                            AbstractDungeon.topPanel.destroyPotion(abstractPotion.slot);
                            return;
                        }
                    } else if (this.hasRelic("Lizard Tail") && ((LizardTail)this.getRelic((String)"Lizard Tail")).counter == -1) {
                        this.currentHealth = 0;
                        this.getRelic("Lizard Tail").onTrigger();
                        return;
                    }
                }
                this.isDead = true;
                AbstractDungeon.deathScreen = new DeathScreen(AbstractDungeon.getMonsters());
                this.currentHealth = 0;
                if (this.currentBlock > 0) {
                    this.loseBlock();
                    AbstractDungeon.effectList.add(new HbBlockBrokenEffect(this.hb.cX - this.hb.width / 2.0f + BLOCK_ICON_X, this.hb.cY - this.hb.height / 2.0f + BLOCK_ICON_Y));
                }
            }
        } else if (this.currentBlock > 0) {
            AbstractDungeon.effectList.add(new BlockedWordEffect(this, this.hb.cX, this.hb.cY, AbstractPlayer.uiStrings.TEXT[0]));
        } else if (hadBlock) {
            AbstractDungeon.effectList.add(new BlockedWordEffect(this, this.hb.cX, this.hb.cY, AbstractPlayer.uiStrings.TEXT[0]));
            AbstractDungeon.effectList.add(new HbBlockBrokenEffect(this.hb.cX - this.hb.width / 2.0f + BLOCK_ICON_X, this.hb.cY - this.hb.height / 2.0f + BLOCK_ICON_Y));
        } else {
            AbstractDungeon.effectList.add(new StrikeEffect((AbstractCreature)this, this.hb.cX, this.hb.cY, 0));
        }
    }

    private void updateCardsOnDamage() {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            for (AbstractCard c : this.hand.group) {
                c.tookDamage();
            }
            for (AbstractCard c : this.discardPile.group) {
                c.tookDamage();
            }
            for (AbstractCard c : this.drawPile.group) {
                c.tookDamage();
            }
        }
    }

    public void updateCardsOnDiscard() {
        for (AbstractCard c : this.hand.group) {
            c.didDiscard();
        }
        for (AbstractCard c : this.discardPile.group) {
            c.didDiscard();
        }
        for (AbstractCard c : this.drawPile.group) {
            c.didDiscard();
        }
    }

    @Override
    public void heal(int healAmount) {
        super.heal(healAmount);
        if ((float)this.currentHealth > (float)this.maxHealth / 2.0f && this.isBloodied) {
            this.isBloodied = false;
            for (AbstractRelic r : this.relics) {
                r.onNotBloodied();
            }
        }
    }

    public void gainEnergy(int e) {
        EnergyPanel.addEnergy(e);
        this.hand.glowCheck();
    }

    public void loseEnergy(int e) {
        EnergyPanel.useEnergy(e);
    }

    public void preBattlePrep() {
        if (!TipTracker.tips.get("COMBAT_TIP").booleanValue()) {
            AbstractDungeon.ftue = new MultiPageFtue();
            TipTracker.neverShowAgain("COMBAT_TIP");
        }
        AbstractDungeon.actionManager.clear();
        this.damagedThisCombat = 0;
        this.cardsPlayedThisTurn = 0;
        this.maxOrbs = 0;
        this.orbs.clear();
        this.increaseMaxOrbSlots(this.masterMaxOrbs, false);
        this.isBloodied = this.currentHealth <= this.maxHealth / 2;
        poisonKillCount = 0;
        GameActionManager.playerHpLastTurn = this.currentHealth;
        this.endTurnQueued = false;
        this.gameHandSize = this.masterHandSize;
        this.isDraggingCard = false;
        this.isHoveringDropZone = false;
        this.hoveredCard = null;
        this.cardInUse = null;
        this.drawPile.initializeDeck(this.masterDeck);
        AbstractDungeon.overlayMenu.endTurnButton.enabled = false;
        this.hand.clear();
        this.discardPile.clear();
        this.exhaustPile.clear();
        if (AbstractDungeon.player.hasRelic("SlaversCollar")) {
            ((SlaversCollar)AbstractDungeon.player.getRelic("SlaversCollar")).beforeEnergyPrep();
        }
        this.energy.prep();
        this.powers.clear();
        this.isEndingTurn = false;
        this.healthBarUpdatedEvent();
        if (ModHelper.isModEnabled("Lethality")) {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new StrengthPower(this, 3), 3));
        }
        if (ModHelper.isModEnabled("Terminal")) {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new PlatedArmorPower(this, 5), 5));
        }
        AbstractDungeon.getCurrRoom().monsters.usePreBattleAction();
        if (Settings.isFinalActAvailable && AbstractDungeon.getCurrMapNode().hasEmeraldKey) {
            AbstractDungeon.getCurrRoom().applyEmeraldEliteBuff();
        }
        AbstractDungeon.actionManager.addToTop(new WaitAction(1.0f));
        this.applyPreCombatLogic();
    }

    public ArrayList<String> getRelicNames() {
        ArrayList<String> arr = new ArrayList<String>();
        for (AbstractRelic relic : this.relics) {
            arr.add(relic.relicId);
        }
        return arr;
    }

    public int getCircletCount() {
        int count = 0;
        int counterSum = 0;
        for (AbstractRelic relic : this.relics) {
            if (!relic.relicId.equals("Circlet")) continue;
            ++count;
            counterSum += relic.counter;
        }
        if (counterSum > 0) {
            return counterSum;
        }
        return count;
    }

    public void draw(int numCards) {
        for (int i = 0; i < numCards; ++i) {
            if (!this.drawPile.isEmpty()) {
                AbstractCard c = this.drawPile.getTopCard();
                c.current_x = CardGroup.DRAW_PILE_X;
                c.current_y = CardGroup.DRAW_PILE_Y;
                c.setAngle(0.0f, true);
                c.lighten(false);
                c.drawScale = 0.12f;
                c.targetDrawScale = 0.75f;
                c.triggerWhenDrawn();
                this.hand.addToHand(c);
                this.drawPile.removeTopCard();
                for (AbstractPower p : this.powers) {
                    p.onCardDraw(c);
                }
                for (AbstractRelic r : this.relics) {
                    r.onCardDraw(c);
                }
                continue;
            }
            logger.info("ERROR: How did this happen? No cards in draw pile?? Player.java");
        }
    }

    public void draw() {
        if (this.hand.size() == 10) {
            this.createHandIsFullDialog();
            return;
        }
        CardCrawlGame.sound.playAV("CARD_DRAW_8", -0.12f, 0.25f);
        this.draw(1);
        this.onCardDrawOrDiscard();
    }

    @Override
    public void render(SpriteBatch sb) {
        this.stance.render(sb);
        if ((AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT || AbstractDungeon.getCurrRoom() instanceof MonsterRoom) && !this.isDead) {
            this.renderHealth(sb);
            if (!this.orbs.isEmpty()) {
                for (AbstractOrb o : this.orbs) {
                    o.render(sb);
                }
            }
        }
        if (!(AbstractDungeon.getCurrRoom() instanceof RestRoom)) {
            if (this.atlas == null || this.renderCorpse) {
                sb.setColor(Color.WHITE);
                sb.draw(this.img, this.drawX - (float)this.img.getWidth() * Settings.scale / 2.0f + this.animX, this.drawY, (float)this.img.getWidth() * Settings.scale, (float)this.img.getHeight() * Settings.scale, 0, 0, this.img.getWidth(), this.img.getHeight(), this.flipHorizontal, this.flipVertical);
            } else {
                this.renderPlayerImage(sb);
            }
            this.hb.render(sb);
            this.healthHb.render(sb);
        } else {
            sb.setColor(Color.WHITE);
            this.renderShoulderImg(sb);
        }
    }

    public void renderShoulderImg(SpriteBatch sb) {
        if (CampfireUI.hidden) {
            sb.draw(this.shoulder2Img, 0.0f, 0.0f, 1920.0f * Settings.scale, 1136.0f * Settings.scale);
        } else {
            sb.draw(this.shoulderImg, this.animX, 0.0f, 1920.0f * Settings.scale, 1136.0f * Settings.scale);
        }
    }

    public void renderPlayerImage(SpriteBatch sb) {
        if (this.atlas != null) {
            this.state.update(Gdx.graphics.getDeltaTime());
            this.state.apply(this.skeleton);
            this.skeleton.updateWorldTransform();
            this.skeleton.setPosition(this.drawX + this.animX, this.drawY + this.animY);
            this.skeleton.setColor(this.tint.color);
            this.skeleton.setFlip(this.flipHorizontal, this.flipVertical);
            sb.end();
            CardCrawlGame.psb.begin();
            sr.draw(CardCrawlGame.psb, this.skeleton);
            CardCrawlGame.psb.end();
            sb.begin();
        } else {
            sb.setColor(Color.WHITE);
            sb.draw(this.img, this.drawX - (float)this.img.getWidth() * Settings.scale / 2.0f + this.animX, this.drawY, (float)this.img.getWidth() * Settings.scale, (float)this.img.getHeight() * Settings.scale, 0, 0, this.img.getWidth(), this.img.getHeight(), this.flipHorizontal, this.flipVertical);
        }
    }

    public void renderPlayerBattleUi(SpriteBatch sb) {
        if ((this.hb.hovered || this.healthHb.hovered) && !AbstractDungeon.isScreenUp) {
            this.renderPowerTips(sb);
        }
    }

    @Override
    public void renderPowerTips(SpriteBatch sb) {
        ArrayList<PowerTip> tips = new ArrayList<PowerTip>();
        if (!this.stance.ID.equals("Neutral")) {
            tips.add(new PowerTip(this.stance.name, this.stance.description));
        }
        for (AbstractPower p : this.powers) {
            if (p.region48 != null) {
                tips.add(new PowerTip(p.name, p.description, p.region48));
                continue;
            }
            tips.add(new PowerTip(p.name, p.description, p.img));
        }
        if (!tips.isEmpty()) {
            if (this.hb.cX + this.hb.width / 2.0f < TIP_X_THRESHOLD) {
                TipHelper.queuePowerTips(this.hb.cX + this.hb.width / 2.0f + TIP_OFFSET_R_X, this.hb.cY + TipHelper.calculateAdditionalOffset(tips, this.hb.cY), tips);
            } else {
                TipHelper.queuePowerTips(this.hb.cX - this.hb.width / 2.0f + TIP_OFFSET_L_X, this.hb.cY + TipHelper.calculateAdditionalOffset(tips, this.hb.cY), tips);
            }
        }
    }

    public void renderHand(SpriteBatch sb) {
        if (Settings.SHOW_CARD_HOTKEYS) {
            this.renderCardHotKeyText(sb);
        }
        if (this.inspectMode && this.inspectHb != null) {
            this.renderReticle(sb, this.inspectHb);
        }
        if (this.hoveredCard != null) {
            int aliveMonsters = 0;
            this.hand.renderHand(sb, this.hoveredCard);
            this.hoveredCard.renderHoverShadow(sb);
            if ((this.isDraggingCard || this.inSingleTargetMode) && this.isHoveringDropZone) {
                if (this.isDraggingCard && !this.inSingleTargetMode) {
                    AbstractMonster theMonster = null;
                    for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                        if (m.isDying || m.currentHealth <= 0) continue;
                        ++aliveMonsters;
                        theMonster = m;
                    }
                    if (aliveMonsters == 1 && this.hoveredMonster == null) {
                        this.hoveredCard.calculateCardDamage(theMonster);
                        this.hoveredCard.render(sb);
                        this.hoveredCard.applyPowers();
                    } else {
                        this.hoveredCard.render(sb);
                    }
                }
                if (!AbstractDungeon.getCurrRoom().isBattleEnding()) {
                    this.renderHoverReticle(sb);
                }
            }
            if (this.hoveredMonster != null) {
                this.hoveredCard.calculateCardDamage(this.hoveredMonster);
                this.hoveredCard.render(sb);
                this.hoveredCard.applyPowers();
            } else if (aliveMonsters != 1) {
                this.hoveredCard.render(sb);
            }
        } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.HAND_SELECT) {
            this.hand.render(sb);
        } else {
            this.hand.renderHand(sb, this.cardInUse);
        }
        if (this.cardInUse != null && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.HAND_SELECT && !PeekButton.isPeeking) {
            this.cardInUse.render(sb);
            if (AbstractDungeon.getCurrRoom().phase != AbstractRoom.RoomPhase.COMBAT) {
                AbstractDungeon.effectList.add(new CardDisappearEffect(this.cardInUse.makeCopy(), this.cardInUse.current_x, this.cardInUse.current_y));
                this.cardInUse = null;
            }
        }
        this.limbo.render(sb);
        if (this.inSingleTargetMode && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT && !AbstractDungeon.getCurrRoom().isBattleEnding()) {
            this.renderTargetingUi(sb);
        }
    }

    private void renderTargetingUi(SpriteBatch sb) {
        this.arrowX = MathHelper.mouseLerpSnap(this.arrowX, InputHelper.mX);
        this.arrowY = MathHelper.mouseLerpSnap(this.arrowY, InputHelper.mY);
        this.controlPoint.x = this.hoveredCard.current_x - (this.arrowX - this.hoveredCard.current_x) / 4.0f;
        this.controlPoint.y = this.arrowY + (this.arrowY - this.hoveredCard.current_y) / 2.0f;
        if (this.hoveredMonster == null) {
            this.arrowScale = Settings.scale;
            this.arrowScaleTimer = 0.0f;
            sb.setColor(Color.WHITE);
        } else {
            this.arrowScaleTimer += Gdx.graphics.getDeltaTime();
            if (this.arrowScaleTimer > 1.0f) {
                this.arrowScaleTimer = 1.0f;
            }
            this.arrowScale = Interpolation.elasticOut.apply(Settings.scale, Settings.scale * 1.2f, this.arrowScaleTimer);
            sb.setColor(ARROW_COLOR);
        }
        this.arrowTmp.x = this.controlPoint.x - this.arrowX;
        this.arrowTmp.y = this.controlPoint.y - this.arrowY;
        this.arrowTmp.nor();
        this.startArrowVector.x = this.hoveredCard.current_x;
        this.startArrowVector.y = this.hoveredCard.current_y;
        this.endArrowVector.x = this.arrowX;
        this.endArrowVector.y = this.arrowY;
        this.drawCurvedLine(sb, this.startArrowVector, this.endArrowVector, this.controlPoint);
        sb.draw(ImageMaster.TARGET_UI_ARROW, this.arrowX - 128.0f, this.arrowY - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, this.arrowScale, this.arrowScale, this.arrowTmp.angle() + 90.0f, 0, 0, 256, 256, false, false);
    }

    private void drawCurvedLine(SpriteBatch sb, Vector2 start, Vector2 end, Vector2 control) {
        float radius = 7.0f * Settings.scale;
        for (int i = 0; i < this.points.length - 1; ++i) {
            this.points[i] = Bezier.quadratic(this.points[i], (float)i / 20.0f, start, control, end, this.arrowTmp);
            radius += 0.4f * Settings.scale;
            if (i != 0) {
                this.arrowTmp.x = this.points[i - 1].x - this.points[i].x;
                this.arrowTmp.y = this.points[i - 1].y - this.points[i].y;
                sb.draw(ImageMaster.TARGET_UI_CIRCLE, this.points[i].x - 64.0f, this.points[i].y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, radius / 18.0f, radius / 18.0f, this.arrowTmp.nor().angle() + 90.0f, 0, 0, 128, 128, false, false);
                continue;
            }
            this.arrowTmp.x = this.controlPoint.x - this.points[i].x;
            this.arrowTmp.y = this.controlPoint.y - this.points[i].y;
            sb.draw(ImageMaster.TARGET_UI_CIRCLE, this.points[i].x - 64.0f, this.points[i].y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, radius / 18.0f, radius / 18.0f, this.arrowTmp.nor().angle() + 270.0f, 0, 0, 128, 128, false, false);
        }
    }

    public void createHandIsFullDialog() {
        AbstractDungeon.effectList.add(new ThoughtBubble(this.dialogX, this.dialogY, 3.0f, MSG[2], true));
    }

    private void renderHoverReticle(SpriteBatch sb) {
        switch (this.hoveredCard.target) {
            case ENEMY: {
                if (!this.inSingleTargetMode || this.hoveredMonster == null) break;
                this.hoveredMonster.renderReticle(sb);
                break;
            }
            case ALL_ENEMY: {
                AbstractDungeon.getCurrRoom().monsters.renderReticle(sb);
                break;
            }
            case SELF: {
                this.renderReticle(sb);
                break;
            }
            case SELF_AND_ENEMY: {
                this.renderReticle(sb);
                if (!this.inSingleTargetMode || this.hoveredMonster == null) break;
                this.hoveredMonster.renderReticle(sb);
                break;
            }
            case ALL: {
                this.renderReticle(sb);
                AbstractDungeon.getCurrRoom().monsters.renderReticle(sb);
                break;
            }
            case NONE: {
                break;
            }
        }
    }

    public void applyPreCombatLogic() {
        for (AbstractRelic r : this.relics) {
            if (r == null) continue;
            r.atPreBattle();
        }
    }

    public void applyStartOfCombatLogic() {
        for (AbstractRelic r : this.relics) {
            if (r == null) continue;
            r.atBattleStart();
        }
        for (AbstractBlight b : this.blights) {
            if (b == null) continue;
            b.atBattleStart();
        }
    }

    public void applyStartOfCombatPreDrawLogic() {
        for (AbstractRelic r : this.relics) {
            if (r == null) continue;
            r.atBattleStartPreDraw();
        }
    }

    public void applyStartOfTurnRelics() {
        this.stance.atStartOfTurn();
        for (AbstractRelic r : this.relics) {
            if (r == null) continue;
            r.atTurnStart();
        }
        for (AbstractBlight b : this.blights) {
            if (b == null) continue;
            b.atTurnStart();
        }
    }

    public void applyStartOfTurnPostDrawRelics() {
        for (AbstractRelic r : this.relics) {
            if (r == null) continue;
            r.atTurnStartPostDraw();
        }
    }

    public void applyStartOfTurnPreDrawCards() {
        for (AbstractCard c : this.hand.group) {
            if (c == null) continue;
            c.atTurnStartPreDraw();
        }
    }

    public void applyStartOfTurnCards() {
        for (AbstractCard c : this.drawPile.group) {
            if (c == null) continue;
            c.atTurnStart();
        }
        for (AbstractCard c : this.hand.group) {
            if (c == null) continue;
            c.atTurnStart();
        }
        for (AbstractCard c : this.discardPile.group) {
            if (c == null) continue;
            c.atTurnStart();
        }
    }

    public void onVictory() {
        if (!this.isDying) {
            for (AbstractRelic r : this.relics) {
                r.onVictory();
            }
            for (AbstractBlight b : this.blights) {
                b.onVictory();
            }
            for (AbstractPower p : this.powers) {
                p.onVictory();
            }
        }
        this.damagedThisCombat = 0;
    }

    public boolean hasRelic(String targetID) {
        for (AbstractRelic r : this.relics) {
            if (!r.relicId.equals(targetID)) continue;
            return true;
        }
        return false;
    }

    public boolean hasBlight(String targetID) {
        for (AbstractBlight b : this.blights) {
            if (!b.blightID.equals(targetID)) continue;
            return true;
        }
        return false;
    }

    public boolean hasPotion(String id) {
        for (AbstractPotion p : this.potions) {
            if (!p.ID.equals(id)) continue;
            return true;
        }
        return false;
    }

    public boolean hasAnyPotions() {
        for (AbstractPotion p : this.potions) {
            if (p instanceof PotionSlot) continue;
            return true;
        }
        return false;
    }

    public void loseRandomRelics(int amount) {
        if (amount > this.relics.size()) {
            for (AbstractRelic r : this.relics) {
                r.onUnequip();
            }
            this.relics.clear();
            return;
        }
        for (int i = 0; i < amount; ++i) {
            int index = MathUtils.random(0, this.relics.size() - 1);
            this.relics.get(index).onUnequip();
            this.relics.remove(index);
        }
        this.reorganizeRelics();
    }

    public boolean loseRelic(String targetID) {
        if (!this.hasRelic(targetID)) {
            return false;
        }
        AbstractRelic toRemove = null;
        for (AbstractRelic r : this.relics) {
            if (!r.relicId.equals(targetID)) continue;
            r.onUnequip();
            toRemove = r;
        }
        if (toRemove == null) {
            logger.info("WHY WAS RELIC: " + this.name + " NOT FOUND???");
            return false;
        }
        this.relics.remove(toRemove);
        this.reorganizeRelics();
        return true;
    }

    public void reorganizeRelics() {
        logger.info("Reorganizing relics");
        ArrayList<AbstractRelic> tmpRelics = new ArrayList<AbstractRelic>();
        tmpRelics.addAll(this.relics);
        this.relics.clear();
        for (int i = 0; i < tmpRelics.size(); ++i) {
            ((AbstractRelic)tmpRelics.get(i)).reorganizeObtain(this, i, false, tmpRelics.size());
        }
    }

    public AbstractRelic getRelic(String targetID) {
        for (AbstractRelic r : this.relics) {
            if (!r.relicId.equals(targetID)) continue;
            return r;
        }
        return null;
    }

    public AbstractBlight getBlight(String targetID) {
        for (AbstractBlight b : this.blights) {
            if (!b.blightID.equals(targetID)) continue;
            return b;
        }
        return null;
    }

    public void obtainPotion(int slot, AbstractPotion potionToObtain) {
        if (slot > this.potionSlots) {
            return;
        }
        this.potions.set(slot, potionToObtain);
        potionToObtain.setAsObtained(slot);
    }

    public boolean obtainPotion(AbstractPotion potionToObtain) {
        int index = 0;
        for (AbstractPotion p : this.potions) {
            if (p instanceof PotionSlot) break;
            ++index;
        }
        if (index < this.potionSlots) {
            this.potions.set(index, potionToObtain);
            potionToObtain.setAsObtained(index);
            potionToObtain.flash();
            AbstractPotion.playPotionSound();
            return true;
        }
        logger.info("NOT ENOUGH POTION SLOTS");
        AbstractDungeon.topPanel.flashRed();
        return false;
    }

    public void renderRelics(SpriteBatch sb) {
        for (int i = 0; i < this.relics.size(); ++i) {
            if (i / AbstractRelic.MAX_RELICS_PER_PAGE != AbstractRelic.relicPage) continue;
            this.relics.get(i).renderInTopPanel(sb);
        }
        for (AbstractRelic r : this.relics) {
            if (!r.hb.hovered) continue;
            r.renderTip(sb);
        }
    }

    public void renderBlights(SpriteBatch sb) {
        for (AbstractBlight b : this.blights) {
            b.renderInTopPanel(sb);
        }
        for (AbstractBlight b : this.blights) {
            if (!b.hb.hovered) continue;
            b.renderTip(sb);
        }
    }

    public void bottledCardUpgradeCheck(AbstractCard c) {
        if (c.inBottleFlame && this.hasRelic("Bottled Flame")) {
            ((BottledFlame)this.getRelic("Bottled Flame")).setDescriptionAfterLoading();
        }
        if (c.inBottleLightning && this.hasRelic("Bottled Lightning")) {
            ((BottledLightning)this.getRelic("Bottled Lightning")).setDescriptionAfterLoading();
        }
        if (c.inBottleTornado && this.hasRelic("Bottled Tornado")) {
            ((BottledTornado)this.getRelic("Bottled Tornado")).setDescriptionAfterLoading();
        }
    }

    public void triggerEvokeAnimation(int slot) {
        if (this.maxOrbs <= 0) {
            return;
        }
        this.orbs.get(slot).triggerEvokeAnimation();
    }

    public void evokeOrb() {
        if (!this.orbs.isEmpty() && !(this.orbs.get(0) instanceof EmptyOrbSlot)) {
            int i;
            this.orbs.get(0).onEvoke();
            EmptyOrbSlot orbSlot = new EmptyOrbSlot();
            for (i = 1; i < this.orbs.size(); ++i) {
                Collections.swap(this.orbs, i, i - 1);
            }
            this.orbs.set(this.orbs.size() - 1, orbSlot);
            for (i = 0; i < this.orbs.size(); ++i) {
                this.orbs.get(i).setSlot(i, this.maxOrbs);
            }
        }
    }

    public void evokeNewestOrb() {
        if (!this.orbs.isEmpty() && !(this.orbs.get(this.orbs.size() - 1) instanceof EmptyOrbSlot)) {
            this.orbs.get(this.orbs.size() - 1).onEvoke();
        }
    }

    public void evokeWithoutLosingOrb() {
        if (!this.orbs.isEmpty() && !(this.orbs.get(0) instanceof EmptyOrbSlot)) {
            this.orbs.get(0).onEvoke();
        }
    }

    public void removeNextOrb() {
        if (!this.orbs.isEmpty() && !(this.orbs.get(0) instanceof EmptyOrbSlot)) {
            int i;
            EmptyOrbSlot orbSlot = new EmptyOrbSlot(this.orbs.get((int)0).cX, this.orbs.get((int)0).cY);
            for (i = 1; i < this.orbs.size(); ++i) {
                Collections.swap(this.orbs, i, i - 1);
            }
            this.orbs.set(this.orbs.size() - 1, orbSlot);
            for (i = 0; i < this.orbs.size(); ++i) {
                this.orbs.get(i).setSlot(i, this.maxOrbs);
            }
        }
    }

    public boolean hasEmptyOrb() {
        if (this.orbs.isEmpty()) {
            return false;
        }
        for (AbstractOrb o : this.orbs) {
            if (!(o instanceof EmptyOrbSlot)) continue;
            return true;
        }
        return false;
    }

    public boolean hasOrb() {
        if (this.orbs.isEmpty()) {
            return false;
        }
        return !(this.orbs.get(0) instanceof EmptyOrbSlot);
    }

    public int filledOrbCount() {
        int orbCount = 0;
        for (AbstractOrb o : this.orbs) {
            if (o instanceof EmptyOrbSlot) continue;
            ++orbCount;
        }
        return orbCount;
    }

    public void channelOrb(AbstractOrb orbToSet) {
        if (this.maxOrbs <= 0) {
            AbstractDungeon.effectList.add(new ThoughtBubble(this.dialogX, this.dialogY, 3.0f, MSG[4], true));
            return;
        }
        if (this.maxOrbs > 0) {
            if (this.hasRelic("Dark Core") && !(orbToSet instanceof Dark)) {
                orbToSet = new Dark();
            }
            int index = -1;
            for (int i = 0; i < this.orbs.size(); ++i) {
                if (!(this.orbs.get(i) instanceof EmptyOrbSlot)) continue;
                index = i;
                break;
            }
            if (index != -1) {
                orbToSet.cX = this.orbs.get((int)index).cX;
                orbToSet.cY = this.orbs.get((int)index).cY;
                this.orbs.set(index, orbToSet);
                this.orbs.get(index).setSlot(index, this.maxOrbs);
                orbToSet.playChannelSFX();
                for (AbstractPower p : this.powers) {
                    p.onChannel(orbToSet);
                }
                AbstractDungeon.actionManager.orbsChanneledThisCombat.add(orbToSet);
                AbstractDungeon.actionManager.orbsChanneledThisTurn.add(orbToSet);
                int plasmaCount = 0;
                for (AbstractOrb o : AbstractDungeon.actionManager.orbsChanneledThisTurn) {
                    if (!(o instanceof Plasma)) continue;
                    ++plasmaCount;
                }
                if (plasmaCount == 9) {
                    UnlockTracker.unlockAchievement("NEON");
                }
                orbToSet.applyFocus();
            } else {
                AbstractDungeon.actionManager.addToTop(new ChannelAction(orbToSet));
                AbstractDungeon.actionManager.addToTop(new EvokeOrbAction(1));
                AbstractDungeon.actionManager.addToTop(new AnimateOrbAction(1));
            }
        }
    }

    public void increaseMaxOrbSlots(int amount, boolean playSfx) {
        int i;
        if (this.maxOrbs == 10) {
            AbstractDungeon.effectList.add(new ThoughtBubble(this.dialogX, this.dialogY, 3.0f, MSG[3], true));
            return;
        }
        if (playSfx) {
            CardCrawlGame.sound.play("ORB_SLOT_GAIN", 0.1f);
        }
        this.maxOrbs += amount;
        for (i = 0; i < amount; ++i) {
            this.orbs.add(new EmptyOrbSlot());
        }
        for (i = 0; i < this.orbs.size(); ++i) {
            this.orbs.get(i).setSlot(i, this.maxOrbs);
        }
    }

    public void decreaseMaxOrbSlots(int amount) {
        if (this.maxOrbs <= 0) {
            return;
        }
        this.maxOrbs -= amount;
        if (this.maxOrbs < 0) {
            this.maxOrbs = 0;
        }
        if (!this.orbs.isEmpty()) {
            this.orbs.remove(this.orbs.size() - 1);
        }
        for (int i = 0; i < this.orbs.size(); ++i) {
            this.orbs.get(i).setSlot(i, this.maxOrbs);
        }
    }

    public void applyStartOfTurnOrbs() {
        if (!this.orbs.isEmpty()) {
            for (AbstractOrb o : this.orbs) {
                o.onStartOfTurn();
            }
            if (this.hasRelic("Cables") && !(this.orbs.get(0) instanceof EmptyOrbSlot)) {
                this.orbs.get(0).onStartOfTurn();
            }
        }
    }

    private void updateEscapeAnimation() {
        if (this.escapeTimer != 0.0f) {
            this.escapeTimer -= Gdx.graphics.getDeltaTime();
            this.drawX = this.flipHorizontal ? (this.drawX -= Gdx.graphics.getDeltaTime() * 400.0f * Settings.scale) : (this.drawX += Gdx.graphics.getDeltaTime() * 500.0f * Settings.scale);
        }
        if (this.escapeTimer < 0.0f) {
            AbstractDungeon.getCurrRoom().endBattle();
            this.flipHorizontal = false;
            this.isEscaping = false;
            this.escapeTimer = 0.0f;
        }
    }

    public boolean relicsDoneAnimating() {
        for (AbstractRelic r : this.relics) {
            if (r.isDone) continue;
            return false;
        }
        return true;
    }

    public void resetControllerValues() {
        if (Settings.isControllerMode) {
            this.toHover = null;
            this.hoveredCard = null;
            this.inspectMode = false;
            this.inspectHb = null;
            this.keyboardCardIndex = -1;
            this.hand.refreshHandLayout();
        }
    }

    public AbstractPotion getRandomPotion() {
        ArrayList<AbstractPotion> list = new ArrayList<AbstractPotion>();
        for (AbstractPotion p : this.potions) {
            if (p instanceof PotionSlot) continue;
            list.add(p);
        }
        if (list.isEmpty()) {
            return null;
        }
        Collections.shuffle(list, new Random(AbstractDungeon.miscRng.randomLong()));
        return (AbstractPotion)list.get(0);
    }

    public void removePotion(AbstractPotion potionOption) {
        int slot = this.potions.indexOf(potionOption);
        if (slot >= 0) {
            this.potions.set(slot, new PotionSlot(slot));
        }
    }

    public void movePosition(float x, float y) {
        this.drawX = x;
        this.drawY = y;
        this.dialogX = this.drawX + 0.0f * Settings.scale;
        this.dialogY = this.drawY + 170.0f * Settings.scale;
        this.animX = 0.0f;
        this.animY = 0.0f;
        this.refreshHitboxLocation();
    }

    public void switchedStance() {
        for (AbstractCard c : this.hand.group) {
            c.switchedStance();
        }
        for (AbstractCard c : this.discardPile.group) {
            c.switchedStance();
        }
        for (AbstractCard c : this.drawPile.group) {
            c.switchedStance();
        }
    }

    public CharacterOption getCharacterSelectOption() {
        return null;
    }

    public void onStanceChange(String id) {
    }

    public static enum PlayerClass {
        IRONCLAD,
        THE_SILENT,
        DEFECT,
        WATCHER;

    }

    private static enum RHoverType {
        RELIC,
        BLIGHT;

    }
}


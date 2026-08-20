/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.GameDataStringBuilder;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.ShaderHelper;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.TipTracker;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.ShopRoom;
import com.megacrit.cardcrawl.rooms.TreasureRoomBoss;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.stances.AbstractStance;
import com.megacrit.cardcrawl.ui.FtueTip;
import com.megacrit.cardcrawl.ui.campfire.AbstractCampfireOption;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.FloatyEffect;
import com.megacrit.cardcrawl.vfx.GlowRelicParticle;
import com.megacrit.cardcrawl.vfx.SmokePuffEffect;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public abstract class AbstractRelic
implements Comparable<AbstractRelic> {
    private static final TutorialStrings tutorialStrings = CardCrawlGame.languagePack.getTutorialString("Relic Tip");
    public static final String[] MSG = AbstractRelic.tutorialStrings.TEXT;
    public static final String[] LABEL = AbstractRelic.tutorialStrings.LABEL;
    public static final String USED_UP_MSG = MSG[2];
    public final String name;
    public final String relicId;
    private final RelicStrings relicStrings;
    public final String[] DESCRIPTIONS;
    public boolean energyBased = false;
    public boolean usedUp = false;
    public boolean grayscale = false;
    public String description;
    public String flavorText = "missing";
    public int cost;
    public int counter = -1;
    public RelicTier tier;
    public ArrayList<PowerTip> tips = new ArrayList();
    public Texture img;
    public Texture largeImg;
    public Texture outlineImg;
    public static final String IMG_DIR = "images/relics/";
    public static final String OUTLINE_DIR = "images/relics/outline/";
    private static final String L_IMG_DIR = "images/largeRelics/";
    public String imgUrl = "";
    public static final int RAW_W = 128;
    public static int relicPage = 0;
    private static float offsetX = 0.0f;
    public static final int MAX_RELICS_PER_PAGE = (int)((float)Settings.WIDTH / (75.0f * Settings.scale));
    public float currentX;
    public float currentY;
    public float targetX;
    public float targetY;
    private static final float START_X = 64.0f * Settings.scale;
    private static final float START_Y = Settings.isMobile ? (float)Settings.HEIGHT - 132.0f * Settings.scale : (float)Settings.HEIGHT - 102.0f * Settings.scale;
    public static final float PAD_X = 72.0f * Settings.scale;
    public static final Color PASSIVE_OUTLINE_COLOR = new Color(0.0f, 0.0f, 0.0f, 0.33f);
    private Color flashColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    private Color goldOutlineColor = new Color(1.0f, 0.9f, 0.4f, 0.0f);
    public boolean isSeen = false;
    public float scale = Settings.scale;
    protected boolean pulse = false;
    private float animationTimer;
    private float glowTimer = 0.0f;
    public float flashTimer = 0.0f;
    private static final float FLASH_ANIM_TIME = 2.0f;
    private static final float DEFAULT_ANIM_SCALE = 4.0f;
    private FloatyEffect f_effect = new FloatyEffect(10.0f, 0.2f);
    public boolean isDone = false;
    public boolean isAnimating = false;
    public boolean isObtained = false;
    private LandingSound landingSFX = LandingSound.CLINK;
    public Hitbox hb = new Hitbox(PAD_X, PAD_X);
    private static final float OBTAIN_SPEED = 6.0f;
    private static final float OBTAIN_THRESHOLD = 0.5f;
    private float rotation = 0.0f;
    public boolean discarded = false;
    private String assetURL;

    public AbstractRelic(String setId, String imgName, RelicTier tier, LandingSound sfx) {
        this.relicId = setId;
        this.relicStrings = CardCrawlGame.languagePack.getRelicStrings(this.relicId);
        this.DESCRIPTIONS = this.relicStrings.DESCRIPTIONS;
        this.imgUrl = imgName;
        ImageMaster.loadRelicImg(setId, imgName);
        this.img = ImageMaster.getRelicImg(setId);
        this.outlineImg = ImageMaster.getRelicOutlineImg(setId);
        this.name = this.relicStrings.NAME;
        this.description = this.getUpdatedDescription();
        this.flavorText = this.relicStrings.FLAVOR;
        this.tier = tier;
        this.landingSFX = sfx;
        this.assetURL = IMG_DIR + imgName;
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    public void usedUp() {
        this.grayscale = true;
        this.usedUp = true;
        this.description = MSG[2];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    public void spawn(float x, float y) {
        if (!(AbstractDungeon.getCurrRoom() instanceof ShopRoom)) {
            AbstractDungeon.effectsQueue.add(new SmokePuffEffect(x, y));
        }
        this.currentX = x;
        this.currentY = y;
        this.isAnimating = true;
        this.isObtained = false;
        if (this.tier == RelicTier.BOSS) {
            this.f_effect.x = 0.0f;
            this.f_effect.y = 0.0f;
            this.targetX = x;
            this.targetY = y;
            this.glowTimer = 0.0f;
        } else {
            this.f_effect.x = 0.0f;
            this.f_effect.y = 0.0f;
        }
    }

    public int getPrice() {
        switch (this.tier) {
            case STARTER: {
                return 300;
            }
            case COMMON: {
                return 150;
            }
            case UNCOMMON: {
                return 250;
            }
            case RARE: {
                return 300;
            }
            case SHOP: {
                return 150;
            }
            case SPECIAL: {
                return 400;
            }
            case BOSS: {
                return 999;
            }
            case DEPRECATED: {
                return -1;
            }
        }
        return -1;
    }

    public void reorganizeObtain(AbstractPlayer p, int slot, boolean callOnEquip, int relicAmount) {
        this.isDone = true;
        this.isObtained = true;
        p.relics.add(this);
        this.currentX = START_X + (float)slot * PAD_X;
        this.currentY = START_Y;
        this.targetX = this.currentX;
        this.targetY = this.currentY;
        this.hb.move(this.currentX, this.currentY);
        if (callOnEquip) {
            this.onEquip();
            this.relicTip();
        }
        UnlockTracker.markRelicAsSeen(this.relicId);
    }

    public void instantObtain(AbstractPlayer p, int slot, boolean callOnEquip) {
        if (this.relicId.equals("Circlet") && p != null && p.hasRelic("Circlet")) {
            AbstractRelic circ = p.getRelic("Circlet");
            ++circ.counter;
            circ.flash();
            this.isDone = true;
            this.isObtained = true;
            this.discarded = true;
        } else {
            this.isDone = true;
            this.isObtained = true;
            if (slot >= p.relics.size()) {
                p.relics.add(this);
            } else {
                p.relics.set(slot, this);
            }
            this.currentX = START_X + (float)slot * PAD_X;
            this.currentY = START_Y;
            this.targetX = this.currentX;
            this.targetY = this.currentY;
            this.hb.move(this.currentX, this.currentY);
            if (callOnEquip) {
                this.onEquip();
                this.relicTip();
            }
            UnlockTracker.markRelicAsSeen(this.relicId);
            this.getUpdatedDescription();
            if (AbstractDungeon.topPanel != null) {
                AbstractDungeon.topPanel.adjustRelicHbs();
            }
        }
    }

    public void instantObtain() {
        if (this.relicId == "Circlet" && AbstractDungeon.player.hasRelic("Circlet")) {
            AbstractRelic circ = AbstractDungeon.player.getRelic("Circlet");
            ++circ.counter;
            circ.flash();
        } else {
            this.playLandingSFX();
            this.isDone = true;
            this.isObtained = true;
            this.currentX = START_X + (float)AbstractDungeon.player.relics.size() * PAD_X;
            this.currentY = START_Y;
            this.targetX = this.currentX;
            this.targetY = this.currentY;
            this.flash();
            AbstractDungeon.player.relics.add(this);
            this.hb.move(this.currentX, this.currentY);
            this.onEquip();
            this.relicTip();
            UnlockTracker.markRelicAsSeen(this.relicId);
        }
        if (AbstractDungeon.topPanel != null) {
            AbstractDungeon.topPanel.adjustRelicHbs();
        }
    }

    public void obtain() {
        if (this.relicId == "Circlet" && AbstractDungeon.player.hasRelic("Circlet")) {
            AbstractRelic circ = AbstractDungeon.player.getRelic("Circlet");
            ++circ.counter;
            circ.flash();
            this.hb.hovered = false;
        } else {
            this.hb.hovered = false;
            int row = AbstractDungeon.player.relics.size();
            this.targetX = START_X + (float)row * PAD_X;
            this.targetY = START_Y;
            AbstractDungeon.player.relics.add(this);
            this.relicTip();
            UnlockTracker.markRelicAsSeen(this.relicId);
        }
    }

    public int getColumn() {
        return AbstractDungeon.player.relics.indexOf(this);
    }

    public void relicTip() {
        if (TipTracker.relicCounter < 20 && ++TipTracker.relicCounter >= 1 && !TipTracker.tips.get("RELIC_TIP").booleanValue()) {
            AbstractDungeon.ftue = new FtueTip(LABEL[0], MSG[0], 360.0f * Settings.scale, 760.0f * Settings.scale, FtueTip.TipType.RELIC);
            TipTracker.neverShowAgain("RELIC_TIP");
        }
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public void update() {
        this.updateFlash();
        if (!this.isDone) {
            if (this.isAnimating) {
                this.glowTimer -= Gdx.graphics.getDeltaTime();
                if (this.glowTimer < 0.0f) {
                    this.glowTimer = 0.5f;
                    AbstractDungeon.effectList.add(new GlowRelicParticle(this.img, this.currentX + this.f_effect.x, this.currentY + this.f_effect.y, this.rotation));
                }
                this.f_effect.update();
                this.scale = this.hb.hovered ? Settings.scale * 1.5f : MathHelper.scaleLerpSnap(this.scale, Settings.scale * 1.1f);
            } else {
                this.scale = this.hb.hovered ? Settings.scale * 1.25f : MathHelper.scaleLerpSnap(this.scale, Settings.scale);
            }
            if (this.isObtained) {
                if (this.rotation != 0.0f) {
                    this.rotation = MathUtils.lerp(this.rotation, 0.0f, Gdx.graphics.getDeltaTime() * 6.0f * 2.0f);
                }
                if (this.currentX != this.targetX) {
                    this.currentX = MathUtils.lerp(this.currentX, this.targetX, Gdx.graphics.getDeltaTime() * 6.0f);
                    if (Math.abs(this.currentX - this.targetX) < 0.5f) {
                        this.currentX = this.targetX;
                    }
                }
                if (this.currentY != this.targetY) {
                    this.currentY = MathUtils.lerp(this.currentY, this.targetY, Gdx.graphics.getDeltaTime() * 6.0f);
                    if (Math.abs(this.currentY - this.targetY) < 0.5f) {
                        this.currentY = this.targetY;
                    }
                }
                if (this.currentY == this.targetY && this.currentX == this.targetX) {
                    this.isDone = true;
                    if (AbstractDungeon.topPanel != null) {
                        AbstractDungeon.topPanel.adjustRelicHbs();
                    }
                    this.hb.move(this.currentX, this.currentY);
                    if (this.tier == RelicTier.BOSS && AbstractDungeon.getCurrRoom() instanceof TreasureRoomBoss) {
                        AbstractDungeon.overlayMenu.proceedButton.show();
                    }
                    this.onEquip();
                }
                this.scale = Settings.scale;
            }
            if (this.hb != null) {
                this.hb.update();
                if (this.hb.hovered && (!AbstractDungeon.isScreenUp || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.NEOW_UNLOCK) {
                    if (InputHelper.justClickedLeft && !this.isObtained) {
                        InputHelper.justClickedLeft = false;
                        this.hb.clickStarted = true;
                    }
                    if ((this.hb.clicked || CInputActionSet.select.isJustPressed()) && !this.isObtained) {
                        CInputActionSet.select.unpress();
                        this.hb.clicked = false;
                        if (!Settings.isTouchScreen) {
                            this.bossObtainLogic();
                        } else {
                            AbstractDungeon.bossRelicScreen.confirmButton.show();
                            AbstractDungeon.bossRelicScreen.confirmButton.isDisabled = false;
                            AbstractDungeon.bossRelicScreen.touchRelic = this;
                        }
                    }
                }
            }
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) {
                this.updateAnimation();
            }
        } else {
            if (AbstractDungeon.player != null && AbstractDungeon.player.relics.indexOf(this) / MAX_RELICS_PER_PAGE == relicPage) {
                this.hb.update();
            } else {
                this.hb.hovered = false;
            }
            if (this.hb.hovered && AbstractDungeon.topPanel.potionUi.isHidden) {
                this.scale = Settings.scale * 1.25f;
                CardCrawlGame.cursor.changeType(GameCursor.CursorType.INSPECT);
            } else {
                this.scale = MathHelper.scaleLerpSnap(this.scale, Settings.scale);
            }
            this.updateRelicPopupClick();
        }
    }

    public void bossObtainLogic() {
        if (!(this.relicId.equals("HolyWater") || this.relicId.equals("Black Blood") || this.relicId.equals("Ring of the Serpent") || this.relicId.equals("FrozenCore"))) {
            this.obtain();
        }
        this.isObtained = true;
        this.f_effect.x = 0.0f;
        this.f_effect.y = 0.0f;
    }

    private void updateRelicPopupClick() {
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            this.hb.clickStarted = true;
        }
        if (this.hb.clicked || this.hb.hovered && CInputActionSet.select.isJustPressed()) {
            CardCrawlGame.relicPopup.open(this, AbstractDungeon.player.relics);
            CInputActionSet.select.unpress();
            this.hb.clicked = false;
            this.hb.clickStarted = false;
        }
    }

    public void updateDescription(AbstractPlayer.PlayerClass c) {
    }

    public String getUpdatedDescription() {
        return "";
    }

    public void playLandingSFX() {
        switch (this.landingSFX) {
            case CLINK: {
                CardCrawlGame.sound.play("RELIC_DROP_CLINK");
                break;
            }
            case FLAT: {
                CardCrawlGame.sound.play("RELIC_DROP_FLAT");
                break;
            }
            case SOLID: {
                CardCrawlGame.sound.play("RELIC_DROP_ROCKY");
                break;
            }
            case HEAVY: {
                CardCrawlGame.sound.play("RELIC_DROP_HEAVY");
                break;
            }
            case MAGICAL: {
                CardCrawlGame.sound.play("RELIC_DROP_MAGICAL");
                break;
            }
            default: {
                CardCrawlGame.sound.play("RELIC_DROP_CLINK");
            }
        }
    }

    protected void updateAnimation() {
        if (this.animationTimer != 0.0f) {
            this.animationTimer -= Gdx.graphics.getDeltaTime();
            if (this.animationTimer < 0.0f) {
                this.animationTimer = 0.0f;
            }
        }
    }

    private void updateFlash() {
        if (this.flashTimer != 0.0f) {
            this.flashTimer -= Gdx.graphics.getDeltaTime();
            if (this.flashTimer < 0.0f) {
                this.flashTimer = this.pulse ? 1.0f : 0.0f;
            }
        }
    }

    public void onEvokeOrb(AbstractOrb ammo) {
    }

    public void onPlayCard(AbstractCard c, AbstractMonster m) {
    }

    public void onPreviewObtainCard(AbstractCard c) {
    }

    public void onObtainCard(AbstractCard c) {
    }

    public void onGainGold() {
    }

    public void onLoseGold() {
    }

    public void onSpendGold() {
    }

    public void onEquip() {
    }

    public void onUnequip() {
    }

    public void atPreBattle() {
    }

    public void atBattleStart() {
    }

    public void onSpawnMonster(AbstractMonster monster) {
    }

    public void atBattleStartPreDraw() {
    }

    public void atTurnStart() {
    }

    public void atTurnStartPostDraw() {
    }

    public void onPlayerEndTurn() {
    }

    public void onBloodied() {
    }

    public void onNotBloodied() {
    }

    public void onManualDiscard() {
    }

    public void onUseCard(AbstractCard targetCard, UseCardAction useCardAction) {
    }

    public void onVictory() {
    }

    public void onMonsterDeath(AbstractMonster m) {
    }

    public void onBlockBroken(AbstractCreature m) {
    }

    public int onPlayerGainBlock(int blockAmount) {
        return blockAmount;
    }

    public int onPlayerGainedBlock(float blockAmount) {
        return MathUtils.floor(blockAmount);
    }

    public int onPlayerHeal(int healAmount) {
        return healAmount;
    }

    public void onMeditate() {
    }

    public void onEnergyRecharge() {
    }

    public void addCampfireOption(ArrayList<AbstractCampfireOption> options) {
    }

    public boolean canUseCampfireOption(AbstractCampfireOption option) {
        return true;
    }

    public void onRest() {
    }

    public void onRitual() {
    }

    public void onEnterRestRoom() {
    }

    public void onRefreshHand() {
    }

    public void onShuffle() {
    }

    public void onSmith() {
    }

    public void onAttack(DamageInfo info, int damageAmount, AbstractCreature target) {
    }

    public int onAttacked(DamageInfo info, int damageAmount) {
        return damageAmount;
    }

    public int onAttackedToChangeDamage(DamageInfo info, int damageAmount) {
        return damageAmount;
    }

    public int onAttackToChangeDamage(DamageInfo info, int damageAmount) {
        return damageAmount;
    }

    public void onExhaust(AbstractCard card) {
    }

    public void onTrigger() {
    }

    public void onTrigger(AbstractCreature target) {
    }

    public boolean checkTrigger() {
        return false;
    }

    public void onEnterRoom(AbstractRoom room) {
    }

    public void justEnteredRoom(AbstractRoom room) {
    }

    public void onCardDraw(AbstractCard drawnCard) {
    }

    public void onChestOpen(boolean bossChest) {
    }

    public void onChestOpenAfter(boolean bossChest) {
    }

    public void onDrawOrDiscard() {
    }

    public void onMasterDeckChange() {
    }

    public float atDamageModify(float damage, AbstractCard c) {
        return damage;
    }

    public int changeNumberOfCardsInReward(int numberOfCards) {
        return numberOfCards;
    }

    public int changeRareCardRewardChance(int rareCardChance) {
        return rareCardChance;
    }

    public int changeUncommonCardRewardChance(int uncommonCardChance) {
        return uncommonCardChance;
    }

    public void renderInTopPanel(SpriteBatch sb) {
        if (Settings.hideRelics) {
            return;
        }
        this.renderOutline(sb, true);
        if (this.grayscale) {
            ShaderHelper.setShader(sb, ShaderHelper.Shader.GRAYSCALE);
        }
        sb.setColor(Color.WHITE);
        sb.draw(this.img, this.currentX - 64.0f + offsetX, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
        if (this.grayscale) {
            ShaderHelper.setShader(sb, ShaderHelper.Shader.DEFAULT);
        }
        this.renderCounter(sb, true);
        this.renderFlash(sb, true);
        this.hb.render(sb);
    }

    public void render(SpriteBatch sb) {
        if (Settings.hideRelics) {
            return;
        }
        this.renderOutline(sb, false);
        if (!(this.isObtained || AbstractDungeon.isScreenUp && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.BOSS_REWARD && AbstractDungeon.screen != AbstractDungeon.CurrentScreen.SHOP)) {
            if (this.hb.hovered) {
                this.renderBossTip(sb);
            }
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) {
                if (this.hb.hovered) {
                    sb.setColor(PASSIVE_OUTLINE_COLOR);
                    sb.draw(this.outlineImg, this.currentX - 64.0f + this.f_effect.x, this.currentY - 64.0f + this.f_effect.y, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
                } else {
                    sb.setColor(PASSIVE_OUTLINE_COLOR);
                    sb.draw(this.outlineImg, this.currentX - 64.0f + this.f_effect.x, this.currentY - 64.0f + this.f_effect.y, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
                }
            }
        }
        if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) {
            if (!this.isObtained) {
                sb.setColor(Color.WHITE);
                sb.draw(this.img, this.currentX - 64.0f + this.f_effect.x, this.currentY - 64.0f + this.f_effect.y, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
            } else {
                sb.setColor(Color.WHITE);
                sb.draw(this.img, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
                this.renderCounter(sb, false);
            }
        } else {
            sb.setColor(Color.WHITE);
            sb.draw(this.img, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
            this.renderCounter(sb, false);
        }
        if (this.isDone) {
            this.renderFlash(sb, false);
        }
        this.hb.render(sb);
    }

    public void renderLock(SpriteBatch sb, Color outlineColor) {
        sb.setColor(outlineColor);
        sb.draw(ImageMaster.RELIC_LOCK_OUTLINE, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.RELIC_LOCK, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
        if (this.hb.hovered) {
            String unlockReq = UnlockTracker.unlockReqs.get(this.relicId);
            if (unlockReq == null) {
                unlockReq = "Missing unlock req.";
            }
            unlockReq = LABEL[2];
            if ((float)InputHelper.mX < 1400.0f * Settings.scale) {
                if (CardCrawlGame.mainMenuScreen.screen == MainMenuScreen.CurScreen.RELIC_VIEW && (float)InputHelper.mY < (float)Settings.HEIGHT / 5.0f) {
                    TipHelper.renderGenericTip((float)InputHelper.mX + 60.0f * Settings.scale, (float)InputHelper.mY + 100.0f * Settings.scale, LABEL[3], unlockReq);
                } else {
                    TipHelper.renderGenericTip((float)InputHelper.mX + 60.0f * Settings.scale, (float)InputHelper.mY - 50.0f * Settings.scale, LABEL[3], unlockReq);
                }
            } else {
                TipHelper.renderGenericTip((float)InputHelper.mX - 350.0f * Settings.scale, (float)InputHelper.mY - 50.0f * Settings.scale, LABEL[3], unlockReq);
            }
            float tmpX = this.currentX;
            float tmpY = this.currentY;
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) {
                tmpX += this.f_effect.x;
                tmpY += this.f_effect.y;
            }
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.RELIC_LOCK, tmpX - 64.0f, tmpY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
        }
        this.hb.render(sb);
    }

    public void render(SpriteBatch sb, boolean renderAmount, Color outlineColor) {
        if (this.isSeen) {
            this.renderOutline(outlineColor, sb, false);
        } else {
            this.renderOutline(Color.LIGHT_GRAY, sb, false);
        }
        if (this.isSeen) {
            sb.setColor(Color.WHITE);
        } else if (this.hb.hovered) {
            sb.setColor(Settings.HALF_TRANSPARENT_BLACK_COLOR);
        } else {
            sb.setColor(Color.BLACK);
        }
        if (AbstractDungeon.screen != null && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NEOW_UNLOCK) {
            if (this.largeImg == null) {
                sb.draw(this.img, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * 2.0f + MathUtils.cosDeg(System.currentTimeMillis() / 5L % 360L) / 15.0f, Settings.scale * 2.0f + MathUtils.cosDeg(System.currentTimeMillis() / 5L % 360L) / 15.0f, this.rotation, 0, 0, 128, 128, false, false);
            } else {
                sb.draw(this.largeImg, this.currentX - 128.0f, this.currentY - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale * 1.0f + MathUtils.cosDeg(System.currentTimeMillis() / 5L % 360L) / 30.0f, Settings.scale * 1.0f + MathUtils.cosDeg(System.currentTimeMillis() / 5L % 360L) / 30.0f, this.rotation, 0, 0, 256, 256, false, false);
            }
        } else {
            sb.draw(this.img, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
            if (this.relicId.equals("Circlet")) {
                this.renderCounter(sb, false);
            }
        }
        if (this.hb.hovered && !CardCrawlGame.relicPopup.isOpen) {
            if (!this.isSeen) {
                if ((float)InputHelper.mX < 1400.0f * Settings.scale) {
                    TipHelper.renderGenericTip((float)InputHelper.mX + 60.0f * Settings.scale, (float)InputHelper.mY - 50.0f * Settings.scale, LABEL[1], MSG[1]);
                } else {
                    TipHelper.renderGenericTip((float)InputHelper.mX - 350.0f * Settings.scale, (float)InputHelper.mY - 50.0f * Settings.scale, LABEL[1], MSG[1]);
                }
                return;
            }
            this.renderTip(sb);
        }
        this.hb.render(sb);
    }

    public void renderWithoutAmount(SpriteBatch sb, Color c) {
        this.renderOutline(c, sb, false);
        sb.setColor(Color.WHITE);
        sb.draw(this.img, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
        if (this.hb.hovered) {
            this.renderTip(sb);
            float tmpX = this.currentX;
            float tmpY = this.currentY;
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.BOSS_REWARD) {
                tmpX += this.f_effect.x;
                tmpY += this.f_effect.y;
            }
            sb.setColor(Color.WHITE);
            sb.draw(this.img, tmpX - 64.0f, tmpY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
        }
        this.hb.render(sb);
    }

    public void renderCounter(SpriteBatch sb, boolean inTopPanel) {
        if (this.counter > -1) {
            if (inTopPanel) {
                FontHelper.renderFontRightTopAligned(sb, FontHelper.topPanelInfoFont, Integer.toString(this.counter), offsetX + this.currentX + 30.0f * Settings.scale, this.currentY - 7.0f * Settings.scale, Color.WHITE);
            } else {
                FontHelper.renderFontRightTopAligned(sb, FontHelper.topPanelInfoFont, Integer.toString(this.counter), this.currentX + 30.0f * Settings.scale, this.currentY - 7.0f * Settings.scale, Color.WHITE);
            }
        }
    }

    public void renderOutline(Color c, SpriteBatch sb, boolean inTopPanel) {
        sb.setColor(c);
        if (AbstractDungeon.screen != null && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.NEOW_UNLOCK) {
            sb.draw(this.outlineImg, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * 2.0f + MathUtils.cosDeg(System.currentTimeMillis() / 5L % 360L) / 15.0f, Settings.scale * 2.0f + MathUtils.cosDeg(System.currentTimeMillis() / 5L % 360L) / 15.0f, this.rotation, 0, 0, 128, 128, false, false);
        } else if (this.hb.hovered && Settings.isControllerMode) {
            sb.setBlendFunction(770, 1);
            this.goldOutlineColor.a = 0.6f + MathUtils.cosDeg(System.currentTimeMillis() / 2L % 360L) / 5.0f;
            sb.setColor(this.goldOutlineColor);
            sb.draw(this.outlineImg, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
            sb.setBlendFunction(770, 771);
        } else {
            sb.draw(this.outlineImg, this.currentX - 64.0f, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
        }
    }

    public void renderOutline(SpriteBatch sb, boolean inTopPanel) {
        float tmpX = this.currentX - 64.0f;
        if (inTopPanel) {
            tmpX += offsetX;
        }
        if (this.hb.hovered && Settings.isControllerMode) {
            sb.setBlendFunction(770, 1);
            this.goldOutlineColor.a = 0.6f + MathUtils.cosDeg(System.currentTimeMillis() / 2L % 360L) / 5.0f;
            sb.setColor(this.goldOutlineColor);
            sb.draw(this.outlineImg, tmpX, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
            sb.setBlendFunction(770, 771);
        } else {
            sb.setColor(PASSIVE_OUTLINE_COLOR);
            sb.draw(this.outlineImg, tmpX, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, this.rotation, 0, 0, 128, 128, false, false);
        }
    }

    public void renderFlash(SpriteBatch sb, boolean inTopPanel) {
        float tmp = Interpolation.exp10In.apply(0.0f, 4.0f, this.flashTimer / 2.0f);
        sb.setBlendFunction(770, 1);
        this.flashColor.a = this.flashTimer * 0.2f;
        sb.setColor(this.flashColor);
        float tmpX = this.currentX - 64.0f;
        if (inTopPanel) {
            tmpX += offsetX;
        }
        sb.draw(this.img, tmpX, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale + tmp, this.scale + tmp, this.rotation, 0, 0, 128, 128, false, false);
        sb.draw(this.img, tmpX, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale + tmp * 0.66f, this.scale + tmp * 0.66f, this.rotation, 0, 0, 128, 128, false, false);
        sb.draw(this.img, tmpX, this.currentY - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale + tmp / 3.0f, this.scale + tmp / 3.0f, this.rotation, 0, 0, 128, 128, false, false);
        sb.setBlendFunction(770, 771);
    }

    public void beginPulse() {
        this.flashTimer = 1.0f;
    }

    public void beginLongPulse() {
        this.flashTimer = 1.0f;
        this.pulse = true;
    }

    public void stopPulse() {
        this.pulse = false;
    }

    public void flash() {
        this.flashTimer = 2.0f;
    }

    public void renderBossTip(SpriteBatch sb) {
        TipHelper.queuePowerTips((float)Settings.WIDTH * 0.63f, (float)Settings.HEIGHT * 0.63f, this.tips);
    }

    public void renderTip(SpriteBatch sb) {
        if ((float)InputHelper.mX < 1400.0f * Settings.scale) {
            if (CardCrawlGame.mainMenuScreen.screen == MainMenuScreen.CurScreen.RELIC_VIEW) {
                TipHelper.queuePowerTips(180.0f * Settings.scale, (float)Settings.HEIGHT * 0.7f, this.tips);
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.SHOP && this.tips.size() > 2 && !AbstractDungeon.player.hasRelic(this.relicId)) {
                TipHelper.queuePowerTips((float)InputHelper.mX + 60.0f * Settings.scale, (float)InputHelper.mY + 180.0f * Settings.scale, this.tips);
            } else if (AbstractDungeon.player != null && AbstractDungeon.player.hasRelic(this.relicId)) {
                TipHelper.queuePowerTips((float)InputHelper.mX + 60.0f * Settings.scale, (float)InputHelper.mY - 30.0f * Settings.scale, this.tips);
            } else if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.COMBAT_REWARD) {
                TipHelper.queuePowerTips(360.0f * Settings.scale, (float)InputHelper.mY + 50.0f * Settings.scale, this.tips);
            } else {
                TipHelper.queuePowerTips((float)InputHelper.mX + 50.0f * Settings.scale, (float)InputHelper.mY + 50.0f * Settings.scale, this.tips);
            }
        } else {
            TipHelper.queuePowerTips((float)InputHelper.mX - 350.0f * Settings.scale, (float)InputHelper.mY - 50.0f * Settings.scale, this.tips);
        }
    }

    public boolean canPlay(AbstractCard card) {
        return true;
    }

    public static String gameDataUploadHeader() {
        GameDataStringBuilder builder = new GameDataStringBuilder();
        builder.addFieldData("name");
        builder.addFieldData("relicID");
        builder.addFieldData("color");
        builder.addFieldData("description");
        builder.addFieldData("flavorText");
        builder.addFieldData("cost");
        builder.addFieldData("tier");
        builder.addFieldData("assetURL");
        return builder.toString();
    }

    protected void initializeTips() {
        Scanner desc = new Scanner(this.description);
        while (desc.hasNext()) {
            String s = desc.next();
            if (s.charAt(0) == '#') {
                s = s.substring(2);
            }
            s = s.replace(',', ' ');
            s = s.replace('.', ' ');
            s = s.trim();
            s = s.toLowerCase();
            boolean alreadyExists = false;
            if (!GameDictionary.keywords.containsKey(s)) continue;
            s = GameDictionary.parentWord.get(s);
            for (PowerTip t : this.tips) {
                if (!t.header.toLowerCase().equals(s)) continue;
                alreadyExists = true;
                break;
            }
            if (alreadyExists) continue;
            this.tips.add(new PowerTip(TipHelper.capitalize(s), GameDictionary.keywords.get(s)));
        }
        desc.close();
    }

    public String gameDataUploadData(String color) {
        GameDataStringBuilder builder = new GameDataStringBuilder();
        builder.addFieldData(this.name);
        builder.addFieldData(this.relicId);
        builder.addFieldData(color);
        builder.addFieldData(this.description);
        builder.addFieldData(this.flavorText);
        builder.addFieldData(this.cost);
        builder.addFieldData(this.tier.name());
        builder.addFieldData(this.assetURL);
        return builder.toString();
    }

    public abstract AbstractRelic makeCopy();

    public String toString() {
        return this.name;
    }

    @Override
    public int compareTo(AbstractRelic arg0) {
        return this.name.compareTo(arg0.name);
    }

    public String getAssetURL() {
        return this.assetURL;
    }

    public HashMap<String, Serializable> getLocStrings() {
        HashMap<String, Serializable> relicData = new HashMap<String, Serializable>();
        relicData.put("name", (Serializable)((Object)this.name));
        relicData.put("description", (Serializable)((Object)this.description));
        return relicData;
    }

    public boolean canSpawn() {
        return true;
    }

    public void onUsePotion() {
    }

    public void onChangeStance(AbstractStance prevStance, AbstractStance newStance) {
    }

    public void onLoseHp(int damageAmount) {
    }

    public static void updateOffsetX() {
        float target = (float)(-relicPage * Settings.WIDTH) + (float)relicPage * (PAD_X + 36.0f * Settings.xScale);
        if (AbstractDungeon.player.relics.size() >= MAX_RELICS_PER_PAGE + 1) {
            target += 36.0f * Settings.scale;
        }
        if (offsetX != target) {
            offsetX = MathHelper.uiLerpSnap(offsetX, target);
        }
    }

    public void loadLargeImg() {
        if (this.largeImg == null) {
            this.largeImg = ImageMaster.loadImage(L_IMG_DIR + this.imgUrl);
        }
    }

    protected void addToBot(AbstractGameAction action) {
        AbstractDungeon.actionManager.addToBottom(action);
    }

    protected void addToTop(AbstractGameAction action) {
        AbstractDungeon.actionManager.addToTop(action);
    }

    public int onLoseHpLast(int damageAmount) {
        return damageAmount;
    }

    public void wasHPLost(int damageAmount) {
    }

    public static enum RelicTier {
        DEPRECATED,
        STARTER,
        COMMON,
        UNCOMMON,
        RARE,
        SPECIAL,
        BOSS,
        SHOP;

    }

    public static enum LandingSound {
        CLINK,
        FLAT,
        HEAVY,
        MAGICAL,
        SOLID;

    }
}


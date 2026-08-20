/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.shrines;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.cards.curses.Decay;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.RainingGoldEffect;
import com.megacrit.cardcrawl.vfx.cardManip.PurgeCardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GremlinWheelGame
extends AbstractImageEvent {
    private static final Logger logger = LogManager.getLogger(GremlinWheelGame.class.getName());
    public static final String ID = "Wheel of Change";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Wheel of Change");
    public static final String NAME = GremlinWheelGame.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = GremlinWheelGame.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = GremlinWheelGame.eventStrings.OPTIONS;
    public static final String INTRO_DIALOG = DESCRIPTIONS[0];
    private CUR_SCREEN screen = CUR_SCREEN.INTRO;
    private int result;
    private float resultAngle;
    private float tmpAngle;
    private boolean startSpin = false;
    private boolean finishSpin = false;
    private boolean doneSpinning = false;
    private boolean bounceIn = true;
    private float bounceTimer = 1.0f;
    private float animTimer = 3.0f;
    private float spinVelocity = 200.0f;
    private int goldAmount;
    private boolean purgeResult = false;
    private boolean buttonPressed = false;
    private Hitbox buttonHb = new Hitbox(450.0f * Settings.scale, 300.0f * Settings.scale);
    private Texture wheelImg;
    private Texture arrowImg;
    private Texture buttonImg;
    private static final float START_Y = Settings.OPTION_Y + 1000.0f * Settings.scale;
    private static final float TARGET_Y = Settings.OPTION_Y;
    private float imgX = (float)Settings.WIDTH / 2.0f;
    private float imgY = START_Y;
    private float wheelAngle = 0.0f;
    private static final int WHEEL_W = 1024;
    private static final int ARROW_W = 512;
    private static final float ARROW_OFFSET_X = 300.0f * Settings.scale;
    private Color color = new Color(1.0f, 1.0f, 1.0f, 0.0f);
    private float hpLossPercent = 0.1f;
    private static final float A_2_HP_LOSS = 0.15f;

    public GremlinWheelGame() {
        super(NAME, INTRO_DIALOG, "images/events/spinTheWheel.jpg");
        this.wheelImg = ImageMaster.loadImage("images/events/wheel.png");
        this.arrowImg = ImageMaster.loadImage("images/events/wheelArrow.png");
        this.buttonImg = ImageMaster.loadImage("images/events/spinButton.png");
        this.noCardsInRewards = true;
        if (AbstractDungeon.ascensionLevel >= 15) {
            this.hpLossPercent = 0.15f;
        }
        this.imageEventText.setDialogOption(OPTIONS[0]);
        this.setGold();
        this.hasDialog = true;
        this.hasFocus = true;
        this.buttonHb.move(500.0f * Settings.scale, -500.0f * Settings.scale);
    }

    private void setGold() {
        if (Objects.equals(AbstractDungeon.id, "Exordium")) {
            this.goldAmount = 100;
        } else if (Objects.equals(AbstractDungeon.id, "TheCity")) {
            this.goldAmount = 200;
        } else if (Objects.equals(AbstractDungeon.id, "TheBeyond")) {
            this.goldAmount = 300;
        }
    }

    @Override
    public void update() {
        super.update();
        this.updatePosition();
        this.purgeLogic();
        if (this.bounceTimer == 0.0f && this.startSpin) {
            if (!this.buttonPressed) {
                this.buttonHb.cY = MathHelper.cardLerpSnap(this.buttonHb.cY, Settings.OPTION_Y - 330.0f * Settings.scale);
                this.buttonHb.move(this.buttonHb.cX, this.buttonHb.cY);
                this.buttonHb.update();
                if (this.buttonHb.hovered && InputHelper.justClickedLeft || CInputActionSet.proceed.isJustPressed()) {
                    this.buttonPressed = true;
                    this.buttonHb.hovered = false;
                    CardCrawlGame.sound.play("WHEEL");
                }
            } else {
                this.buttonHb.cY = MathHelper.cardLerpSnap(this.buttonHb.cY, -500.0f * Settings.scale);
            }
        }
        if (this.startSpin && this.bounceTimer == 0.0f && this.buttonPressed) {
            this.imgY = TARGET_Y;
            if (this.animTimer > 0.0f) {
                this.animTimer -= Gdx.graphics.getDeltaTime();
                this.wheelAngle += this.spinVelocity * Gdx.graphics.getDeltaTime();
            } else {
                this.finishSpin = true;
                this.animTimer = 3.0f;
                this.startSpin = false;
                this.tmpAngle = this.resultAngle;
            }
        } else if (this.finishSpin) {
            if (this.animTimer > 0.0f) {
                this.animTimer -= Gdx.graphics.getDeltaTime();
                if (this.animTimer < 0.0f) {
                    this.animTimer = 1.0f;
                    this.finishSpin = false;
                    this.doneSpinning = true;
                }
                this.wheelAngle = Interpolation.elasticIn.apply(this.tmpAngle, -180.0f, this.animTimer / 3.0f);
            }
        } else if (this.doneSpinning) {
            if (this.animTimer > 0.0f) {
                this.animTimer -= Gdx.graphics.getDeltaTime();
                if (this.animTimer <= 0.0f) {
                    this.bounceTimer = 1.0f;
                    this.bounceIn = false;
                }
            } else if (this.bounceTimer == 0.0f) {
                this.doneSpinning = false;
                this.imageEventText.clearAllDialogs();
                this.preApplyResult();
                GenericEventDialog.show();
                this.screen = CUR_SCREEN.COMPLETE;
            }
        }
        if (!GenericEventDialog.waitForInput) {
            this.buttonEffect(GenericEventDialog.getSelectedOption());
        }
    }

    private void updatePosition() {
        if (this.bounceTimer != 0.0f) {
            this.bounceTimer -= Gdx.graphics.getDeltaTime();
            if (this.bounceTimer < 0.0f) {
                this.bounceTimer = 0.0f;
            }
            if (this.bounceIn && this.startSpin) {
                this.color.a = Interpolation.fade.apply(1.0f, 0.0f, this.bounceTimer);
                this.imgY = Interpolation.bounceIn.apply(TARGET_Y, START_Y, this.bounceTimer);
            } else if (this.doneSpinning) {
                this.color.a = Interpolation.fade.apply(0.0f, 1.0f, this.bounceTimer);
                this.imgY = Interpolation.swingOut.apply(START_Y, TARGET_Y, this.bounceTimer);
            }
        }
    }

    private void preApplyResult() {
        switch (this.result) {
            case 0: {
                this.imageEventText.updateBodyText(DESCRIPTIONS[1]);
                this.imageEventText.setDialogOption(OPTIONS[1]);
                AbstractDungeon.effectList.add(new RainingGoldEffect(this.goldAmount));
                AbstractDungeon.player.gainGold(this.goldAmount);
                break;
            }
            case 1: {
                this.imageEventText.updateBodyText(DESCRIPTIONS[2]);
                this.imageEventText.setDialogOption(OPTIONS[2]);
                break;
            }
            case 2: {
                this.imageEventText.updateBodyText(DESCRIPTIONS[3]);
                this.imageEventText.setDialogOption(OPTIONS[3]);
                break;
            }
            case 3: {
                this.imageEventText.updateBodyText(DESCRIPTIONS[4]);
                this.imageEventText.setDialogOption(OPTIONS[4]);
                break;
            }
            case 4: {
                this.imageEventText.updateBodyText(DESCRIPTIONS[5]);
                this.imageEventText.setDialogOption(OPTIONS[5]);
                break;
            }
            default: {
                this.imageEventText.updateBodyText(DESCRIPTIONS[6]);
                this.imageEventText.setDialogOption(OPTIONS[6] + (int)((float)AbstractDungeon.player.maxHealth * this.hpLossPercent) + OPTIONS[7]);
                this.color = new Color(0.5f, 0.5f, 0.5f, 1.0f);
            }
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                if (buttonPressed != 0) break;
                GenericEventDialog.hide();
                this.result = AbstractDungeon.miscRng.random(0, 5);
                this.resultAngle = (float)this.result * 60.0f + MathUtils.random(-10.0f, 10.0f);
                this.wheelAngle = 0.0f;
                this.startSpin = true;
                this.bounceTimer = 2.0f;
                this.animTimer = 2.0f;
                this.spinVelocity = 1500.0f;
                break;
            }
            case COMPLETE: {
                this.applyResult();
                this.imageEventText.clearAllDialogs();
                this.imageEventText.setDialogOption(OPTIONS[8]);
                this.screen = CUR_SCREEN.LEAVE;
                break;
            }
            case LEAVE: {
                this.openMap();
                break;
            }
            default: {
                logger.info("UNHANDLED CASE");
            }
        }
    }

    private void applyResult() {
        switch (this.result) {
            case 0: {
                this.hasFocus = false;
                GremlinWheelGame.logMetricGainGold(ID, "Gold", this.goldAmount);
                break;
            }
            case 1: {
                AbstractDungeon.getCurrRoom().rewards.clear();
                AbstractRelic r = AbstractDungeon.returnRandomScreenlessRelic(AbstractDungeon.returnRandomRelicTier());
                AbstractDungeon.getCurrRoom().addRelicToRewards(r);
                AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
                AbstractDungeon.combatRewardScreen.open();
                GremlinWheelGame.logMetric(ID, "Relic");
                this.hasFocus = false;
                break;
            }
            case 2: {
                GremlinWheelGame.logMetricHeal(ID, "Full Heal", AbstractDungeon.player.maxHealth - AbstractDungeon.player.currentHealth);
                AbstractDungeon.player.heal(AbstractDungeon.player.maxHealth);
                this.hasFocus = false;
                break;
            }
            case 3: {
                Decay curse = new Decay();
                GremlinWheelGame.logMetricObtainCard(ID, "Cursed", curse);
                AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(curse, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                this.hasFocus = false;
                break;
            }
            case 4: {
                if (CardGroup.getGroupWithoutBottledCards(AbstractDungeon.player.masterDeck.getPurgeableCards()).size() <= 0) break;
                AbstractDungeon.gridSelectScreen.open(CardGroup.getGroupWithoutBottledCards(AbstractDungeon.player.masterDeck.getPurgeableCards()), 1, OPTIONS[9], false, false, false, true);
                this.roomEventText.hide();
                this.purgeResult = true;
                break;
            }
            default: {
                this.imageEventText.updateBodyText(DESCRIPTIONS[7]);
                CardCrawlGame.sound.play("ATTACK_DAGGER_6");
                CardCrawlGame.sound.play("BLOOD_SPLAT");
                int damageAmount = (int)((float)AbstractDungeon.player.maxHealth * this.hpLossPercent);
                AbstractDungeon.player.damage(new DamageInfo(null, damageAmount, DamageInfo.DamageType.HP_LOSS));
                GremlinWheelGame.logMetricTakeDamage(ID, "Damaged", damageAmount);
            }
        }
    }

    private void purgeLogic() {
        if (this.purgeResult && !AbstractDungeon.isScreenUp && !AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            AbstractCard c = AbstractDungeon.gridSelectScreen.selectedCards.get(0);
            GremlinWheelGame.logMetricCardRemoval(ID, "Card Removal", c);
            AbstractDungeon.player.masterDeck.removeCard(c);
            AbstractDungeon.effectList.add(new PurgeCardEffect(c));
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
            this.hasFocus = false;
            this.purgeResult = false;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.setColor(this.color);
        sb.draw(this.wheelImg, this.imgX - 512.0f, this.imgY - 512.0f, 512.0f, 512.0f, 1024.0f, 1024.0f, Settings.scale, Settings.scale, this.wheelAngle, 0, 0, 1024, 1024, false, false);
        sb.draw(this.arrowImg, this.imgX - 256.0f + ARROW_OFFSET_X + 180.0f * Settings.scale, this.imgY - 256.0f, 256.0f, 256.0f, 512.0f, 512.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 512, false, false);
        if (this.buttonHb.hovered) {
            sb.draw(this.buttonImg, this.buttonHb.cX - 256.0f, this.buttonHb.cY - 256.0f, 256.0f, 256.0f, 512.0f, 512.0f, Settings.scale * 1.05f, Settings.scale * 1.05f, 0.0f, 0, 0, 512, 512, false, false);
        } else {
            sb.draw(this.buttonImg, this.buttonHb.cX - 256.0f, this.buttonHb.cY - 256.0f, 256.0f, 256.0f, 512.0f, 512.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 512, false, false);
        }
        sb.setBlendFunction(770, 1);
        if (this.buttonHb.hovered) {
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.25f));
        } else {
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, (MathUtils.cosDeg(System.currentTimeMillis() / 5L % 360L) + 1.25f) / 3.5f));
        }
        if (this.buttonHb.hovered) {
            sb.draw(this.buttonImg, this.buttonHb.cX - 256.0f, this.buttonHb.cY - 256.0f, 256.0f, 256.0f, 512.0f, 512.0f, Settings.scale * 1.05f, Settings.scale * 1.05f, 0.0f, 0, 0, 512, 512, false, false);
        } else {
            sb.draw(this.buttonImg, this.buttonHb.cX - 256.0f, this.buttonHb.cY - 256.0f, 256.0f, 256.0f, 512.0f, 512.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 512, 512, false, false);
        }
        if (Settings.isControllerMode) {
            sb.draw(CInputActionSet.proceed.getKeyImg(), this.buttonHb.cX - 32.0f - 160.0f * Settings.scale, this.buttonHb.cY - 32.0f - 70.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        }
        sb.setBlendFunction(770, 771);
        this.buttonHb.render(sb);
    }

    @Override
    public void renderAboveTopPanel(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
        super.dispose();
        if (this.wheelImg != null) {
            this.wheelImg.dispose();
        }
        if (this.arrowImg != null) {
            this.arrowImg.dispose();
        }
        if (this.buttonImg != null) {
            this.buttonImg.dispose();
        }
    }

    private static enum CUR_SCREEN {
        INTRO,
        LEAVE,
        SPIN,
        COMPLETE;

    }
}


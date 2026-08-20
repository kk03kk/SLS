/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.MonsterHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AbstractEvent
implements Disposable {
    private static final Logger logger = LogManager.getLogger(AbstractEvent.class.getName());
    protected Texture img = null;
    public RoomEventDialog roomEventText = new RoomEventDialog();
    public GenericEventDialog imageEventText = new GenericEventDialog();
    protected float drawX;
    protected float drawY;
    protected float imgWidth;
    protected float imgHeight;
    protected Color imgColor = Color.WHITE.cpy();
    protected Hitbox hb = null;
    public float panelAlpha = 0.0f;
    public boolean hideAlpha = false;
    public boolean hasFocus = false;
    protected String body = null;
    public float waitTimer = 1.5f;
    protected boolean waitForInput = false;
    public boolean hasDialog = false;
    protected int screenNum = 0;
    public static EventType type = EventType.IMAGE;
    public static String NAME;
    public static String[] DESCRIPTIONS;
    public static String[] OPTIONS;
    public boolean combatTime = false;
    public boolean noCardsInRewards = false;
    public ArrayList<Integer> optionsSelected = new ArrayList();

    public AbstractEvent() {
        type = EventType.ROOM;
        if (Settings.FAST_MODE) {
            this.waitTimer = 0.1f;
        }
    }

    protected void initializeImage(String imgUrl, float x, float y) {
        this.img = ImageMaster.loadImage(imgUrl);
        this.drawX = x;
        this.drawY = y;
        this.imgWidth = (float)this.img.getWidth() * Settings.xScale;
        this.imgHeight = (float)this.img.getHeight() * Settings.scale;
    }

    public void onEnterRoom() {
    }

    public void enterCombat() {
        this.roomEventText.clear();
        AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMBAT;
        AbstractDungeon.getCurrRoom().monsters.init();
        AbstractRoom.waitTimer = 0.1f;
        AbstractDungeon.player.preBattlePrep();
        this.hasFocus = false;
        this.roomEventText.hide();
    }

    protected abstract void buttonEffect(int var1);

    public void updateDialog() {
        this.imageEventText.update();
        this.roomEventText.update();
    }

    public void update() {
        if (this.waitTimer > 0.0f) {
            this.waitTimer -= Gdx.graphics.getDeltaTime();
            if (this.waitTimer < 0.0f && this.hasDialog) {
                this.roomEventText.show(this.body);
                this.waitTimer = 0.0f;
            }
        } else {
            this.panelAlpha = AbstractDungeon.getCurrRoom().phase != AbstractRoom.RoomPhase.COMBAT && !this.hideAlpha ? MathHelper.fadeLerpSnap(this.panelAlpha, 0.66f) : MathHelper.fadeLerpSnap(this.panelAlpha, 0.0f);
        }
        if (!RoomEventDialog.waitForInput) {
            this.buttonEffect(this.roomEventText.getSelectedOption());
        }
    }

    public void logInput(int buttonPressed) {
        this.optionsSelected.add(buttonPressed);
    }

    protected void openMap() {
        AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMPLETE;
        AbstractDungeon.dungeonMapScreen.open(false);
    }

    public void render(SpriteBatch sb) {
        if (this.img != null) {
            sb.setColor(this.imgColor);
            sb.draw(this.img, this.drawX, this.drawY, this.imgWidth, this.imgHeight);
        }
        if (this.hb != null) {
            this.hb.render(sb);
            if (this.img != null && this.hb.hovered) {
                sb.setBlendFunction(770, 1);
                sb.setColor(new Color(1.0f, 1.0f, 1.0f, 0.3f));
                sb.draw(this.img, this.drawX, this.drawY, this.imgWidth, this.imgHeight);
                sb.setBlendFunction(770, 771);
            }
        }
    }

    public void renderText(SpriteBatch sb) {
        this.roomEventText.render(sb);
        this.imageEventText.render(sb);
    }

    public void renderRoomEventPanel(SpriteBatch sb) {
        sb.setColor(new Color(0.0f, 0.0f, 0.0f, this.panelAlpha));
        sb.draw(ImageMaster.EVENT_ROOM_PANEL, 0.0f, (float)Settings.HEIGHT - 475.0f * Settings.scale, (float)Settings.WIDTH, 300.0f * Settings.scale);
    }

    public void showProceedScreen(String bodyText) {
        this.roomEventText.updateBodyText(bodyText);
        this.roomEventText.updateDialogOption(0, "[ #bProceed ]");
        this.roomEventText.clearRemainingOptions();
        this.screenNum = 99;
    }

    public void renderAboveTopPanel(SpriteBatch sb) {
    }

    public void reopen() {
    }

    public void postCombatLoad() {
        AbstractDungeon.getCurrRoom().phase = AbstractRoom.RoomPhase.COMBAT;
        AbstractDungeon.getCurrRoom().isBattleOver = true;
        AbstractDungeon.getCurrRoom().monsters = MonsterHelper.getEncounter("Colosseum Nobs");
        this.hasFocus = false;
        GenericEventDialog.hide();
        AbstractDungeon.rs = AbstractDungeon.RenderScene.NORMAL;
    }

    public static void logMetric(String eventName, String playerChoice, List<String> cardsObtained, List<String> cardsRemoved, List<String> cardsTransformed, List<String> cardsUpgraded, List<String> relicsObtained, List<String> potionsObtained, List<String> relicsLost, int damageTaken, int damageHealed, int hpLoss, int hpGain, int goldGain, int goldLoss) {
        HashMap<String, Object> choice = new HashMap<String, Object>();
        choice.put("event_name", eventName);
        choice.put("player_choice", playerChoice);
        choice.put("floor", AbstractDungeon.floorNum);
        choice.put("cards_obtained", cardsObtained);
        choice.put("cards_removed", cardsRemoved);
        choice.put("cards_transformed", cardsTransformed);
        choice.put("cards_upgraded", cardsUpgraded);
        choice.put("relics_obtained", relicsObtained);
        choice.put("potions_obtained", potionsObtained);
        choice.put("relics_lost", relicsLost);
        choice.put("damage_taken", damageTaken);
        choice.put("damage_healed", damageHealed);
        choice.put("max_hp_loss", hpLoss);
        choice.put("max_hp_gain", hpGain);
        choice.put("gold_gain", goldGain);
        choice.put("gold_loss", goldLoss);
        CardCrawlGame.metricData.event_choices.add(choice);
    }

    public static void logMetricTransformCardsAtCost(String eventName, String playerChoice, List<String> cardsTransformed, List<String> cardsObtained, int cost) {
        AbstractEvent.logMetric(eventName, playerChoice, cardsObtained, null, cardsTransformed, null, null, null, null, 0, 0, 0, 0, 0, cost);
    }

    public static void logMetricRemoveCardsAtCost(String eventName, String playerChoice, List<String> cardsRemoved, int cost) {
        AbstractEvent.logMetric(eventName, playerChoice, null, cardsRemoved, null, null, null, null, null, 0, 0, 0, 0, 0, cost);
    }

    public static void logMetricRemoveCards(String eventName, String playerChoice, List<String> cardsRemoved) {
        AbstractEvent.logMetricRemoveCardsAtCost(eventName, playerChoice, cardsRemoved, 0);
    }

    public static void logMetricObtainCardsLoseMapHP(String eventName, String playerChoice, List<String> cardsObtained, int maxHPLoss) {
        AbstractEvent.logMetric(eventName, playerChoice, cardsObtained, null, null, null, null, null, null, 0, 0, maxHPLoss, 0, 0, 0);
    }

    public static void logMetricObtainCardsLoseRelic(String eventName, String playerChoice, List<String> cardsObtained, AbstractRelic relicLost) {
        ArrayList<String> tempList2 = new ArrayList<String>();
        tempList2.add(relicLost.relicId);
        AbstractEvent.logMetric(eventName, playerChoice, cardsObtained, null, null, null, null, null, tempList2, 0, 0, 0, 0, 0, 0);
    }

    public static void logMetricObtainCards(String eventName, String playerChoice, List<String> cardsObtained) {
        AbstractEvent.logMetricObtainCardsLoseMapHP(eventName, playerChoice, cardsObtained, 0);
    }

    public static void logMetricUpgradeCardsAtCost(String eventName, String playerChoice, List<String> cardsUpgraded, int cost) {
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, cardsUpgraded, null, null, null, 0, 0, 0, 0, 0, cost);
    }

    public static void logMetricUpgradeCards(String eventName, String playerChoice, List<String> cardsUpgraded) {
        AbstractEvent.logMetricUpgradeCardsAtCost(eventName, playerChoice, cardsUpgraded, 0);
    }

    public static void logMetricTransformCards(String eventName, String playerChoice, List<String> cardsTransformed, List<String> cardsObtained) {
        AbstractEvent.logMetricTransformCardsAtCost(eventName, playerChoice, cardsTransformed, cardsObtained, 0);
    }

    public static void logMetricGainGoldAndDamage(String eventName, String playerChoice, int gold, int damage) {
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, null, null, null, damage, 0, 0, 0, gold, 0);
    }

    public static void logMetricGainGoldAndRelic(String eventName, String playerChoice, AbstractRelic relicGained, int gold) {
        ArrayList<String> tempList2 = new ArrayList<String>();
        tempList2.add(relicGained.relicId);
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, tempList2, null, null, 0, 0, 0, 0, gold, 0);
    }

    public static void logMetricGainGoldAndLoseRelic(String eventName, String playerChoice, AbstractRelic relicLost, int gold) {
        ArrayList<String> tempList2 = new ArrayList<String>();
        tempList2.add(relicLost.relicId);
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, null, null, tempList2, 0, 0, 0, 0, gold, 0);
    }

    public static void logMetricGainGoldAndCard(String eventName, String playerChoice, AbstractCard cardGained, int gold) {
        ArrayList<String> tempList2 = new ArrayList<String>();
        tempList2.add(cardGained.cardID);
        AbstractEvent.logMetric(eventName, playerChoice, tempList2, null, null, null, null, null, null, 0, 0, 0, 0, gold, 0);
    }

    public static void logMetricObtainRelicAndLoseMaxHP(String eventName, String playerChoice, AbstractRelic relicGained, int hpLoss) {
        ArrayList<String> tempList2 = new ArrayList<String>();
        tempList2.add(relicGained.relicId);
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, tempList2, null, null, 0, 0, hpLoss, 0, 0, 0);
    }

    public static void logMetricObtainRelicAndDamage(String eventName, String playerChoice, AbstractRelic relicGained, int damage) {
        ArrayList<String> tempList2 = new ArrayList<String>();
        tempList2.add(relicGained.relicId);
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, tempList2, null, null, damage, 0, 0, 0, 0, 0);
    }

    public static void logMetricObtainRelicAtCost(String eventName, String playerChoice, AbstractRelic relicGained, int cost) {
        ArrayList<String> tempList2 = new ArrayList<String>();
        tempList2.add(relicGained.relicId);
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, tempList2, null, null, 0, 0, 0, 0, 0, cost);
    }

    public static void logMetricGainAndLoseGold(String eventName, String playerChoice, int goldGain, int goldLoss) {
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, null, null, null, 0, 0, 0, 0, goldGain, goldLoss);
    }

    public static void logMetricGainGold(String eventName, String playerChoice, int gold) {
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, null, null, null, 0, 0, 0, 0, gold, 0);
    }

    public static void logMetricLoseGold(String eventName, String playerChoice, int gold) {
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, null, null, null, 0, 0, 0, 0, 0, gold);
    }

    public static void logMetricTakeDamage(String eventName, String playerChoice, int damage) {
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, null, null, null, damage, 0, 0, 0, 0, 0);
    }

    public static void logMetricCardRemovalAtCost(String eventName, String playerChoice, AbstractCard cardRemoved, int cost) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(cardRemoved.cardID);
        AbstractEvent.logMetric(eventName, playerChoice, null, tempList, null, null, null, null, null, 0, 0, 0, 0, 0, cost);
    }

    public static void logMetricCardRemovalAndDamage(String eventName, String playerChoice, AbstractCard cardRemoved, int damage) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(cardRemoved.cardID);
        AbstractEvent.logMetric(eventName, playerChoice, null, tempList, null, null, null, null, null, damage, 0, 0, 0, 0, 0);
    }

    public static void logMetricCardRemovalHealMaxHPUp(String eventName, String playerChoice, AbstractCard cardRemoved, int heal, int maxUp) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(cardRemoved.cardID);
        AbstractEvent.logMetric(eventName, playerChoice, null, tempList, null, null, null, null, null, 0, heal, 0, maxUp, 0, 0);
    }

    public static void logMetricCardRemovalAndHeal(String eventName, String playerChoice, AbstractCard cardRemoved, int heal) {
        AbstractEvent.logMetricCardRemovalHealMaxHPUp(eventName, playerChoice, cardRemoved, heal, 0);
    }

    public static void logMetricCardRemoval(String eventName, String playerChoice, AbstractCard cardRemoved) {
        AbstractEvent.logMetricCardRemovalAtCost(eventName, playerChoice, cardRemoved, 0);
    }

    public static void logMetricCardUpgradeAndRemovalAtCost(String eventName, String playerChoice, AbstractCard cardUpgraded, AbstractCard cardRemoved, int cost) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(cardUpgraded.cardID);
        ArrayList<String> tempList2 = new ArrayList<String>();
        tempList2.add(cardRemoved.cardID);
        AbstractEvent.logMetric(eventName, playerChoice, null, tempList2, null, tempList, null, null, null, 0, 0, 0, 0, 0, cost);
    }

    public static void logMetricCardUpgradeAndRemoval(String eventName, String playerChoice, AbstractCard cardUpgraded, AbstractCard cardRemoved) {
        AbstractEvent.logMetricCardUpgradeAndRemovalAtCost(eventName, playerChoice, cardUpgraded, cardRemoved, 0);
    }

    public static void logMetricCardUpgradeAtCost(String eventName, String playerChoice, AbstractCard cardUpgraded, int cost) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(cardUpgraded.cardID);
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, tempList, null, null, null, 0, 0, 0, 0, 0, cost);
    }

    public static void logMetricCardUpgrade(String eventName, String playerChoice, AbstractCard cardUpgraded) {
        AbstractEvent.logMetricCardUpgradeAtCost(eventName, playerChoice, cardUpgraded, 0);
    }

    public static void logMetricHealAtCost(String eventName, String playerChoice, int cost, int healAmount) {
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, null, null, null, 0, healAmount, 0, 0, 0, cost);
    }

    public static void logMetricHealAndLoseMaxHP(String eventName, String playerChoice, int healAmount, int maxHPLoss) {
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, null, null, null, 0, healAmount, maxHPLoss, 0, 0, 0);
    }

    public static void logMetricHeal(String eventName, String playerChoice, int healAmount) {
        AbstractEvent.logMetricHealAtCost(eventName, playerChoice, 0, healAmount);
    }

    public static void logMetric(String eventName, String playerChoice) {
        AbstractEvent.logMetricHeal(eventName, playerChoice, 0);
    }

    public static void logMetricIgnored(String eventName) {
        AbstractEvent.logMetric(eventName, "Ignored");
    }

    public static void logMetricMaxHPGain(String eventName, String playerChoice, int maxHPAmount) {
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, null, null, null, 0, 0, 0, maxHPAmount, 0, 0);
    }

    public static void logMetricMaxHPLoss(String eventName, String playerChoice, int hpLoss) {
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, null, null, null, 0, 0, hpLoss, 0, 0, 0);
    }

    public static void logMetricDamageAndMaxHPGain(String eventName, String playerChoice, int damage, int maxHPAmount) {
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, null, null, null, damage, 0, 0, maxHPAmount, 0, 0);
    }

    public static void logMetricObtainCardAndHeal(String eventName, String playerChoice, AbstractCard cardGained, int heal) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(cardGained.cardID);
        AbstractEvent.logMetric(eventName, playerChoice, tempList, null, null, null, null, null, null, 0, heal, 0, 0, 0, 0);
    }

    public static void logMetricObtainCardAndDamage(String eventName, String playerChoice, AbstractCard cardGained, int damage) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(cardGained.cardID);
        AbstractEvent.logMetric(eventName, playerChoice, tempList, null, null, null, null, null, null, damage, 0, 0, 0, 0, 0);
    }

    public static void logMetricObtainCardAndLoseCard(String eventName, String playerChoice, AbstractCard cardGained, AbstractCard cardLost) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(cardGained.cardID);
        ArrayList<String> tempList2 = new ArrayList<String>();
        tempList2.add(cardLost.cardID);
        AbstractEvent.logMetric(eventName, playerChoice, tempList, tempList2, null, null, null, null, null, 0, 0, 0, 0, 0, 0);
    }

    public static void logMetricObtainCardAndRelic(String eventName, String playerChoice, AbstractCard cardGained, AbstractRelic relicGained) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(cardGained.cardID);
        ArrayList<String> tempList2 = new ArrayList<String>();
        tempList2.add(relicGained.relicId);
        AbstractEvent.logMetric(eventName, playerChoice, tempList, null, null, null, tempList2, null, null, 0, 0, 0, 0, 0, 0);
    }

    public static void logMetricRemoveCardAndObtainRelic(String eventName, String playerChoice, AbstractCard cardRemoved, AbstractRelic relicGained) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(cardRemoved.cardID);
        ArrayList<String> tempList2 = new ArrayList<String>();
        tempList2.add(relicGained.relicId);
        AbstractEvent.logMetric(eventName, playerChoice, null, tempList, null, null, tempList2, null, null, 0, 0, 0, 0, 0, 0);
    }

    public static void logMetricTransformCardAtCost(String eventName, String playerChoice, AbstractCard cardTransformed, AbstractCard cardGained, int cost) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(cardTransformed.cardID);
        ArrayList<String> tempList2 = new ArrayList<String>();
        tempList2.add(cardGained.cardID);
        AbstractEvent.logMetric(eventName, playerChoice, tempList2, null, tempList, null, null, null, null, 0, 0, 0, 0, 0, cost);
    }

    public static void logMetricTransformCard(String eventName, String playerChoice, AbstractCard cardTransformed, AbstractCard cardGained) {
        AbstractEvent.logMetricTransformCardAtCost(eventName, playerChoice, cardTransformed, cardGained, 0);
    }

    public static void logMetricRelicSwap(String eventName, String playerChoice, AbstractRelic relicGained, AbstractRelic relicLost) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(relicGained.relicId);
        ArrayList<String> tempList2 = new ArrayList<String>();
        tempList2.add(relicLost.relicId);
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, tempList, null, tempList2, 0, 0, 0, 0, 0, 0);
    }

    public static void logMetricObtainRelic(String eventName, String playerChoice, AbstractRelic relicGained) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(relicGained.relicId);
        AbstractEvent.logMetric(eventName, playerChoice, null, null, null, null, tempList, null, null, 0, 0, 0, 0, 0, 0);
    }

    public static void logMetricObtainCard(String eventName, String playerChoice, AbstractCard cardGained) {
        ArrayList<String> tempList = new ArrayList<String>();
        tempList.add(cardGained.cardID);
        AbstractEvent.logMetric(eventName, playerChoice, tempList, null, null, null, null, null, null, 0, 0, 0, 0, 0, 0);
    }

    public HashMap<String, Serializable> getLocStrings() {
        HashMap<String, Serializable> data = new HashMap<String, Serializable>();
        data.put("name", (Serializable)((Object)NAME));
        data.put("moves", (Serializable)DESCRIPTIONS);
        data.put("dialogs", (Serializable)OPTIONS);
        return data;
    }

    @Override
    public void dispose() {
        if (this.img != null) {
            logger.info("Disposed event img asset");
            this.img.dispose();
            this.img = null;
        }
        this.imageEventText.clear();
        this.roomEventText.clear();
    }

    public static enum EventType {
        TEXT,
        IMAGE,
        ROOM;

    }
}


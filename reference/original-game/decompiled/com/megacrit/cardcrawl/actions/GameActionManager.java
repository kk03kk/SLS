/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.IntentFlashAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.EnableEndTurnButtonAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.ShowMoveNameAction;
import com.megacrit.cardcrawl.actions.defect.TriggerEndOfTurnOrbsAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.cards.tempCards.Shiv;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.daily.mods.Careless;
import com.megacrit.cardcrawl.daily.mods.ControlledChaos;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.TipTracker;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterQueueItem;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.UnceasingTop;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.ThoughtBubble;
import com.megacrit.cardcrawl.vfx.cardManip.ExhaustCardEffect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GameActionManager {
    private static final Logger logger = LogManager.getLogger(GameActionManager.class.getName());
    private ArrayList<AbstractGameAction> nextCombatActions = new ArrayList();
    public ArrayList<AbstractGameAction> actions = new ArrayList();
    public ArrayList<AbstractGameAction> preTurnActions = new ArrayList();
    public ArrayList<CardQueueItem> cardQueue = new ArrayList();
    public ArrayList<MonsterQueueItem> monsterQueue = new ArrayList();
    public ArrayList<AbstractCard> cardsPlayedThisTurn = new ArrayList();
    public ArrayList<AbstractCard> cardsPlayedThisCombat = new ArrayList();
    public ArrayList<AbstractOrb> orbsChanneledThisCombat = new ArrayList();
    public ArrayList<AbstractOrb> orbsChanneledThisTurn = new ArrayList();
    public HashMap<String, Integer> uniqueStancesThisCombat = new HashMap();
    public int mantraGained = 0;
    public AbstractGameAction currentAction;
    public AbstractGameAction previousAction;
    public AbstractGameAction turnStartCurrentAction;
    public AbstractCard lastCard = null;
    public Phase phase = Phase.WAITING_ON_USER;
    public boolean hasControl = true;
    public boolean turnHasEnded = false;
    public boolean usingCard = false;
    public boolean monsterAttacksQueued = true;
    public static int totalDiscardedThisTurn = 0;
    public static int damageReceivedThisTurn = 0;
    public static int damageReceivedThisCombat = 0;
    public static int hpLossThisCombat = 0;
    public static int playerHpLastTurn;
    public static int energyGainedThisCombat;
    public static int turn;

    public void addToNextCombat(AbstractGameAction action) {
        this.nextCombatActions.add(action);
    }

    public void useNextCombatActions() {
        for (AbstractGameAction a : this.nextCombatActions) {
            this.addToBottom(a);
        }
        this.nextCombatActions.clear();
    }

    public void addToBottom(AbstractGameAction action) {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.actions.add(action);
        }
    }

    public void addCardQueueItem(CardQueueItem c, boolean inFrontOfQueue) {
        if (inFrontOfQueue) {
            if (!AbstractDungeon.actionManager.cardQueue.isEmpty()) {
                AbstractDungeon.actionManager.cardQueue.add(1, c);
            } else {
                AbstractDungeon.actionManager.cardQueue.add(c);
            }
        } else {
            AbstractDungeon.actionManager.cardQueue.add(c);
        }
    }

    public void addCardQueueItem(CardQueueItem c) {
        this.addCardQueueItem(c, false);
    }

    public void removeFromQueue(AbstractCard c) {
        int index = -1;
        for (int i = 0; i < this.cardQueue.size(); ++i) {
            if (this.cardQueue.get((int)i).card == null || !this.cardQueue.get((int)i).card.equals(c)) continue;
            index = i;
            break;
        }
        if (index != -1) {
            this.cardQueue.remove(index);
        }
    }

    public void clearPostCombatActions() {
        Iterator<AbstractGameAction> i = this.actions.iterator();
        while (i.hasNext()) {
            AbstractGameAction e = i.next();
            if (e instanceof HealAction || e instanceof GainBlockAction || e instanceof UseCardAction || e.actionType == AbstractGameAction.ActionType.DAMAGE) continue;
            i.remove();
        }
    }

    public void addToTop(AbstractGameAction action) {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.actions.add(0, action);
        }
    }

    public void addToTurnStart(AbstractGameAction action) {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.preTurnActions.add(0, action);
        }
    }

    public void update() {
        switch (this.phase) {
            case WAITING_ON_USER: {
                this.getNextAction();
                break;
            }
            case EXECUTING_ACTIONS: {
                if (this.currentAction != null && !this.currentAction.isDone) {
                    this.currentAction.update();
                    break;
                }
                this.previousAction = this.currentAction;
                this.currentAction = null;
                this.getNextAction();
                if (this.currentAction == null && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT && !this.usingCard) {
                    this.phase = Phase.WAITING_ON_USER;
                    AbstractDungeon.player.hand.refreshHandLayout();
                    this.hasControl = false;
                }
                this.usingCard = false;
                break;
            }
            default: {
                logger.info("This should never be called");
            }
        }
    }

    public void endTurn() {
        AbstractDungeon.player.resetControllerValues();
        this.turnHasEnded = true;
        playerHpLastTurn = AbstractDungeon.player.currentHealth;
    }

    private void getNextAction() {
        if (!this.actions.isEmpty()) {
            this.currentAction = this.actions.remove(0);
            this.phase = Phase.EXECUTING_ACTIONS;
            this.hasControl = true;
        } else if (!this.preTurnActions.isEmpty()) {
            this.currentAction = this.preTurnActions.remove(0);
            this.phase = Phase.EXECUTING_ACTIONS;
            this.hasControl = true;
        } else if (!this.cardQueue.isEmpty()) {
            AbstractRelic top;
            this.usingCard = true;
            AbstractCard c = this.cardQueue.get((int)0).card;
            if (c == null) {
                this.callEndOfTurnActions();
            } else if (c.equals(this.lastCard)) {
                logger.info("Last card! " + c.name);
                this.lastCard = null;
            }
            if (this.cardQueue.size() == 1 && this.cardQueue.get((int)0).isEndTurnAutoPlay && (top = AbstractDungeon.player.getRelic("Unceasing Top")) != null) {
                ((UnceasingTop)top).disableUntilTurnEnds();
            }
            boolean canPlayCard = false;
            if (c != null) {
                c.isInAutoplay = this.cardQueue.get((int)0).autoplayCard;
            }
            if (c != null && this.cardQueue.get((int)0).randomTarget) {
                this.cardQueue.get((int)0).monster = AbstractDungeon.getMonsters().getRandomMonster(null, true, AbstractDungeon.cardRandomRng);
            }
            if (this.cardQueue.get((int)0).card != null && (c.canUse(AbstractDungeon.player, this.cardQueue.get((int)0).monster) || this.cardQueue.get((int)0).card.dontTriggerOnUseCard)) {
                canPlayCard = true;
                if (c.freeToPlay()) {
                    c.freeToPlayOnce = true;
                }
                this.cardQueue.get((int)0).card.energyOnUse = this.cardQueue.get((int)0).energyOnUse;
                this.cardQueue.get((int)0).card.ignoreEnergyOnUse = c.isInAutoplay ? true : this.cardQueue.get((int)0).ignoreEnergyTotal;
                if (!this.cardQueue.get((int)0).card.dontTriggerOnUseCard) {
                    for (AbstractPower abstractPower : AbstractDungeon.player.powers) {
                        abstractPower.onPlayCard(this.cardQueue.get((int)0).card, this.cardQueue.get((int)0).monster);
                    }
                    for (AbstractMonster abstractMonster : AbstractDungeon.getMonsters().monsters) {
                        for (AbstractPower p : abstractMonster.powers) {
                            p.onPlayCard(this.cardQueue.get((int)0).card, this.cardQueue.get((int)0).monster);
                        }
                    }
                    for (AbstractRelic abstractRelic : AbstractDungeon.player.relics) {
                        abstractRelic.onPlayCard(this.cardQueue.get((int)0).card, this.cardQueue.get((int)0).monster);
                    }
                    AbstractDungeon.player.stance.onPlayCard(this.cardQueue.get((int)0).card);
                    for (AbstractBlight abstractBlight : AbstractDungeon.player.blights) {
                        abstractBlight.onPlayCard(this.cardQueue.get((int)0).card, this.cardQueue.get((int)0).monster);
                    }
                    for (AbstractCard abstractCard : AbstractDungeon.player.hand.group) {
                        abstractCard.onPlayCard(this.cardQueue.get((int)0).card, this.cardQueue.get((int)0).monster);
                    }
                    for (AbstractCard abstractCard : AbstractDungeon.player.discardPile.group) {
                        abstractCard.onPlayCard(this.cardQueue.get((int)0).card, this.cardQueue.get((int)0).monster);
                    }
                    for (AbstractCard abstractCard : AbstractDungeon.player.drawPile.group) {
                        abstractCard.onPlayCard(this.cardQueue.get((int)0).card, this.cardQueue.get((int)0).monster);
                    }
                    ++AbstractDungeon.player.cardsPlayedThisTurn;
                    this.cardsPlayedThisTurn.add(this.cardQueue.get((int)0).card);
                    this.cardsPlayedThisCombat.add(this.cardQueue.get((int)0).card);
                }
                if (this.cardsPlayedThisTurn.size() == 25) {
                    UnlockTracker.unlockAchievement("INFINITY");
                }
                if (this.cardsPlayedThisTurn.size() >= 20 && !CardCrawlGame.combo) {
                    CardCrawlGame.combo = true;
                }
                if (this.cardQueue.get((int)0).card instanceof Shiv) {
                    int shivCount = 0;
                    for (AbstractCard i : this.cardsPlayedThisTurn) {
                        if (!(i instanceof Shiv) || ++shivCount != 10) continue;
                        UnlockTracker.unlockAchievement("NINJA");
                        break;
                    }
                }
                if (this.cardQueue.get((int)0).card != null) {
                    if (this.cardQueue.get((int)0).card.target == AbstractCard.CardTarget.ENEMY && (this.cardQueue.get((int)0).monster == null || this.cardQueue.get((int)0).monster.isDeadOrEscaped())) {
                        Iterator<AbstractCard> i = AbstractDungeon.player.limbo.group.iterator();
                        while (i.hasNext()) {
                            AbstractCard abstractCard = i.next();
                            if (abstractCard != this.cardQueue.get((int)0).card) continue;
                            this.cardQueue.get((int)0).card.fadingOut = true;
                            AbstractDungeon.effectList.add(new ExhaustCardEffect(this.cardQueue.get((int)0).card));
                            i.remove();
                        }
                        if (this.cardQueue.get((int)0).monster == null) {
                            this.cardQueue.get((int)0).card.drawScale = this.cardQueue.get((int)0).card.targetDrawScale;
                            this.cardQueue.get((int)0).card.angle = this.cardQueue.get((int)0).card.targetAngle;
                            this.cardQueue.get((int)0).card.current_x = this.cardQueue.get((int)0).card.target_x;
                            this.cardQueue.get((int)0).card.current_y = this.cardQueue.get((int)0).card.target_y;
                            AbstractDungeon.effectList.add(new ExhaustCardEffect(this.cardQueue.get((int)0).card));
                        }
                    } else {
                        AbstractDungeon.player.useCard(this.cardQueue.get((int)0).card, this.cardQueue.get((int)0).monster, this.cardQueue.get((int)0).energyOnUse);
                    }
                }
            } else {
                Iterator<AbstractCard> i = AbstractDungeon.player.limbo.group.iterator();
                while (i.hasNext()) {
                    AbstractCard abstractCard = i.next();
                    if (abstractCard != c) continue;
                    c.fadingOut = true;
                    AbstractDungeon.effectList.add(new ExhaustCardEffect(c));
                    i.remove();
                }
                if (c != null) {
                    AbstractDungeon.effectList.add(new ThoughtBubble(AbstractDungeon.player.dialogX, AbstractDungeon.player.dialogY, 3.0f, c.cantUseMessage, true));
                }
            }
            this.cardQueue.remove(0);
            if (!canPlayCard && c != null && c.isInAutoplay) {
                c.dontTriggerOnUseCard = true;
                AbstractDungeon.actionManager.addToBottom(new UseCardAction(c));
            }
        } else if (!this.monsterAttacksQueued) {
            this.monsterAttacksQueued = true;
            if (!AbstractDungeon.getCurrRoom().skipMonsterTurn) {
                AbstractDungeon.getCurrRoom().monsters.queueMonsters();
            }
        } else if (!this.monsterQueue.isEmpty()) {
            AbstractMonster m = this.monsterQueue.get((int)0).monster;
            if (!m.isDeadOrEscaped() || m.halfDead) {
                if (m.intent != AbstractMonster.Intent.NONE) {
                    this.addToBottom(new ShowMoveNameAction(m));
                    this.addToBottom(new IntentFlashAction(m));
                }
                if (!(TipTracker.tips.get("INTENT_TIP").booleanValue() || AbstractDungeon.player.currentBlock != 0 || m.intent != AbstractMonster.Intent.ATTACK && m.intent != AbstractMonster.Intent.ATTACK_DEBUFF && m.intent != AbstractMonster.Intent.ATTACK_BUFF && m.intent != AbstractMonster.Intent.ATTACK_DEFEND)) {
                    if (AbstractDungeon.floorNum <= 5) {
                        ++TipTracker.blockCounter;
                    } else {
                        TipTracker.neverShowAgain("INTENT_TIP");
                    }
                }
                m.takeTurn();
                m.applyTurnPowers();
            }
            this.monsterQueue.remove(0);
            if (this.monsterQueue.isEmpty()) {
                this.addToBottom(new WaitAction(1.5f));
            }
        } else if (this.turnHasEnded && !AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            if (!AbstractDungeon.getCurrRoom().skipMonsterTurn) {
                AbstractDungeon.getCurrRoom().monsters.applyEndOfTurnPowers();
            }
            AbstractDungeon.player.cardsPlayedThisTurn = 0;
            this.orbsChanneledThisTurn.clear();
            if (ModHelper.isModEnabled("Careless")) {
                Careless.modAction();
            }
            if (ModHelper.isModEnabled("ControlledChaos")) {
                ControlledChaos.modAction();
                AbstractDungeon.player.hand.applyPowers();
            }
            AbstractDungeon.player.applyStartOfTurnRelics();
            AbstractDungeon.player.applyStartOfTurnPreDrawCards();
            AbstractDungeon.player.applyStartOfTurnCards();
            AbstractDungeon.player.applyStartOfTurnPowers();
            AbstractDungeon.player.applyStartOfTurnOrbs();
            ++turn;
            AbstractDungeon.getCurrRoom().skipMonsterTurn = false;
            this.turnHasEnded = false;
            totalDiscardedThisTurn = 0;
            this.cardsPlayedThisTurn.clear();
            damageReceivedThisTurn = 0;
            if (!AbstractDungeon.player.hasPower("Barricade") && !AbstractDungeon.player.hasPower("Blur")) {
                if (!AbstractDungeon.player.hasRelic("Calipers")) {
                    AbstractDungeon.player.loseBlock();
                } else {
                    AbstractDungeon.player.loseBlock(15);
                }
            }
            if (!AbstractDungeon.getCurrRoom().isBattleOver) {
                this.addToBottom(new DrawCardAction(null, AbstractDungeon.player.gameHandSize, true));
                AbstractDungeon.player.applyStartOfTurnPostDrawRelics();
                AbstractDungeon.player.applyStartOfTurnPostDrawPowers();
                this.addToBottom(new EnableEndTurnButtonAction());
            }
        }
    }

    private void callEndOfTurnActions() {
        AbstractDungeon.getCurrRoom().applyEndOfTurnRelics();
        AbstractDungeon.getCurrRoom().applyEndOfTurnPreCardPowers();
        this.addToBottom(new TriggerEndOfTurnOrbsAction());
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            c.triggerOnEndOfTurnForPlayingCard();
        }
        AbstractDungeon.player.stance.onEndOfTurn();
    }

    public void callEndTurnEarlySequence() {
        for (CardQueueItem i : AbstractDungeon.actionManager.cardQueue) {
            if (!i.autoplayCard) continue;
            i.card.dontTriggerOnUseCard = true;
            AbstractDungeon.actionManager.addToBottom(new UseCardAction(i.card));
        }
        AbstractDungeon.actionManager.cardQueue.clear();
        for (AbstractCard c : AbstractDungeon.player.limbo.group) {
            AbstractDungeon.effectList.add(new ExhaustCardEffect(c));
        }
        AbstractDungeon.player.limbo.group.clear();
        AbstractDungeon.player.releaseCard();
        AbstractDungeon.overlayMenu.endTurnButton.disable(true);
    }

    public void cleanCardQueue() {
        Iterator<Object> i = this.cardQueue.iterator();
        while (i.hasNext()) {
            CardQueueItem cardQueueItem = i.next();
            if (!AbstractDungeon.player.hand.contains(cardQueueItem.card)) continue;
            i.remove();
        }
        for (AbstractCard abstractCard : AbstractDungeon.player.limbo.group) {
            abstractCard.fadingOut = true;
        }
    }

    public boolean isEmpty() {
        return this.actions.isEmpty();
    }

    public void clearNextRoomCombatActions() {
        this.nextCombatActions.clear();
    }

    public void clear() {
        this.actions.clear();
        this.preTurnActions.clear();
        this.currentAction = null;
        this.previousAction = null;
        this.turnStartCurrentAction = null;
        this.cardsPlayedThisCombat.clear();
        this.cardsPlayedThisTurn.clear();
        this.orbsChanneledThisCombat.clear();
        this.orbsChanneledThisTurn.clear();
        this.uniqueStancesThisCombat.clear();
        this.cardQueue.clear();
        energyGainedThisCombat = 0;
        this.mantraGained = 0;
        damageReceivedThisCombat = 0;
        damageReceivedThisTurn = 0;
        hpLossThisCombat = 0;
        this.turnHasEnded = false;
        turn = 1;
        this.phase = Phase.WAITING_ON_USER;
        totalDiscardedThisTurn = 0;
    }

    public static void incrementDiscard(boolean endOfTurn) {
        ++totalDiscardedThisTurn;
        if (!AbstractDungeon.actionManager.turnHasEnded && !endOfTurn) {
            AbstractDungeon.player.updateCardsOnDiscard();
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onManualDiscard();
            }
        }
    }

    public void updateEnergyGain(int energyGain) {
        energyGainedThisCombat += energyGain;
    }

    public static void queueExtraCard(AbstractCard card, AbstractMonster m) {
        AbstractCard tmp = card.makeSameInstanceOf();
        AbstractDungeon.player.limbo.addToBottom(tmp);
        tmp.current_x = card.current_x;
        tmp.current_y = card.current_y;
        int extraCount = 0;
        for (CardQueueItem c : AbstractDungeon.actionManager.cardQueue) {
            if (!c.card.uuid.equals(card.uuid)) continue;
            ++extraCount;
        }
        tmp.target_y = (float)Settings.HEIGHT / 2.0f;
        switch (extraCount) {
            case 0: {
                tmp.target_x = (float)Settings.WIDTH / 2.0f - 300.0f * Settings.xScale;
                break;
            }
            case 1: {
                tmp.target_x = (float)Settings.WIDTH / 2.0f + 300.0f * Settings.xScale;
                break;
            }
            case 2: {
                tmp.target_x = (float)Settings.WIDTH / 2.0f - 600.0f * Settings.xScale;
                break;
            }
            case 3: {
                tmp.target_x = (float)Settings.WIDTH / 2.0f + 600.0f * Settings.xScale;
                break;
            }
            default: {
                tmp.target_x = MathUtils.random((float)Settings.WIDTH * 0.2f, (float)Settings.WIDTH * 0.8f);
                tmp.target_y = MathUtils.random((float)Settings.HEIGHT * 0.3f, (float)Settings.HEIGHT * 0.7f);
            }
        }
        if (m != null) {
            tmp.calculateCardDamage(m);
        }
        tmp.purgeOnUse = true;
        AbstractDungeon.actionManager.addCardQueueItem(new CardQueueItem(tmp, m, card.energyOnUse, true, true), true);
    }

    static {
        turn = 0;
    }

    public static enum Phase {
        WAITING_ON_USER,
        EXECUTING_ACTIONS;

    }
}


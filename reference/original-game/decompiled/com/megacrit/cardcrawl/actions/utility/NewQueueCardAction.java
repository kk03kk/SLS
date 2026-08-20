/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NewQueueCardAction
extends AbstractGameAction {
    private AbstractCard card;
    private boolean randomTarget;
    private boolean immediateCard;
    private boolean autoplayCard;
    private static final Logger logger = LogManager.getLogger(NewQueueCardAction.class.getName());

    public NewQueueCardAction(AbstractCard card, AbstractCreature target, boolean immediateCard, boolean autoplayCard) {
        this.card = card;
        this.target = target;
        this.immediateCard = immediateCard;
        this.autoplayCard = autoplayCard;
        this.randomTarget = false;
    }

    public NewQueueCardAction(AbstractCard card, AbstractCreature target, boolean immediateCard) {
        this(card, target, immediateCard, false);
    }

    public NewQueueCardAction(AbstractCard card, AbstractCreature target) {
        this(card, target, false);
    }

    public NewQueueCardAction(AbstractCard card, boolean randomTarget, boolean immediateCard, boolean autoplayCard) {
        this(card, null, immediateCard, autoplayCard);
        this.randomTarget = randomTarget;
    }

    public NewQueueCardAction(AbstractCard card, boolean randomTarget, boolean immediateCard) {
        this(card, randomTarget, immediateCard, false);
    }

    public NewQueueCardAction(AbstractCard card, boolean randomTarget) {
        this(card, randomTarget, false);
    }

    public NewQueueCardAction() {
        this(null, null);
    }

    @Override
    public void update() {
        if (this.card == null) {
            if (!this.queueContainsEndTurnCard()) {
                AbstractDungeon.actionManager.cardQueue.add(new CardQueueItem());
            }
        } else if (!this.queueContains(this.card)) {
            if (this.randomTarget) {
                AbstractDungeon.actionManager.addCardQueueItem(new CardQueueItem(this.card, true, EnergyPanel.getCurrentEnergy(), false, this.autoplayCard), this.immediateCard);
            } else {
                if (!(this.target instanceof AbstractMonster) && this.target != null) {
                    logger.info("WARNING: NewQueueCardAction does not contain an AbstractMonster!");
                    this.isDone = true;
                    return;
                }
                AbstractDungeon.actionManager.addCardQueueItem(new CardQueueItem(this.card, (AbstractMonster)this.target, EnergyPanel.getCurrentEnergy(), false, this.autoplayCard), this.immediateCard);
            }
        }
        this.isDone = true;
    }

    private boolean queueContains(AbstractCard card) {
        for (CardQueueItem i : AbstractDungeon.actionManager.cardQueue) {
            if (i.card != card) continue;
            return true;
        }
        return false;
    }

    private boolean queueContainsEndTurnCard() {
        for (CardQueueItem i : AbstractDungeon.actionManager.cardQueue) {
            if (i.card != null) continue;
            return true;
        }
        return false;
    }
}


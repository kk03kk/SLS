/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

public class CardQueueItem {
    public AbstractCard card;
    public AbstractMonster monster;
    public int energyOnUse = 0;
    public boolean ignoreEnergyTotal = false;
    public boolean autoplayCard = false;
    public boolean randomTarget = false;
    public boolean isEndTurnAutoPlay = false;

    public CardQueueItem() {
        this.card = null;
        this.monster = null;
    }

    public CardQueueItem(AbstractCard card, boolean isEndTurnAutoPlay) {
        this(card, null);
        this.isEndTurnAutoPlay = isEndTurnAutoPlay;
    }

    public CardQueueItem(AbstractCard card, AbstractMonster monster) {
        this(card, monster, EnergyPanel.getCurrentEnergy(), false);
    }

    public CardQueueItem(AbstractCard card, AbstractMonster monster, int setEnergyOnUse) {
        this(card, monster, setEnergyOnUse, false);
    }

    public CardQueueItem(AbstractCard card, AbstractMonster monster, int setEnergyOnUse, boolean ignoreEnergyTotal) {
        this(card, monster, setEnergyOnUse, ignoreEnergyTotal, false);
    }

    public CardQueueItem(AbstractCard card, AbstractMonster monster, int setEnergyOnUse, boolean ignoreEnergyTotal, boolean autoplayCard) {
        this.card = card;
        this.monster = monster;
        this.energyOnUse = setEnergyOnUse;
        this.ignoreEnergyTotal = ignoreEnergyTotal;
        this.autoplayCard = autoplayCard;
    }

    public CardQueueItem(AbstractCard card, boolean randomTarget, int setEnergyOnUse, boolean ignoreEnergyTotal, boolean autoplayCard) {
        this(card, null, setEnergyOnUse, ignoreEnergyTotal, autoplayCard);
        this.randomTarget = randomTarget;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.cards.ironclad;

import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class EvolveUnlock
extends AbstractUnlock {
    public EvolveUnlock() {
        this.type = AbstractUnlock.UnlockType.CARD;
        this.card = CardLibrary.getCard("Evolve");
        this.key = this.card.cardID;
        this.title = this.card.name;
    }
}


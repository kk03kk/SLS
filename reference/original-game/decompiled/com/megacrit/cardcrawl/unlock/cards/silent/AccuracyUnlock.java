/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.cards.silent;

import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class AccuracyUnlock
extends AbstractUnlock {
    public AccuracyUnlock() {
        this.type = AbstractUnlock.UnlockType.CARD;
        this.card = CardLibrary.getCard("Accuracy");
        this.key = this.card.cardID;
        this.title = this.card.name;
    }
}


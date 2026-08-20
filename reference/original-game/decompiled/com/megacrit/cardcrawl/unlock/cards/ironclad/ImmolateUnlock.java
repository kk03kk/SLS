/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.cards.ironclad;

import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class ImmolateUnlock
extends AbstractUnlock {
    public ImmolateUnlock() {
        this.type = AbstractUnlock.UnlockType.CARD;
        this.card = CardLibrary.getCard("Immolate");
        this.key = this.card.cardID;
        this.title = this.card.name;
    }
}


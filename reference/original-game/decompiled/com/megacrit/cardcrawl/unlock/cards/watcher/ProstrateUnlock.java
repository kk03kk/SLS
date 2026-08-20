/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.cards.watcher;

import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class ProstrateUnlock
extends AbstractUnlock {
    public ProstrateUnlock() {
        this.type = AbstractUnlock.UnlockType.CARD;
        this.card = CardLibrary.getCard("Prostrate");
        this.key = this.card.cardID;
        this.title = this.card.name;
    }
}


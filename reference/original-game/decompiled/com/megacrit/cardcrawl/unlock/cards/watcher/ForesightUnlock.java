/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.cards.watcher;

import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class ForesightUnlock
extends AbstractUnlock {
    public ForesightUnlock() {
        this.type = AbstractUnlock.UnlockType.CARD;
        this.card = CardLibrary.getCard("Wireheading");
        this.key = this.card.cardID;
        this.title = this.card.name;
    }
}


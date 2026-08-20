/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.unlock.cards.defect;

import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;

public class MeteorStrikeUnlock
extends AbstractUnlock {
    public MeteorStrikeUnlock() {
        this.type = AbstractUnlock.UnlockType.CARD;
        this.card = CardLibrary.getCard("Meteor Strike");
        this.key = this.card.cardID;
        this.title = this.card.name;
    }
}


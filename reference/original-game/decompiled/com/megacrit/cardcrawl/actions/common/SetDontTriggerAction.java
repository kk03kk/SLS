/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;

public class SetDontTriggerAction
extends AbstractGameAction {
    private AbstractCard card;
    private boolean trigger;

    public SetDontTriggerAction(AbstractCard card, boolean dontTrigger) {
        this.card = card;
        this.trigger = dontTrigger;
    }

    @Override
    public void update() {
        this.card.dontTriggerOnUseCard = this.trigger;
        this.isDone = true;
    }
}


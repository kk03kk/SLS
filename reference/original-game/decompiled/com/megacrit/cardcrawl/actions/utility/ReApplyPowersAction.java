/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class ReApplyPowersAction
extends AbstractGameAction {
    private AbstractCard card;
    private AbstractMonster m;

    public ReApplyPowersAction(AbstractCard card, AbstractMonster m) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.card = card;
        this.m = m;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            this.card.calculateCardDamage(this.m);
            this.isDone = true;
        }
    }
}


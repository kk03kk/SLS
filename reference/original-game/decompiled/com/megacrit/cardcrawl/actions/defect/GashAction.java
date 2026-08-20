/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.blue.Claw;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class GashAction
extends AbstractGameAction {
    private AbstractCard card;

    public GashAction(AbstractCard card, int amount) {
        this.card = card;
        this.amount = amount;
    }

    @Override
    public void update() {
        this.card.baseDamage += this.amount;
        this.card.applyPowers();
        for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
            if (!(c instanceof Claw)) continue;
            c.baseDamage += this.amount;
            c.applyPowers();
        }
        for (AbstractCard c : AbstractDungeon.player.drawPile.group) {
            if (!(c instanceof Claw)) continue;
            c.baseDamage += this.amount;
            c.applyPowers();
        }
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (!(c instanceof Claw)) continue;
            c.baseDamage += this.amount;
            c.applyPowers();
        }
        this.isDone = true;
    }
}


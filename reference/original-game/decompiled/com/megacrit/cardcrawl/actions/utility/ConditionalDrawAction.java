/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class ConditionalDrawAction
extends AbstractGameAction {
    private AbstractCard.CardType restrictedType;

    public ConditionalDrawAction(int newAmount, AbstractCard.CardType restrictedType) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.source = AbstractDungeon.player;
        this.target = AbstractDungeon.player;
        this.amount = newAmount;
        this.restrictedType = restrictedType;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (this.checkCondition()) {
                this.addToTop(new DrawCardAction(this.source, this.amount));
            }
            this.isDone = true;
        }
    }

    private boolean checkCondition() {
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (c.type != this.restrictedType) continue;
            return false;
        }
        return true;
    }
}


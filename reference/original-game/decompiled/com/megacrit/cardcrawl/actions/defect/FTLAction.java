/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class FTLAction
extends AbstractGameAction {
    private DamageInfo info;
    private AbstractCreature target;
    private int cardPlayCount = 0;

    public FTLAction(AbstractCreature target, DamageInfo info, int cardPlayCount) {
        this.info = info;
        this.target = target;
        this.cardPlayCount = cardPlayCount;
    }

    @Override
    public void update() {
        this.addToBot(new DamageAction(this.target, this.info, AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
        if (AbstractDungeon.actionManager.cardsPlayedThisTurn.size() - 1 < this.cardPlayCount) {
            this.addToTop(new DrawCardAction(AbstractDungeon.player, 1));
        }
        this.isDone = true;
    }
}


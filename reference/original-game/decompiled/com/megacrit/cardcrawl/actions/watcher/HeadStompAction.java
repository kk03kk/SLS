/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.WeakPower;

public class HeadStompAction
extends AbstractGameAction {
    private AbstractMonster m;
    private int magicNumber;

    public HeadStompAction(AbstractMonster monster, int vulnAmount) {
        this.m = monster;
        this.magicNumber = vulnAmount;
    }

    @Override
    public void update() {
        if (AbstractDungeon.actionManager.cardsPlayedThisCombat.size() >= 2 && AbstractDungeon.actionManager.cardsPlayedThisCombat.get((int)(AbstractDungeon.actionManager.cardsPlayedThisCombat.size() - 2)).type == AbstractCard.CardType.ATTACK) {
            this.addToTop(new ApplyPowerAction(this.m, AbstractDungeon.player, new WeakPower(this.m, this.magicNumber, false), this.magicNumber));
        }
        this.isDone = true;
    }
}


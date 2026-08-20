/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class DamagePerAttackPlayedAction
extends AbstractGameAction {
    private DamageInfo info;

    public DamagePerAttackPlayedAction(AbstractCreature target, DamageInfo info, AbstractGameAction.AttackEffect effect) {
        this.info = info;
        this.setValues(target, info);
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.attackEffect = effect;
    }

    public DamagePerAttackPlayedAction(AbstractCreature target, DamageInfo info) {
        this(target, info, AbstractGameAction.AttackEffect.NONE);
    }

    @Override
    public void update() {
        this.isDone = true;
        if (this.target != null && this.target.currentHealth > 0) {
            int count = 0;
            for (AbstractCard c : AbstractDungeon.actionManager.cardsPlayedThisTurn) {
                if (c.type != AbstractCard.CardType.ATTACK) continue;
                ++count;
            }
            --count;
            for (int i = 0; i < count; ++i) {
                this.addToTop(new DamageAction(this.target, this.info, this.attackEffect));
            }
        }
    }
}


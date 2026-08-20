/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.ExhaustAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class FiendFireAction
extends AbstractGameAction {
    private DamageInfo info;
    private float startingDuration;

    public FiendFireAction(AbstractCreature target, DamageInfo info) {
        this.info = info;
        this.setValues(target, info);
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.attackEffect = AbstractGameAction.AttackEffect.FIRE;
        this.duration = this.startingDuration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        int i;
        int count = AbstractDungeon.player.hand.size();
        for (i = 0; i < count; ++i) {
            this.addToTop(new DamageAction(this.target, this.info, AbstractGameAction.AttackEffect.FIRE));
        }
        for (i = 0; i < count; ++i) {
            if (Settings.FAST_MODE) {
                this.addToTop(new ExhaustAction(1, true, true, false, Settings.ACTION_DUR_XFAST));
                continue;
            }
            this.addToTop(new ExhaustAction(1, true, true));
        }
        this.isDone = true;
    }
}


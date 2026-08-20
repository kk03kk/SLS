/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class MindBlastAction
extends AbstractGameAction {
    public MindBlastAction(AbstractCreature target) {
        this.setValues(target, AbstractDungeon.player);
        this.duration = Settings.ACTION_DUR_FAST;
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST && this.target != null) {
            DamageInfo info = new DamageInfo(this.source, AbstractDungeon.player.drawPile.size());
            info.applyPowers(this.source, this.target);
            this.addToTop(new DamageAction(this.target, info, AbstractGameAction.AttackEffect.NONE));
        }
        this.tickDuration();
    }
}


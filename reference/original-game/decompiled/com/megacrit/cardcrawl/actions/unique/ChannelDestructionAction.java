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

public class ChannelDestructionAction
extends AbstractGameAction {
    public ChannelDestructionAction(AbstractCreature target) {
        this.target = target;
        this.source = AbstractDungeon.player;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            DamageInfo info = new DamageInfo(this.source, AbstractDungeon.player.hand.size() * 2);
            info.applyPowers(AbstractDungeon.player, this.target);
            this.addToTop(new DamageAction(this.target, info, AbstractGameAction.AttackEffect.BLUNT_HEAVY));
        }
        this.tickDuration();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.defect.LightningOrbEvokeAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class MasterRealityAction
extends AbstractGameAction {
    private static final float DURATION = 0.01f;

    public MasterRealityAction(int damageAmount) {
        this.amount = damageAmount;
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.attackEffect = AbstractGameAction.AttackEffect.NONE;
        this.duration = 0.01f;
    }

    @Override
    public void update() {
        if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
            AbstractDungeon.actionManager.clearPostCombatActions();
            this.isDone = true;
            return;
        }
        int count = 0;
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (!c.selfRetain && !c.retain) continue;
            ++count;
        }
        for (int i = 0; i < count; ++i) {
            this.addToTop(new LightningOrbEvokeAction(new DamageInfo(AbstractDungeon.player, this.amount, DamageInfo.DamageType.THORNS), false));
        }
        this.isDone = true;
    }
}


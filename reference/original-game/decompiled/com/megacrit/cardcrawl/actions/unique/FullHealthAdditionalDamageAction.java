/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class FullHealthAdditionalDamageAction
extends AbstractGameAction {
    private DamageInfo info;
    private static final int ADDITIONAL_DAMAGE = 6;

    public FullHealthAdditionalDamageAction(AbstractCreature target, DamageInfo info) {
        this.info = info;
        this.setValues(target, info);
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
    }

    public FullHealthAdditionalDamageAction(AbstractCreature target, AbstractCreature source, int damage) {
        this(target, source, damage, DamageInfo.DamageType.NORMAL);
    }

    public FullHealthAdditionalDamageAction(AbstractCreature target, AbstractCreature source, int damage, DamageInfo.DamageType type) {
        this.info = new DamageInfo(source, damage, type);
        this.setValues(target, this.info);
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
    }

    @Override
    public void update() {
        if (this.duration == 0.5f) {
            if (this.target.currentHealth != this.target.maxHealth) {
                this.target.damage(this.info);
            } else {
                this.target.damage(new DamageInfo(this.info.owner, this.info.output + 6, this.info.type));
            }
            if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
                AbstractDungeon.actionManager.clearPostCombatActions();
            }
        }
        this.tickDuration();
    }
}


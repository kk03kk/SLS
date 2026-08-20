/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.VulnerablePower;

public class IndignationAction
extends AbstractGameAction {
    public IndignationAction(int amount) {
        this.amount = amount;
    }

    @Override
    public void update() {
        if (AbstractDungeon.player.stance.ID.equals("Wrath")) {
            for (AbstractMonster mo : AbstractDungeon.getCurrRoom().monsters.monsters) {
                this.addToBot(new ApplyPowerAction(mo, AbstractDungeon.player, new VulnerablePower(mo, this.amount, false), this.amount, true, AbstractGameAction.AttackEffect.NONE));
            }
        } else {
            this.addToBot(new ChangeStanceAction("Wrath"));
        }
        this.isDone = true;
    }
}


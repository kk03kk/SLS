/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class RemoveAllPowersAction
extends AbstractGameAction {
    private boolean debuffsOnly;
    private AbstractCreature c;

    public RemoveAllPowersAction(AbstractCreature c, boolean debuffsOnly) {
        this.debuffsOnly = debuffsOnly;
        this.c = c;
        this.duration = 0.5f;
    }

    @Override
    public void update() {
        for (AbstractPower p : this.c.powers) {
            if (p.type != AbstractPower.PowerType.DEBUFF && this.debuffsOnly) continue;
            this.addToTop(new RemoveSpecificPowerAction(this.c, this.c, p.ID));
        }
        this.isDone = true;
    }
}


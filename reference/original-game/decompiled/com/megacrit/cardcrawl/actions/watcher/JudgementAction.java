/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.InstantKillAction;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class JudgementAction
extends AbstractGameAction {
    private int cutoff;

    public JudgementAction(AbstractCreature target, int cutoff) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.source = null;
        this.target = target;
        this.cutoff = cutoff;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST && this.target.currentHealth <= this.cutoff && this.target instanceof AbstractMonster) {
            this.addToTop(new InstantKillAction(this.target));
        }
        this.isDone = true;
    }
}


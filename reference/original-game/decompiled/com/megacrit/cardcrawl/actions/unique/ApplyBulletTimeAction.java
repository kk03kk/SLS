/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class ApplyBulletTimeAction
extends AbstractGameAction {
    public ApplyBulletTimeAction() {
        this.duration = Settings.ACTION_DUR_XFAST;
    }

    @Override
    public void update() {
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            c.setCostForTurn(-9);
        }
        this.isDone = true;
    }
}


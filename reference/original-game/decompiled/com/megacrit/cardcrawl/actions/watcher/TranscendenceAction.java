/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class TranscendenceAction
extends AbstractGameAction {
    public TranscendenceAction() {
        this.duration = Settings.ACTION_DUR_MED;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_MED) {
            AbstractPlayer p = AbstractDungeon.player;
            for (AbstractCard c : p.hand.group) {
                if (!c.canUpgrade() || !c.retain && !c.selfRetain) continue;
                c.superFlash();
                c.upgrade();
                c.applyPowers();
            }
            this.isDone = true;
        }
    }
}


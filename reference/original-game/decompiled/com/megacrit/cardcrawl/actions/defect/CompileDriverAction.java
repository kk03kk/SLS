/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import java.util.ArrayList;

public class CompileDriverAction
extends AbstractGameAction {
    public CompileDriverAction(AbstractPlayer source, int amount) {
        this.setValues(this.target, source, amount);
        this.actionType = AbstractGameAction.ActionType.WAIT;
    }

    @Override
    public void update() {
        ArrayList<String> orbList = new ArrayList<String>();
        for (AbstractOrb o : AbstractDungeon.player.orbs) {
            if (o.ID == null || o.ID.equals("Empty") || orbList.contains(o.ID)) continue;
            orbList.add(o.ID);
        }
        int toDraw = orbList.size() * this.amount;
        if (toDraw > 0) {
            this.addToTop(new DrawCardAction(this.source, toDraw));
        }
        this.isDone = true;
    }
}


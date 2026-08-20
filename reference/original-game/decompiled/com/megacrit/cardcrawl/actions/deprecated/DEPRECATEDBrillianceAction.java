/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.deprecated;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.TransformCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Miracle;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.ArrayList;

public class DEPRECATEDBrillianceAction
extends AbstractGameAction {
    @Override
    public void update() {
        ArrayList<AbstractCard> g = AbstractDungeon.player.hand.group;
        for (int i = 0; i < g.size(); ++i) {
            if (g.get((int)i).cardID.equals("Miracle")) continue;
            this.addToBot(new TransformCardInHandAction(i, new Miracle()));
        }
        this.isDone = true;
    }
}


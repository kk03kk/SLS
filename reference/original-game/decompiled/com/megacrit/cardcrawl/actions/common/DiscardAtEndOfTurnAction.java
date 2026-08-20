/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DiscardAction;
import com.megacrit.cardcrawl.actions.unique.RestoreRetainedCardsAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class DiscardAtEndOfTurnAction
extends AbstractGameAction {
    private static final float DURATION = Settings.ACTION_DUR_XFAST;

    public DiscardAtEndOfTurnAction() {
        this.duration = DURATION;
    }

    @Override
    public void update() {
        if (this.duration == DURATION) {
            Iterator<AbstractCard> c = AbstractDungeon.player.hand.group.iterator();
            while (c.hasNext()) {
                AbstractCard e = c.next();
                if (!e.retain && !e.selfRetain) continue;
                AbstractDungeon.player.limbo.addToTop(e);
                c.remove();
            }
            this.addToTop(new RestoreRetainedCardsAction(AbstractDungeon.player.limbo));
            if (!AbstractDungeon.player.hasRelic("Runic Pyramid") && !AbstractDungeon.player.hasPower("Equilibrium")) {
                int tempSize = AbstractDungeon.player.hand.size();
                for (int i = 0; i < tempSize; ++i) {
                    this.addToTop(new DiscardAction(AbstractDungeon.player, null, AbstractDungeon.player.hand.size(), true, true));
                }
            }
            ArrayList cards = (ArrayList)AbstractDungeon.player.hand.group.clone();
            Collections.shuffle(cards);
            for (AbstractCard c2 : cards) {
                c2.triggerOnEndOfPlayerTurn();
            }
            this.isDone = true;
        }
    }
}


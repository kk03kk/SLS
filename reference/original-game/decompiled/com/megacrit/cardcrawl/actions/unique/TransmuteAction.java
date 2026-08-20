/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class TransmuteAction
extends AbstractGameAction {
    private AbstractPlayer p;

    public TransmuteAction() {
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.p = AbstractDungeon.player;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            AbstractDungeon.actionManager.cleanCardQueue();
            if (this.p.hand.group.isEmpty()) {
                this.isDone = true;
                return;
            }
            CardGroup tmp = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
            tmp.group.addAll(this.p.hand.group);
            this.p.hand.clear();
            for (AbstractCard c : tmp.group) {
                AbstractDungeon.transformCard(c);
                AbstractCard transformedCard = AbstractDungeon.getTransformedCard();
                this.p.hand.addToTop(transformedCard);
            }
            this.tickDuration();
            return;
        }
        this.p.hand.refreshHandLayout();
        this.isDone = true;
    }
}


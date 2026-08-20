/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ExhaustSpecificCardAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.util.ArrayList;

public class BlockPerNonAttackAction
extends AbstractGameAction {
    private int blockPerCard;

    public BlockPerNonAttackAction(int blockAmount) {
        this.blockPerCard = blockAmount;
        this.setValues((AbstractCreature)AbstractDungeon.player, AbstractDungeon.player);
        this.actionType = AbstractGameAction.ActionType.BLOCK;
    }

    @Override
    public void update() {
        ArrayList<AbstractCard> cardsToExhaust = new ArrayList<AbstractCard>();
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (c.type == AbstractCard.CardType.ATTACK) continue;
            cardsToExhaust.add(c);
        }
        for (AbstractCard c : cardsToExhaust) {
            this.addToTop(new GainBlockAction((AbstractCreature)AbstractDungeon.player, AbstractDungeon.player, this.blockPerCard));
        }
        for (AbstractCard c : cardsToExhaust) {
            this.addToTop(new ExhaustSpecificCardAction(c, AbstractDungeon.player.hand));
        }
        this.isDone = true;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.deprecated;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class DEPRECATEDExperiencedAction
extends AbstractGameAction {
    private int blockPerCard;
    private AbstractCard card;

    public DEPRECATEDExperiencedAction(int blockPerCard, AbstractCard card) {
        this.blockPerCard = blockPerCard;
        this.card = card;
    }

    @Override
    public void update() {
        int upgradeCount = 0;
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (!c.upgraded || c == this.card) continue;
            ++upgradeCount;
        }
        this.addToTop(new GainBlockAction((AbstractCreature)AbstractDungeon.player, AbstractDungeon.player, upgradeCount * this.blockPerCard));
        this.isDone = true;
    }
}


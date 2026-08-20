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

public class ApotheosisAction
extends AbstractGameAction {
    public ApotheosisAction() {
        this.duration = Settings.ACTION_DUR_MED;
        this.actionType = AbstractGameAction.ActionType.WAIT;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_MED) {
            AbstractPlayer p = AbstractDungeon.player;
            this.upgradeAllCardsInGroup(p.hand);
            this.upgradeAllCardsInGroup(p.drawPile);
            this.upgradeAllCardsInGroup(p.discardPile);
            this.upgradeAllCardsInGroup(p.exhaustPile);
            this.isDone = true;
        }
    }

    private void upgradeAllCardsInGroup(CardGroup cardGroup) {
        for (AbstractCard c : cardGroup.group) {
            if (!c.canUpgrade()) continue;
            if (cardGroup.type == CardGroup.CardGroupType.HAND) {
                c.superFlash();
            }
            c.upgrade();
            c.applyPowers();
        }
    }
}


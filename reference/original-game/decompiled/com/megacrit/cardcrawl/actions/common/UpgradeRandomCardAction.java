/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class UpgradeRandomCardAction
extends AbstractGameAction {
    private AbstractPlayer p;

    public UpgradeRandomCardAction() {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.p = AbstractDungeon.player;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (this.p.hand.group.size() <= 0) {
                this.isDone = true;
                return;
            }
            CardGroup upgradeable = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
            for (AbstractCard c : this.p.hand.group) {
                if (!c.canUpgrade() || c.type == AbstractCard.CardType.STATUS) continue;
                upgradeable.addToTop(c);
            }
            if (upgradeable.size() > 0) {
                upgradeable.shuffle();
                upgradeable.group.get(0).upgrade();
                upgradeable.group.get(0).superFlash();
                upgradeable.group.get(0).applyPowers();
            }
            this.isDone = true;
            return;
        }
        this.tickDuration();
    }
}


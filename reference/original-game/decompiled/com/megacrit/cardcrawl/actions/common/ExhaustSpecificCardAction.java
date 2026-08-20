/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public class ExhaustSpecificCardAction
extends AbstractGameAction {
    private AbstractCard targetCard;
    private CardGroup group;
    private float startingDuration;

    public ExhaustSpecificCardAction(AbstractCard targetCard, CardGroup group, boolean isFast) {
        this.targetCard = targetCard;
        this.setValues(AbstractDungeon.player, AbstractDungeon.player, this.amount);
        this.actionType = AbstractGameAction.ActionType.EXHAUST;
        this.group = group;
        this.duration = this.startingDuration = Settings.ACTION_DUR_FAST;
    }

    public ExhaustSpecificCardAction(AbstractCard targetCard, CardGroup group) {
        this(targetCard, group, false);
    }

    @Override
    public void update() {
        if (this.duration == this.startingDuration && this.group.contains(this.targetCard)) {
            this.group.moveToExhaustPile(this.targetCard);
            CardCrawlGame.dungeon.checkForPactAchievement();
            this.targetCard.exhaustOnUseOnce = false;
            this.targetCard.freeToPlayOnce = false;
        }
        this.tickDuration();
    }
}


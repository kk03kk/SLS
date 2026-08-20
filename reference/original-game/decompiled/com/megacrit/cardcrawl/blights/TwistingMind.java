/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.blights;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.status.Burn;
import com.megacrit.cardcrawl.cards.status.Dazed;
import com.megacrit.cardcrawl.cards.status.Slimed;
import com.megacrit.cardcrawl.cards.status.VoidCard;
import com.megacrit.cardcrawl.cards.status.Wound;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.BlightStrings;

public class TwistingMind
extends AbstractBlight {
    public static final String ID = "TwistingMind";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString("TwistingMind");
    public static final String NAME = TwistingMind.blightStrings.NAME;
    public static final String[] DESC = TwistingMind.blightStrings.DESCRIPTION;

    public TwistingMind() {
        super(ID, NAME, DESC[0] + 1 + DESC[1], "twist.png", false);
        this.counter = 1;
    }

    @Override
    public void stack() {
        ++this.counter;
        this.updateDescription();
        this.flash();
    }

    @Override
    public void updateDescription() {
        this.description = DESC[0] + this.counter + DESC[1];
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public void onPlayerEndTurn() {
        this.flash();
        CardGroup group = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        group.addToBottom(new Slimed());
        group.addToBottom(new VoidCard());
        group.addToBottom(new Burn());
        group.addToBottom(new Dazed());
        group.addToBottom(new Wound());
        group.shuffle();
        AbstractDungeon.actionManager.addToBottom(new MakeTempCardInDrawPileAction(group.getBottomCard(), this.counter, false, true));
        group.clear();
    }
}


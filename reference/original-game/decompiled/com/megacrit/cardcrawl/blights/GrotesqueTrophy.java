/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.blights;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.curses.Pride;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.BlightStrings;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class GrotesqueTrophy
extends AbstractBlight {
    public static final String ID = "GrotesqueTrophy";
    private static final BlightStrings blightStrings = CardCrawlGame.languagePack.getBlightString("GrotesqueTrophy");
    public static final String NAME = GrotesqueTrophy.blightStrings.NAME;
    public static final String[] DESC = GrotesqueTrophy.blightStrings.DESCRIPTION;

    public GrotesqueTrophy() {
        super(ID, NAME, DESC[0] + 3 + DESC[1], "trophy.png", false);
        this.counter = 1;
    }

    @Override
    public void onEquip() {
        CardGroup group = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        for (int i = 0; i < 3; ++i) {
            Pride bloatCard = new Pride();
            UnlockTracker.markCardAsSeen(bloatCard.cardID);
            group.addToBottom(((AbstractCard)bloatCard).makeCopy());
        }
        AbstractDungeon.gridSelectScreen.openConfirmationGrid(group, DESC[2]);
    }

    @Override
    public void stack() {
        ++this.counter;
        this.flash();
        CardGroup group = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        for (int i = 0; i < 3; ++i) {
            Pride bloatCard = new Pride();
            UnlockTracker.markCardAsSeen(bloatCard.cardID);
            group.addToBottom(((AbstractCard)bloatCard).makeCopy());
        }
        AbstractDungeon.gridSelectScreen.openConfirmationGrid(group, DESC[2]);
    }
}


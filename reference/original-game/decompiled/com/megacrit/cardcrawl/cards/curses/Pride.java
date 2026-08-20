/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.curses;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Pride
extends AbstractCard {
    public static final String ID = "Pride";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Pride");

    public Pride() {
        super(ID, Pride.cardStrings.NAME, "curse/pride", 1, Pride.cardStrings.DESCRIPTION, AbstractCard.CardType.CURSE, AbstractCard.CardColor.CURSE, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
        this.isInnate = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void triggerOnEndOfTurnForPlayingCard() {
        this.addToBot(new MakeTempCardInDrawPileAction(this.makeStatEquivalentCopy(), 1, false, true));
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new Pride();
    }
}


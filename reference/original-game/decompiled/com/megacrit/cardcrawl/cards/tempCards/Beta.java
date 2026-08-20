/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.tempCards;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Omega;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Beta
extends AbstractCard {
    public static final String ID = "Beta";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Beta");

    public Beta() {
        super(ID, Beta.cardStrings.NAME, "colorless/skill/beta", 2, Beta.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.NONE);
        this.cardsToPreview = new Omega();
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new MakeTempCardInDrawPileAction(this.cardsToPreview.makeStatEquivalentCopy(), 1, true, true));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Beta();
    }
}


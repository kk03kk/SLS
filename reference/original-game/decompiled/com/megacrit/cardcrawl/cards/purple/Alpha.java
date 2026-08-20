/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Beta;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Alpha
extends AbstractCard {
    public static final String ID = "Alpha";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Alpha");

    public Alpha() {
        super(ID, Alpha.cardStrings.NAME, "purple/skill/alpha", 1, Alpha.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
        this.cardsToPreview = new Beta();
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new MakeTempCardInDrawPileAction(this.cardsToPreview.makeStatEquivalentCopy(), 1, true, true));
    }

    @Override
    public AbstractCard makeCopy() {
        return new Alpha();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.isInnate = true;
            this.rawDescription = Alpha.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.deprecated.DEPRECATEDCausality;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDMetaphysics
extends AbstractCard {
    public static final String ID = "Metaphysics";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Metaphysics");

    public DEPRECATEDMetaphysics() {
        super(ID, DEPRECATEDMetaphysics.cardStrings.NAME, "purple/skill/metaphysics", 1, DEPRECATEDMetaphysics.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.cardsToPreview = new DEPRECATEDCausality();
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
            this.cardsToPreview.upgrade();
            this.rawDescription = DEPRECATEDMetaphysics.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDMetaphysics();
    }
}


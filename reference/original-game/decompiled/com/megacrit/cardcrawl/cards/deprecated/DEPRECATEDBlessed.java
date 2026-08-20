/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInDrawPileAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Miracle;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDBlessed
extends AbstractCard {
    public static final String ID = "Blessed";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Blessed");

    public DEPRECATEDBlessed() {
        super(ID, DEPRECATEDBlessed.cardStrings.NAME, null, 0, DEPRECATEDBlessed.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
        this.magicNumber = this.baseMagicNumber = 2;
        this.cardsToPreview = new Miracle();
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        AbstractCard miracle = CardLibrary.getCard("Miracle").makeCopy();
        if (this.upgraded) {
            miracle.upgrade();
        }
        this.addToBot(new MakeTempCardInDrawPileAction(miracle, this.magicNumber, true, true, false));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.cardsToPreview.upgrade();
            this.rawDescription = DEPRECATEDBlessed.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDBlessed();
    }
}


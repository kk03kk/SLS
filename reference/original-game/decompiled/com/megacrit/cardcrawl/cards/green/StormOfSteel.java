/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.unique.BladeFuryAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Shiv;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class StormOfSteel
extends AbstractCard {
    public static final String ID = "Storm of Steel";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Storm of Steel");

    public StormOfSteel() {
        super(ID, StormOfSteel.cardStrings.NAME, "green/skill/storm_of_steel", 1, StormOfSteel.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
        this.cardsToPreview = new Shiv();
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new BladeFuryAction(this.upgraded));
    }

    @Override
    public AbstractCard makeCopy() {
        return new StormOfSteel();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.cardsToPreview.upgrade();
            this.rawDescription = StormOfSteel.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }
}


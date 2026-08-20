/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.deprecated.DEPRECATEDExperiencedAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDExperienced
extends AbstractCard {
    public static final String ID = "Experienced";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Experienced");

    public DEPRECATEDExperienced() {
        super(ID, DEPRECATEDExperienced.cardStrings.NAME, null, 1, DEPRECATEDExperienced.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 5;
        this.block = 5;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DEPRECATEDExperiencedAction(this.block, this));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDExperienced();
    }
}


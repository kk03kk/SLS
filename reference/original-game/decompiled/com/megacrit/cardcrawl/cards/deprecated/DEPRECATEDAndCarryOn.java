/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDAndCarryOn
extends AbstractCard {
    public static final String ID = "AndCarryOn";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("AndCarryOn");

    public DEPRECATEDAndCarryOn() {
        super(ID, DEPRECATEDAndCarryOn.cardStrings.NAME, null, 1, DEPRECATEDAndCarryOn.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 4;
        this.baseMagicNumber = 2;
        this.magicNumber = 2;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new GainBlockAction(p, p, this.block, false));
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
        return new DEPRECATEDAndCarryOn();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDSmile
extends AbstractCard {
    public static final String ID = "Smile";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Smile");

    public DEPRECATEDSmile() {
        super(ID, DEPRECATEDSmile.cardStrings.NAME, null, 1, DEPRECATEDSmile.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.updateCost(0);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDSmile();
    }
}


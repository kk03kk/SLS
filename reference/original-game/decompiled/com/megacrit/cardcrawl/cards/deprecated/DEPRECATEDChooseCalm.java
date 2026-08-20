/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDChooseCalm
extends AbstractCard {
    public static final String ID = "Calm";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Calm");

    public DEPRECATEDChooseCalm() {
        super(ID, DEPRECATEDChooseCalm.cardStrings.NAME, "colorless/skill/deep_breath", -2, DEPRECATEDChooseCalm.cardStrings.DESCRIPTION, AbstractCard.CardType.STATUS, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void onChoseThisOption() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDChooseCalm();
    }

    @Override
    public void upgrade() {
    }
}


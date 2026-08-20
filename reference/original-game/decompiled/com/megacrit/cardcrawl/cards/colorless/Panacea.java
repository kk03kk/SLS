/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.ArtifactPower;

public class Panacea
extends AbstractCard {
    public static final String ID = "Panacea";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Panacea");

    public Panacea() {
        super(ID, Panacea.cardStrings.NAME, "colorless/skill/panacea", 0, Panacea.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 1;
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new ArtifactPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Panacea();
    }
}


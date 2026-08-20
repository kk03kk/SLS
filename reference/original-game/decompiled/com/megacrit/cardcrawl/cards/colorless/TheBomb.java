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
import com.megacrit.cardcrawl.powers.TheBombPower;

public class TheBomb
extends AbstractCard {
    public static final String ID = "The Bomb";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("The Bomb");

    public TheBomb() {
        super(ID, TheBomb.cardStrings.NAME, "colorless/skill/the_bomb", 2, TheBomb.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 40;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new TheBombPower(p, 3, this.magicNumber), 3));
    }

    @Override
    public AbstractCard makeCopy() {
        return new TheBomb();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(10);
        }
    }
}


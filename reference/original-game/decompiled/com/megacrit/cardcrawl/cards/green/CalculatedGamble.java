/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.unique.CalculatedGambleAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class CalculatedGamble
extends AbstractCard {
    public static final String ID = "Calculated Gamble";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Calculated Gamble");

    public CalculatedGamble() {
        super(ID, CalculatedGamble.cardStrings.NAME, "green/skill/calculated_gamble", 0, CalculatedGamble.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.NONE);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new CalculatedGambleAction(false));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.rawDescription = CalculatedGamble.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
            this.exhaust = false;
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new CalculatedGamble();
    }
}


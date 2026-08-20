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
import com.megacrit.cardcrawl.powers.IntangiblePlayerPower;

public class Apparition
extends AbstractCard {
    public static final String ID = "Ghostly";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Ghostly");

    public Apparition() {
        super(ID, Apparition.cardStrings.NAME, "colorless/skill/apparition", 1, Apparition.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
        this.isEthereal = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new IntangiblePlayerPower(p, 1), 1));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.rawDescription = Apparition.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
            this.isEthereal = false;
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Apparition();
    }
}


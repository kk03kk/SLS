/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.Lightning;
import com.megacrit.cardcrawl.powers.ElectroPower;

public class Electrodynamics
extends AbstractCard {
    public static final String ID = "Electrodynamics";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Electrodynamics");

    public Electrodynamics() {
        super(ID, Electrodynamics.cardStrings.NAME, "blue/power/electrodynamics", 2, Electrodynamics.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 2;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (!p.hasPower(ID)) {
            this.addToBot(new ApplyPowerAction(p, p, new ElectroPower(p)));
        }
        for (int i = 0; i < this.magicNumber; ++i) {
            Lightning orb = new Lightning();
            this.addToBot(new ChannelAction(orb));
        }
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
        return new Electrodynamics();
    }
}


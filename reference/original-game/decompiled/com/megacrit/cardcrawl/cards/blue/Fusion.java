/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.Plasma;

public class Fusion
extends AbstractCard {
    public static final String ID = "Fusion";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Fusion");

    public Fusion() {
        super(ID, Fusion.cardStrings.NAME, "blue/skill/fusion", 2, Fusion.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 1;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        for (int i = 0; i < this.magicNumber; ++i) {
            Plasma orb = new Plasma();
            this.addToBot(new ChannelAction(orb));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Fusion();
    }
}


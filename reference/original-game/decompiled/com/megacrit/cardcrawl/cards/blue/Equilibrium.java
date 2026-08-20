/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.EquilibriumPower;

public class Equilibrium
extends AbstractCard {
    public static final String ID = "Undo";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Undo");

    public Equilibrium() {
        super(ID, Equilibrium.cardStrings.NAME, "blue/skill/equilibrium", 2, Equilibrium.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 13;
        this.magicNumber = this.baseMagicNumber = 1;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new GainBlockAction((AbstractCreature)p, p, this.block));
        this.addToBot(new ApplyPowerAction(p, p, new EquilibriumPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(3);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Equilibrium();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.Frost;

public class Coolheaded
extends AbstractCard {
    public static final String ID = "Coolheaded";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Coolheaded");

    public Coolheaded() {
        super(ID, Coolheaded.cardStrings.NAME, "blue/skill/coolheaded", 1, Coolheaded.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.SELF);
        this.showEvokeValue = true;
        this.showEvokeOrbCount = 1;
        this.magicNumber = this.baseMagicNumber = 1;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ChannelAction(new Frost()));
        this.addToBot(new DrawCardAction(p, this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
            this.rawDescription = Coolheaded.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Coolheaded();
    }
}


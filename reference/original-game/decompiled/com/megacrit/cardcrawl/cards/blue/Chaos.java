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
import com.megacrit.cardcrawl.orbs.AbstractOrb;

public class Chaos
extends AbstractCard {
    public static final String ID = "Chaos";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Chaos");

    public Chaos() {
        super(ID, Chaos.cardStrings.NAME, "blue/skill/chaos", 1, Chaos.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.showEvokeValue = true;
        this.showEvokeOrbCount = 1;
        this.magicNumber = this.baseMagicNumber = 1;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (this.upgraded) {
            this.addToBot(new ChannelAction(AbstractOrb.getRandomOrb(true)));
        }
        this.addToBot(new ChannelAction(AbstractOrb.getRandomOrb(true)));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
            this.showEvokeOrbCount = 2;
            this.rawDescription = Chaos.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Chaos();
    }
}


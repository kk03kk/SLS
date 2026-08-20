/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.actions.defect.DarkImpulseAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.Dark;

public class Darkness
extends AbstractCard {
    public static final String ID = "Darkness";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Darkness");

    public Darkness() {
        super(ID, Darkness.cardStrings.NAME, "blue/skill/darkness", 1, Darkness.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.showEvokeValue = true;
        this.showEvokeOrbCount = 1;
        this.baseMagicNumber = 1;
        this.magicNumber = 1;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ChannelAction(new Dark()));
        if (this.upgraded) {
            this.addToBot(new DarkImpulseAction());
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.rawDescription = Darkness.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Darkness();
    }
}


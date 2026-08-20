/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.defect.ImpulseAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Impulse
extends AbstractCard {
    public static final String ID = "Impulse";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Impulse");

    public Impulse() {
        super(ID, Impulse.cardStrings.NAME, "blue/skill/impulse", 1, Impulse.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ImpulseAction());
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.exhaust = false;
            this.upgradeName();
            this.rawDescription = Impulse.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Impulse();
    }
}


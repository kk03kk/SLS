/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDIntrospection
extends AbstractCard {
    public static final String ID = "Introspection";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Introspection");

    public DEPRECATEDIntrospection() {
        super(ID, DEPRECATEDIntrospection.cardStrings.NAME, null, 1, DEPRECATEDIntrospection.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new RemoveSpecificPowerAction((AbstractCreature)p, (AbstractCreature)p, "Frail"));
        this.addToBot(new RemoveSpecificPowerAction((AbstractCreature)p, (AbstractCreature)p, "Vulnerable"));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.exhaust = false;
            this.rawDescription = DEPRECATEDIntrospection.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDIntrospection();
    }
}


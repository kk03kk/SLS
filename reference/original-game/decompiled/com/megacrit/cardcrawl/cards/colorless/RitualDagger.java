/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.unique.RitualDaggerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class RitualDagger
extends AbstractCard {
    public static final String ID = "RitualDagger";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("RitualDagger");

    public RitualDagger() {
        super(ID, RitualDagger.cardStrings.NAME, "colorless/attack/ritual_dagger", 1, RitualDagger.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.ENEMY);
        this.misc = 15;
        this.magicNumber = this.baseMagicNumber = 3;
        this.baseDamage = this.misc;
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new RitualDaggerAction(m, new DamageInfo(p, this.damage, this.damageTypeForTurn), this.magicNumber, this.uuid));
    }

    @Override
    public void applyPowers() {
        this.baseBlock = this.misc;
        super.applyPowers();
        this.initializeDescription();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeMagicNumber(2);
            this.upgradeName();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new RitualDagger();
    }
}


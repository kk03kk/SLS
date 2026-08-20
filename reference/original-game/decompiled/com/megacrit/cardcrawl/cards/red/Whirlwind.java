/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.megacrit.cardcrawl.actions.unique.WhirlwindAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Whirlwind
extends AbstractCard {
    public static final String ID = "Whirlwind";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Whirlwind");

    public Whirlwind() {
        super(ID, Whirlwind.cardStrings.NAME, "red/attack/whirlwind", -1, Whirlwind.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.RED, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ALL_ENEMY);
        this.baseDamage = 5;
        this.isMultiDamage = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new WhirlwindAction(p, this.multiDamage, this.damageTypeForTurn, this.freeToPlayOnce, this.energyOnUse));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(3);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Whirlwind();
    }
}


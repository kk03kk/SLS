/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.defect.BarrageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Barrage
extends AbstractCard {
    public static final String ID = "Barrage";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Barrage");

    public Barrage() {
        super(ID, Barrage.cardStrings.NAME, "blue/attack/barrage", 1, Barrage.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 4;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new BarrageAction(m, new DamageInfo(p, this.damage, DamageInfo.DamageType.NORMAL)));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Barrage();
    }
}


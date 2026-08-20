/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.AttackDamageRandomEnemyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Ragnarok
extends AbstractCard {
    public static final String ID = "Ragnarok";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Ragnarok");

    public Ragnarok() {
        super(ID, Ragnarok.cardStrings.NAME, "purple/attack/ragnarok", 3, Ragnarok.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.ALL_ENEMY);
        this.baseDamage = 5;
        this.magicNumber = this.baseMagicNumber = 5;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        for (int i = 0; i < this.magicNumber; ++i) {
            this.addToBot(new AttackDamageRandomEnemyAction(this, AbstractGameAction.AttackEffect.LIGHTNING));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(1);
            this.upgradeMagicNumber(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Ragnarok();
    }
}


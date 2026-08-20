/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class WheelKick
extends AbstractCard {
    public static final String ID = "WheelKick";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("WheelKick");

    public WheelKick() {
        super(ID, WheelKick.cardStrings.NAME, "purple/attack/wheel_kick", 2, WheelKick.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 15;
        this.magicNumber = this.baseMagicNumber = 2;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
        this.addToBot(new DrawCardAction(p, this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(5);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new WheelKick();
    }
}


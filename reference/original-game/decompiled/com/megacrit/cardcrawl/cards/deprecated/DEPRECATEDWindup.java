/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.watcher.VigorPower;

public class DEPRECATEDWindup
extends AbstractCard {
    public static final String ID = "Windup";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Windup");

    public DEPRECATEDWindup() {
        super(ID, DEPRECATEDWindup.cardStrings.NAME, null, 1, DEPRECATEDWindup.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 6;
        this.magicNumber = this.baseMagicNumber = 3;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
        this.addToBot(new ApplyPowerAction(p, p, new VigorPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(3);
            this.upgradeMagicNumber(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDWindup();
    }
}


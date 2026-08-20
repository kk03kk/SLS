/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.defect.AllCostToHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class AllForOne
extends AbstractCard {
    public static final String ID = "All For One";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("All For One");

    public AllForOne() {
        super(ID, AllForOne.cardStrings.NAME, "blue/attack/all_for_one", 2, AllForOne.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 10;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
        this.addToBot(new AllCostToHandAction(0));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeDamage(4);
            this.upgradeName();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new AllForOne();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.AttackDamageRandomEnemyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class SwordBoomerang
extends AbstractCard {
    public static final String ID = "Sword Boomerang";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Sword Boomerang");

    public SwordBoomerang() {
        super(ID, SwordBoomerang.cardStrings.NAME, "red/attack/sword_boomerang", 1, SwordBoomerang.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.RED, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.ALL_ENEMY);
        this.baseDamage = 3;
        this.magicNumber = this.baseMagicNumber = 3;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        for (int i = 0; i < this.magicNumber; ++i) {
            this.addToBot(new AttackDamageRandomEnemyAction(this, AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new SwordBoomerang();
    }
}


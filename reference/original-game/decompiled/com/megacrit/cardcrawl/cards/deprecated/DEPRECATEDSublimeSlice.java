/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.deprecated.DEPRECATEDRandomStanceAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDSublimeSlice
extends AbstractCard {
    public static final String ID = "SublimeSlice";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("SublimeSlice");

    public DEPRECATEDSublimeSlice() {
        super(ID, DEPRECATEDSublimeSlice.cardStrings.NAME, null, 2, DEPRECATEDSublimeSlice.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 10;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(m, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.SLASH_HORIZONTAL));
        this.addToBot(new DEPRECATEDRandomStanceAction());
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(4);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDSublimeSlice();
    }
}


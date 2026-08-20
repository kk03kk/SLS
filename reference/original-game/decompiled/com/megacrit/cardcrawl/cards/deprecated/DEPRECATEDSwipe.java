/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDSwipe
extends AbstractCard {
    public static final String ID = "Swipe";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Swipe");

    public DEPRECATEDSwipe() {
        super(ID, DEPRECATEDSwipe.cardStrings.NAME, "red/attack/cleave", 2, DEPRECATEDSwipe.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 14;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DamageAction(m, new DamageInfo(p, this.damage, this.damageTypeForTurn)));
        for (AbstractMonster mo : AbstractDungeon.getMonsters().monsters) {
            if (mo == m) continue;
            this.addToBot(new DamageAction(mo, new DamageInfo(p, this.damage / 2, this.damageTypeForTurn)));
        }
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
        return new DEPRECATEDSwipe();
    }
}


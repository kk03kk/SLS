/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.watcher.FlickerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDFlicker
extends AbstractCard {
    public static final String ID = "DEPRECATEDFlicker";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("DEPRECATEDFlicker");

    public DEPRECATEDFlicker() {
        super(ID, DEPRECATEDFlicker.cardStrings.NAME, null, 0, DEPRECATEDFlicker.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 5;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new FlickerAction(m, new DamageInfo(p, this.damage, this.damageTypeForTurn), this));
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
        return new DEPRECATEDFlicker();
    }
}


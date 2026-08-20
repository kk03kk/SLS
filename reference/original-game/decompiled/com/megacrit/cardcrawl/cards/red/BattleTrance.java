/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.NoDrawPower;

public class BattleTrance
extends AbstractCard {
    public static final String ID = "Battle Trance";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Battle Trance");

    public BattleTrance() {
        super(ID, BattleTrance.cardStrings.NAME, "red/skill/battle_trance", 0, BattleTrance.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.RED, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.NONE);
        this.magicNumber = this.baseMagicNumber = 3;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DrawCardAction(p, this.magicNumber));
        this.addToBot(new ApplyPowerAction(p, p, new NoDrawPower(p)));
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
        return new BattleTrance();
    }
}


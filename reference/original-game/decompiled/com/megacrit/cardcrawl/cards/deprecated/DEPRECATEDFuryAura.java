/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.LoseStrengthPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

public class DEPRECATEDFuryAura
extends AbstractCard {
    public static final String ID = "FuryAura";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("FuryAura");

    public DEPRECATEDFuryAura() {
        super(ID, DEPRECATEDFuryAura.cardStrings.NAME, null, 2, DEPRECATEDFuryAura.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.selfRetain = true;
        this.magicNumber = 4;
        this.baseMagicNumber = 4;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new StrengthPower(p, this.magicNumber), this.magicNumber));
        this.addToBot(new ApplyPowerAction(p, p, new LoseStrengthPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public void atTurnStartPreDraw() {
        AbstractPlayer p = AbstractDungeon.player;
        this.addToBot(new ApplyPowerAction(p, p, new StrengthPower(p, 1), 1));
        this.addToBot(new ApplyPowerAction(p, p, new LoseStrengthPower(p, 1), 1));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDFuryAura();
    }
}


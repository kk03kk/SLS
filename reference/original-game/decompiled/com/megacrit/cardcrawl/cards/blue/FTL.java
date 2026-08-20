/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.defect.FTLAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class FTL
extends AbstractCard {
    public static final String ID = "FTL";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("FTL");

    public FTL() {
        super(ID, FTL.cardStrings.NAME, "blue/attack/ftl", 0, FTL.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 5;
        this.magicNumber = this.baseMagicNumber = 3;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new FTLAction(m, new DamageInfo(p, this.damage, this.damageTypeForTurn), this.magicNumber));
        this.rawDescription = FTL.cardStrings.DESCRIPTION;
        this.initializeDescription();
    }

    @Override
    public void applyPowers() {
        super.applyPowers();
        int count = AbstractDungeon.actionManager.cardsPlayedThisTurn.size();
        this.rawDescription = FTL.cardStrings.DESCRIPTION;
        this.rawDescription = this.rawDescription + FTL.cardStrings.EXTENDED_DESCRIPTION[0] + count;
        this.rawDescription = count == 1 ? this.rawDescription + FTL.cardStrings.EXTENDED_DESCRIPTION[1] : this.rawDescription + FTL.cardStrings.EXTENDED_DESCRIPTION[2];
        this.initializeDescription();
    }

    @Override
    public void onMoveToDiscard() {
        this.rawDescription = FTL.cardStrings.DESCRIPTION;
        this.initializeDescription();
    }

    @Override
    public void triggerOnGlowCheck() {
        this.glowColor = AbstractDungeon.actionManager.cardsPlayedThisTurn.size() < this.magicNumber ? AbstractCard.GOLD_BORDER_GLOW_COLOR : AbstractCard.BLUE_BORDER_GLOW_COLOR;
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
        return new FTL();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.VulnerablePower;

public class Trip
extends AbstractCard {
    public static final String ID = "Trip";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Trip");

    public Trip() {
        super(ID, Trip.cardStrings.NAME, "colorless/skill/trip", 0, Trip.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ENEMY);
        this.magicNumber = this.baseMagicNumber = 2;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (!this.upgraded) {
            this.addToBot(new ApplyPowerAction(m, p, new VulnerablePower(m, this.magicNumber, false), this.magicNumber));
        } else {
            for (AbstractMonster mo : AbstractDungeon.getCurrRoom().monsters.monsters) {
                this.addToBot(new ApplyPowerAction(mo, p, new VulnerablePower(mo, this.magicNumber, false), this.magicNumber, true, AbstractGameAction.AttackEffect.NONE));
            }
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Trip();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.target = AbstractCard.CardTarget.ALL_ENEMY;
            this.rawDescription = Trip.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }
}


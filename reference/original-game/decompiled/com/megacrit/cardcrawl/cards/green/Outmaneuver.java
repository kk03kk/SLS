/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.EnergizedPower;

public class Outmaneuver
extends AbstractCard {
    public static final String ID = "Outmaneuver";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Outmaneuver");

    public Outmaneuver() {
        super(ID, Outmaneuver.cardStrings.NAME, "green/skill/outmaneuver", 1, Outmaneuver.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (!this.upgraded) {
            this.addToBot(new ApplyPowerAction(p, p, new EnergizedPower(p, 2), 2));
        } else {
            this.addToBot(new ApplyPowerAction(p, p, new EnergizedPower(p, 3), 3));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.rawDescription = Outmaneuver.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Outmaneuver();
    }
}


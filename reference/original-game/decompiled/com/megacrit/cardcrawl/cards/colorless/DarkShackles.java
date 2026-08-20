/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.GainStrengthPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

public class DarkShackles
extends AbstractCard {
    public static final String ID = "Dark Shackles";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Dark Shackles");

    public DarkShackles() {
        super(ID, DarkShackles.cardStrings.NAME, "colorless/skill/dark_shackles", 0, DarkShackles.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ENEMY);
        this.exhaust = true;
        this.magicNumber = this.baseMagicNumber = 9;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(m, p, new StrengthPower(m, -this.magicNumber), -this.magicNumber));
        if (m != null && !m.hasPower("Artifact")) {
            this.addToBot(new ApplyPowerAction(m, p, new GainStrengthPower(m, this.magicNumber), this.magicNumber));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(6);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DarkShackles();
    }
}


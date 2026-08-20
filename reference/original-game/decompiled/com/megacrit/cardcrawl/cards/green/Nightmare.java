/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.unique.NightmareAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Nightmare
extends AbstractCard {
    public static final String ID = "Night Terror";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Night Terror");

    public Nightmare() {
        super(ID, Nightmare.cardStrings.NAME, "green/skill/nightmare", 3, Nightmare.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
        this.baseMagicNumber = 3;
        this.magicNumber = 3;
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new NightmareAction(p, p, this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Nightmare();
    }
}


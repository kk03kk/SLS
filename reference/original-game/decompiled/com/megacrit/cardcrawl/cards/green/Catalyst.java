/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.unique.DoublePoisonAction;
import com.megacrit.cardcrawl.actions.unique.TriplePoisonAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Catalyst
extends AbstractCard {
    public static final String ID = "Catalyst";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Catalyst");

    public Catalyst() {
        super(ID, Catalyst.cardStrings.NAME, "green/skill/catalyst", 1, Catalyst.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ENEMY);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (!this.upgraded) {
            this.addToBot(new DoublePoisonAction(m, p));
        } else {
            this.addToBot(new TriplePoisonAction(m, p));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.rawDescription = Catalyst.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Catalyst();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.megacrit.cardcrawl.actions.unique.LimitBreakAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class LimitBreak
extends AbstractCard {
    public static final String ID = "Limit Break";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Limit Break");

    public LimitBreak() {
        super(ID, LimitBreak.cardStrings.NAME, "red/skill/limit_break", 1, LimitBreak.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.RED, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new LimitBreakAction());
    }

    @Override
    public AbstractCard makeCopy() {
        return new LimitBreak();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.exhaust = false;
            this.rawDescription = LimitBreak.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }
}


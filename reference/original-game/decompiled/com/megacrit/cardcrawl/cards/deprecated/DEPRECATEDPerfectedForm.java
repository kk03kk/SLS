/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.watcher.PerfectedFormAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDPerfectedForm
extends AbstractCard {
    public static final String ID = "PerfectedForm";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("PerfectedForm");

    public DEPRECATEDPerfectedForm() {
        super(ID, DEPRECATEDPerfectedForm.cardStrings.NAME, null, 0, DEPRECATEDPerfectedForm.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new PerfectedFormAction());
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.initializeDescription();
            this.exhaust = false;
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDPerfectedForm();
    }
}


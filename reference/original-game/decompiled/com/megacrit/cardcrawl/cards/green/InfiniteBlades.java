/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Shiv;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.InfiniteBladesPower;

public class InfiniteBlades
extends AbstractCard {
    public static final String ID = "Infinite Blades";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Infinite Blades");

    public InfiniteBlades() {
        super(ID, InfiniteBlades.cardStrings.NAME, "green/power/infinite_blades", 1, InfiniteBlades.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.cardsToPreview = new Shiv();
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new InfiniteBladesPower(p, 1), 1));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.isInnate = true;
            this.rawDescription = InfiniteBlades.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new InfiniteBlades();
    }
}


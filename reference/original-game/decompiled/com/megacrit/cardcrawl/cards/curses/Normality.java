/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.curses;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Normality
extends AbstractCard {
    public static final String ID = "Normality";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Normality");
    private static final int PLAY_LIMIT = 3;

    public Normality() {
        super(ID, Normality.cardStrings.NAME, "curse/normality", -2, Normality.cardStrings.DESCRIPTION, AbstractCard.CardType.CURSE, AbstractCard.CardColor.CURSE, AbstractCard.CardRarity.CURSE, AbstractCard.CardTarget.NONE);
    }

    @Override
    public boolean canPlay(AbstractCard card) {
        if (AbstractDungeon.actionManager.cardsPlayedThisTurn.size() >= 3) {
            card.cantUseMessage = Normality.cardStrings.EXTENDED_DESCRIPTION[0];
            return false;
        }
        return true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void applyPowers() {
        super.applyPowers();
        this.rawDescription = AbstractDungeon.actionManager.cardsPlayedThisTurn.size() == 0 ? Normality.cardStrings.EXTENDED_DESCRIPTION[1] + 3 + Normality.cardStrings.EXTENDED_DESCRIPTION[2] : (AbstractDungeon.actionManager.cardsPlayedThisTurn.size() == 1 ? Normality.cardStrings.EXTENDED_DESCRIPTION[1] + 3 + Normality.cardStrings.EXTENDED_DESCRIPTION[3] + AbstractDungeon.actionManager.cardsPlayedThisTurn.size() + Normality.cardStrings.EXTENDED_DESCRIPTION[4] : Normality.cardStrings.EXTENDED_DESCRIPTION[1] + 3 + Normality.cardStrings.EXTENDED_DESCRIPTION[3] + AbstractDungeon.actionManager.cardsPlayedThisTurn.size() + Normality.cardStrings.EXTENDED_DESCRIPTION[5]);
        this.initializeDescription();
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new Normality();
    }
}


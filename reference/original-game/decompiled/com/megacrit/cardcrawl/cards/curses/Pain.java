/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.curses;

import com.megacrit.cardcrawl.actions.common.LoseHPAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Pain
extends AbstractCard {
    public static final String ID = "Pain";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Pain");

    public Pain() {
        super(ID, Pain.cardStrings.NAME, "curse/pain", -2, Pain.cardStrings.DESCRIPTION, AbstractCard.CardType.CURSE, AbstractCard.CardColor.CURSE, AbstractCard.CardRarity.CURSE, AbstractCard.CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void triggerOnOtherCardPlayed(AbstractCard c) {
        this.addToTop(new LoseHPAction(AbstractDungeon.player, AbstractDungeon.player, 1));
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new Pain();
    }
}


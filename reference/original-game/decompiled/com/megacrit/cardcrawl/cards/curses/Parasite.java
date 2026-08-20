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

public class Parasite
extends AbstractCard {
    public static final String ID = "Parasite";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Parasite");

    public Parasite() {
        super(ID, Parasite.cardStrings.NAME, "curse/parasite", -2, Parasite.cardStrings.DESCRIPTION, AbstractCard.CardType.CURSE, AbstractCard.CardColor.CURSE, AbstractCard.CardRarity.CURSE, AbstractCard.CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void onRemoveFromMasterDeck() {
        AbstractDungeon.player.decreaseMaxHealth(3);
        CardCrawlGame.sound.play("BLOOD_SWISH");
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new Parasite();
    }
}


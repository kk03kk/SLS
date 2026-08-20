/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.ShuffleAction;
import com.megacrit.cardcrawl.actions.defect.ShuffleAllAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Reboot
extends AbstractCard {
    public static final String ID = "Reboot";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Reboot");

    public Reboot() {
        super(ID, Reboot.cardStrings.NAME, "blue/skill/reboot", 0, Reboot.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 4;
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ShuffleAllAction());
        this.addToBot(new ShuffleAction(AbstractDungeon.player.drawPile, false));
        this.addToBot(new DrawCardAction(p, this.magicNumber));
    }

    @Override
    public AbstractCard makeCopy() {
        return new Reboot();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(2);
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDWardAura
extends AbstractCard {
    public static final String ID = "WardAura";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("WardAura");

    public DEPRECATEDWardAura() {
        super(ID, DEPRECATEDWardAura.cardStrings.NAME, null, 1, DEPRECATEDWardAura.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.selfRetain = true;
        this.baseBlock = 8;
        this.magicNumber = 2;
        this.baseMagicNumber = 2;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new GainBlockAction((AbstractCreature)p, p, this.block));
    }

    @Override
    public void atTurnStartPreDraw() {
        this.addToBot(new GainBlockAction((AbstractCreature)AbstractDungeon.player, AbstractDungeon.player, this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
            this.upgradeBlock(3);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDWardAura();
    }
}


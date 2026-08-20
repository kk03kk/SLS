/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Protect
extends AbstractCard {
    public static final String ID = "Protect";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Protect");

    public Protect() {
        super(ID, Protect.cardStrings.NAME, "purple/skill/protect", 2, Protect.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 12;
        this.selfRetain = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new GainBlockAction((AbstractCreature)p, p, this.block));
    }

    @Override
    public AbstractCard makeCopy() {
        return new Protect();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(4);
        }
    }
}


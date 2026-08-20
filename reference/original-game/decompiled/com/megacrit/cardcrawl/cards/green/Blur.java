/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.BlurPower;

public class Blur
extends AbstractCard {
    public static final String ID = "Blur";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Blur");

    public Blur() {
        super(ID, Blur.cardStrings.NAME, "green/skill/blur", 1, Blur.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 5;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new GainBlockAction((AbstractCreature)p, p, this.block));
        this.addToBot(new ApplyPowerAction(p, p, new BlurPower(p, 1), 1));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(3);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Blur();
    }
}


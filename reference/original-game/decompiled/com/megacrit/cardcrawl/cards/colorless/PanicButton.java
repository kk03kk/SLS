/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.NoBlockPower;

public class PanicButton
extends AbstractCard {
    public static final String ID = "PanicButton";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("PanicButton");

    public PanicButton() {
        super(ID, PanicButton.cardStrings.NAME, "colorless/skill/panic_button", 0, PanicButton.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 30;
        this.magicNumber = this.baseMagicNumber = 2;
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new GainBlockAction((AbstractCreature)p, p, this.block));
        this.addToBot(new ApplyPowerAction(p, p, new NoBlockPower(p, this.magicNumber, false), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(10);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new PanicButton();
    }
}


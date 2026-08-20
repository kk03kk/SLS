/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.watcher.MantraPower;

public class Prostrate
extends AbstractCard {
    public static final String ID = "Prostrate";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Prostrate");

    public Prostrate() {
        super(ID, Prostrate.cardStrings.NAME, "purple/skill/prostrate", 0, Prostrate.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.SELF);
        this.baseMagicNumber = 2;
        this.magicNumber = 2;
        this.block = this.baseBlock = 4;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new MantraPower(p, this.magicNumber), this.magicNumber));
        this.addToBot(new GainBlockAction(p, this.block));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
        }
    }

    @Override
    public void initializeDescription() {
        super.initializeDescription();
        this.keywords.add(GameDictionary.ENLIGHTENMENT.NAMES[0].toLowerCase());
    }

    @Override
    public AbstractCard makeCopy() {
        return new Prostrate();
    }
}


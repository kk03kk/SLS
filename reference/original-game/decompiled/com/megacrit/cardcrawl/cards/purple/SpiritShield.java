/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class SpiritShield
extends AbstractCard {
    public static final String ID = "SpiritShield";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("SpiritShield");

    public SpiritShield() {
        super(ID, SpiritShield.cardStrings.NAME, "purple/skill/spirit_shield", 2, SpiritShield.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.magicNumber = this.baseMagicNumber = 3;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.applyPowers();
        this.addToBot(new GainBlockAction((AbstractCreature)p, p, this.block));
    }

    @Override
    public void applyPowers() {
        int count = 0;
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (c == this) continue;
            ++count;
        }
        this.baseBlock = count * this.magicNumber;
        super.applyPowers();
        this.rawDescription = SpiritShield.cardStrings.DESCRIPTION + SpiritShield.cardStrings.EXTENDED_DESCRIPTION[0];
        this.initializeDescription();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(1);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new SpiritShield();
    }
}


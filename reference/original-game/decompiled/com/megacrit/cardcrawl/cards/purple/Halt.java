/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.watcher.HaltAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Halt
extends AbstractCard {
    public static final String ID = "Halt";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Halt");
    private static final int BLOCK_AMOUNT = 3;
    private static final int UPGRADE_PLUS_BLOCK = 1;
    private static final int BLOCK_DIFFERENCE = 6;
    private static final int UPGRADE_PLUS_BLOCK_DIFFERENCE = 4;

    public Halt() {
        super(ID, Halt.cardStrings.NAME, "purple/skill/halt", 0, Halt.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 3;
        this.block = 3;
        this.baseMagicNumber = 9;
        this.magicNumber = 9;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.applyPowers();
        this.addToBot(new HaltAction(p, this.block, this.magicNumber));
    }

    @Override
    public void applyPowers() {
        this.baseBlock += 6 + this.timesUpgraded * 4;
        this.baseMagicNumber = this.baseBlock;
        super.applyPowers();
        this.magicNumber = this.block;
        this.isMagicNumberModified = this.isBlockModified;
        this.baseBlock -= 6 + this.timesUpgraded * 4;
        super.applyPowers();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(1);
            this.baseMagicNumber = this.baseBlock + 6 + this.timesUpgraded * 4;
            this.upgradedMagicNumber = this.upgradedBlock;
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Halt();
    }
}


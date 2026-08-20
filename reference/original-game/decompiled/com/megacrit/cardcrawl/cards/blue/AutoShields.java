/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.blue;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class AutoShields
extends AbstractCard {
    public static final String ID = "Auto Shields";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Auto Shields");

    public AutoShields() {
        super(ID, AutoShields.cardStrings.NAME, "blue/skill/auto_shields", 1, AutoShields.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 11;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (AbstractDungeon.player.currentBlock == 0) {
            this.addToBot(new GainBlockAction((AbstractCreature)p, p, this.block));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(4);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new AutoShields();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class InfernalBlade
extends AbstractCard {
    public static final String ID = "Infernal Blade";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Infernal Blade");

    public InfernalBlade() {
        super(ID, InfernalBlade.cardStrings.NAME, "red/skill/infernal_blade", 1, InfernalBlade.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.RED, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.NONE);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        AbstractCard c = AbstractDungeon.returnTrulyRandomCardInCombat(AbstractCard.CardType.ATTACK).makeCopy();
        c.setCostForTurn(0);
        this.addToBot(new MakeTempCardInHandAction(c, true));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(0);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new InfernalBlade();
    }
}


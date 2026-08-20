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

public class ForceField
extends AbstractCard {
    public static final String ID = "Force Field";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Force Field");

    public ForceField() {
        super(ID, ForceField.cardStrings.NAME, "blue/skill/forcefield", 4, ForceField.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.BLUE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 12;
        if (CardCrawlGame.dungeon != null && AbstractDungeon.currMapNode != null) {
            this.configureCostsOnNewCard();
        }
    }

    public void configureCostsOnNewCard() {
        for (AbstractCard c : AbstractDungeon.actionManager.cardsPlayedThisCombat) {
            if (c.type != AbstractCard.CardType.POWER) continue;
            this.updateCost(-1);
        }
    }

    @Override
    public void triggerOnCardPlayed(AbstractCard c) {
        if (c.type == AbstractCard.CardType.POWER) {
            this.updateCost(-1);
        }
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new GainBlockAction((AbstractCreature)p, p, this.block));
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
        return new ForceField();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.common.PlayTopCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDLetFateDecide
extends AbstractCard {
    public static final String ID = "LetFateDecide";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("LetFateDecide");

    public DEPRECATEDLetFateDecide() {
        super(ID, DEPRECATEDLetFateDecide.cardStrings.NAME, null, -1, DEPRECATEDLetFateDecide.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        for (int i = 0; i < this.energyOnUse; ++i) {
            this.addToBot(new PlayTopCardAction(AbstractDungeon.getCurrRoom().monsters.getRandomMonster(null, true, AbstractDungeon.cardRandomRng), false));
        }
        if (this.energyOnUse >= 3) {
            // empty if block
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDLetFateDecide();
    }
}


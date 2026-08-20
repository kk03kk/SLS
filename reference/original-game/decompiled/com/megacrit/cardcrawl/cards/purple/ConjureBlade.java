/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.watcher.ConjureBladeAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.tempCards.Expunger;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class ConjureBlade
extends AbstractCard {
    public static final String ID = "ConjureBlade";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("ConjureBlade");

    public ConjureBlade() {
        super(ID, ConjureBlade.cardStrings.NAME, "purple/skill/conjure_blade", -1, ConjureBlade.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.cardsToPreview = new Expunger();
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (this.upgraded) {
            this.addToBot(new ConjureBladeAction(p, this.freeToPlayOnce, this.energyOnUse + 1));
        } else {
            this.addToBot(new ConjureBladeAction(p, this.freeToPlayOnce, this.energyOnUse));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.rawDescription = ConjureBlade.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new ConjureBlade();
    }
}


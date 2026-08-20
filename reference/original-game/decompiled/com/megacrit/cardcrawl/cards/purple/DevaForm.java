/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.watcher.DevaPower;

public class DevaForm
extends AbstractCard {
    public static final String ID = "DevaForm";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("DevaForm");

    public DevaForm() {
        super(ID, DevaForm.cardStrings.NAME, "purple/power/deva_form", 3, DevaForm.cardStrings.DESCRIPTION, AbstractCard.CardType.POWER, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.isEthereal = true;
        this.magicNumber = this.baseMagicNumber = 1;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new DevaPower(p), 1));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.isEthereal = false;
            this.rawDescription = DevaForm.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DevaForm();
    }
}


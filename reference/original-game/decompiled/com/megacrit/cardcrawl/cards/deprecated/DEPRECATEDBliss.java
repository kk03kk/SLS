/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDBliss
extends AbstractCard {
    public static final String ID = "Bliss";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Bliss");

    public DEPRECATEDBliss() {
        super(ID, DEPRECATEDBliss.cardStrings.NAME, "purple/skill/never_surrender", 1, DEPRECATEDBliss.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 7;
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new GainBlockAction((AbstractCreature)p, p, this.block));
        this.addToBot(new ChangeStanceAction("Calm"));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(3);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDBliss();
    }
}


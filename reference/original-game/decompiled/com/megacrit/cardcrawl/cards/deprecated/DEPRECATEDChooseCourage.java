/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDChooseCourage
extends AbstractCard {
    public static final String ID = "Joy";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Joy");

    public DEPRECATEDChooseCourage() {
        super(ID, DEPRECATEDChooseCourage.cardStrings.NAME, "red/skill/warcry", -2, DEPRECATEDChooseCourage.cardStrings.DESCRIPTION, AbstractCard.CardType.STATUS, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
    }

    @Override
    public void onChoseThisOption() {
        this.addToBot(new ChangeStanceAction("Calm"));
    }

    @Override
    public void upgrade() {
    }

    @Override
    public AbstractCard makeCopy() {
        return new DEPRECATEDChooseCourage();
    }
}


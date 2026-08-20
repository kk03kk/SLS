/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.watcher.ChangeStanceAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.watcher.EndTurnDeathPower;

public class Blasphemy
extends AbstractCard {
    public static final String ID = "Blasphemy";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Blasphemy");

    public Blasphemy() {
        super(ID, Blasphemy.cardStrings.NAME, "purple/skill/blasphemy", 1, Blasphemy.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.SELF);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ChangeStanceAction("Divinity"));
        this.addToBot(new ApplyPowerAction(p, p, new EndTurnDeathPower(p)));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.selfRetain = true;
            this.rawDescription = Blasphemy.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Blasphemy();
    }
}


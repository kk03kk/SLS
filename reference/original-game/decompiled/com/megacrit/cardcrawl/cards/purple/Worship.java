/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.watcher.MantraPower;

public class Worship
extends AbstractCard {
    public static final String ID = "Worship";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Worship");

    public Worship() {
        super(ID, Worship.cardStrings.NAME, "purple/skill/worship", 2, Worship.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.baseMagicNumber = 5;
        this.magicNumber = 5;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new MantraPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.selfRetain = true;
            this.rawDescription = Worship.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public void initializeDescription() {
        super.initializeDescription();
        this.keywords.add(GameDictionary.ENLIGHTENMENT.NAMES[0].toLowerCase());
    }

    @Override
    public AbstractCard makeCopy() {
        return new Worship();
    }
}


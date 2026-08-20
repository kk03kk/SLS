/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.watcher.ChooseOneAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.optionCards.BecomeAlmighty;
import com.megacrit.cardcrawl.cards.optionCards.FameAndFortune;
import com.megacrit.cardcrawl.cards.optionCards.LiveForever;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import java.util.ArrayList;

public class Wish
extends AbstractCard {
    public static final String ID = "Wish";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Wish");

    public Wish() {
        super(ID, Wish.cardStrings.NAME, "purple/skill/wish", 3, Wish.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
        this.baseDamage = 3;
        this.baseMagicNumber = 25;
        this.magicNumber = 25;
        this.baseBlock = 6;
        this.exhaust = true;
        this.tags.add(AbstractCard.CardTags.HEALING);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        ArrayList<AbstractCard> stanceChoices = new ArrayList<AbstractCard>();
        stanceChoices.add(new BecomeAlmighty());
        stanceChoices.add(new FameAndFortune());
        stanceChoices.add(new LiveForever());
        if (this.upgraded) {
            for (AbstractCard c : stanceChoices) {
                c.upgrade();
            }
        }
        this.addToBot(new ChooseOneAction(stanceChoices));
    }

    @Override
    public void applyPowers() {
    }

    @Override
    public void calculateCardDamage(AbstractMonster mo) {
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(1);
            this.upgradeMagicNumber(5);
            this.upgradeBlock(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Wish();
    }
}


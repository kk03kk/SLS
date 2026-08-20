/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.tempCards;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.watcher.ExpungeVFXAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import java.util.ArrayList;

public class Expunger
extends AbstractCard {
    public static final String ID = "Expunger";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Expunger");

    public Expunger() {
        super(ID, Expunger.cardStrings.NAME, "colorless/attack/expunger", 1, Expunger.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.SPECIAL, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 9;
    }

    public void setX(int amount) {
        this.magicNumber = amount;
        if (this.upgraded) {
            ++this.magicNumber;
        }
        this.baseMagicNumber = this.magicNumber;
        this.rawDescription = this.baseMagicNumber == 1 ? Expunger.cardStrings.EXTENDED_DESCRIPTION[1] : Expunger.cardStrings.EXTENDED_DESCRIPTION[0];
        this.initializeDescription();
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        for (int i = 0; i < this.magicNumber; ++i) {
            this.addToBot(new ExpungeVFXAction(m));
            this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.NONE));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(6);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Expunger();
    }

    @Override
    public AbstractCard makeStatEquivalentCopy() {
        AbstractCard card = super.makeStatEquivalentCopy();
        card.baseMagicNumber = this.baseMagicNumber;
        card.magicNumber = this.magicNumber;
        card.description = (ArrayList)this.description.clone();
        return card;
    }
}


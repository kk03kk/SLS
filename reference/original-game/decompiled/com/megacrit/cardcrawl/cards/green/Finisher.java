/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.unique.DamagePerAttackPlayedAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Finisher
extends AbstractCard {
    public static final String ID = "Finisher";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Finisher");

    public Finisher() {
        super(ID, Finisher.cardStrings.NAME, "green/attack/finisher", 1, Finisher.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 6;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DamagePerAttackPlayedAction(m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.SLASH_DIAGONAL));
        this.rawDescription = Finisher.cardStrings.DESCRIPTION;
        this.initializeDescription();
    }

    @Override
    public void applyPowers() {
        super.applyPowers();
        int count = 0;
        for (AbstractCard c : AbstractDungeon.actionManager.cardsPlayedThisTurn) {
            if (c.type != AbstractCard.CardType.ATTACK) continue;
            ++count;
        }
        this.rawDescription = Finisher.cardStrings.DESCRIPTION;
        this.rawDescription = this.rawDescription + Finisher.cardStrings.EXTENDED_DESCRIPTION[0] + count;
        this.rawDescription = count == 1 ? this.rawDescription + Finisher.cardStrings.EXTENDED_DESCRIPTION[1] : this.rawDescription + Finisher.cardStrings.EXTENDED_DESCRIPTION[2];
        this.initializeDescription();
    }

    @Override
    public void onMoveToDiscard() {
        this.rawDescription = Finisher.cardStrings.DESCRIPTION;
        this.initializeDescription();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new Finisher();
    }
}


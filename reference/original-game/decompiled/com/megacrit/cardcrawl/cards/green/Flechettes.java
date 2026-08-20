/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.unique.FlechetteAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class Flechettes
extends AbstractCard {
    public static final String ID = "Flechettes";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Flechettes");

    public Flechettes() {
        super(ID, Flechettes.cardStrings.NAME, "green/attack/flechettes", 1, Flechettes.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 4;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new FlechetteAction(m, new DamageInfo(p, this.damage, this.damageTypeForTurn)));
        this.rawDescription = Flechettes.cardStrings.DESCRIPTION;
        this.initializeDescription();
    }

    @Override
    public void applyPowers() {
        super.applyPowers();
        int count = 0;
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (c.type != AbstractCard.CardType.SKILL) continue;
            ++count;
        }
        this.rawDescription = Flechettes.cardStrings.DESCRIPTION;
        this.rawDescription = this.rawDescription + Flechettes.cardStrings.EXTENDED_DESCRIPTION[0] + count;
        this.rawDescription = count == 1 ? this.rawDescription + Flechettes.cardStrings.EXTENDED_DESCRIPTION[1] : this.rawDescription + Flechettes.cardStrings.EXTENDED_DESCRIPTION[2];
        this.initializeDescription();
    }

    @Override
    public void onMoveToDiscard() {
        this.rawDescription = Flechettes.cardStrings.DESCRIPTION;
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
        return new Flechettes();
    }
}


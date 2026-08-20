/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.PoisonPower;
import com.megacrit.cardcrawl.powers.WeakPower;

public class CripplingPoison
extends AbstractCard {
    public static final String ID = "Crippling Poison";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Crippling Poison");

    public CripplingPoison() {
        super(ID, CripplingPoison.cardStrings.NAME, "green/skill/crippling_poison", 2, CripplingPoison.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ALL_ENEMY);
        this.magicNumber = this.baseMagicNumber = 4;
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (!AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.flash();
            for (AbstractMonster monster : AbstractDungeon.getMonsters().monsters) {
                if (monster.isDead || monster.isDying) continue;
                this.addToBot(new ApplyPowerAction(monster, p, new PoisonPower(monster, p, this.magicNumber), this.magicNumber));
                this.addToBot(new ApplyPowerAction(monster, p, new WeakPower(monster, 2, false), 2));
            }
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeMagicNumber(3);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new CripplingPoison();
    }
}


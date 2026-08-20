/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.purple;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class BowlingBash
extends AbstractCard {
    public static final String ID = "BowlingBash";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("BowlingBash");

    public BowlingBash() {
        super(ID, BowlingBash.cardStrings.NAME, "purple/attack/bowling_bash", 1, BowlingBash.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 7;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        int count = 0;
        for (AbstractMonster m2 : AbstractDungeon.getCurrRoom().monsters.monsters) {
            if (m2.isDeadOrEscaped()) continue;
            ++count;
            this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
        }
        if (count >= 3) {
            this.addToBot(new SFXAction("ATTACK_BOWLING"));
        }
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(3);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new BowlingBash();
    }
}


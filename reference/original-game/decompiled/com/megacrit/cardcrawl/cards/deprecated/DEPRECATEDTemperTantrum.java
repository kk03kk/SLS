/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.deprecated;

import com.megacrit.cardcrawl.actions.unique.SwordBoomerangAction;
import com.megacrit.cardcrawl.actions.watcher.StanceCheckAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class DEPRECATEDTemperTantrum
extends AbstractCard {
    public static final String ID = "TemperTantrum";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("TemperTantrum");

    public DEPRECATEDTemperTantrum() {
        super(ID, DEPRECATEDTemperTantrum.cardStrings.NAME, "red/attack/sword_boomerang", 1, DEPRECATEDTemperTantrum.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.PURPLE, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.ALL_ENEMY);
        this.baseDamage = 6;
        this.magicNumber = this.baseMagicNumber;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new SwordBoomerangAction(AbstractDungeon.getMonsters().getRandomMonster(null, true, AbstractDungeon.cardRandomRng), new DamageInfo(p, this.baseDamage), 1));
        this.addToBot(new StanceCheckAction("Wrath", new SwordBoomerangAction(new DamageInfo(p, this.baseDamage), 1)));
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
        return new DEPRECATEDTemperTantrum();
    }
}


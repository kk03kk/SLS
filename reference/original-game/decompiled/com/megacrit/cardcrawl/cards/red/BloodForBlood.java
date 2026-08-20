/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class BloodForBlood
extends AbstractCard {
    public static final String ID = "Blood for Blood";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Blood for Blood");

    public BloodForBlood() {
        super(ID, BloodForBlood.cardStrings.NAME, "red/attack/blood_for_blood", 4, BloodForBlood.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.RED, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 18;
    }

    @Override
    public void tookDamage() {
        this.updateCost(-1);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.SLASH_HEAVY));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            if (this.cost < 4) {
                this.upgradeBaseCost(this.cost - 1);
                if (this.cost < 0) {
                    this.cost = 0;
                }
            } else {
                this.upgradeBaseCost(3);
            }
            this.upgradeDamage(4);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        BloodForBlood tmp = new BloodForBlood();
        if (AbstractDungeon.player != null) {
            tmp.updateCost(-AbstractDungeon.player.damagedThisCombat);
        }
        return tmp;
    }
}


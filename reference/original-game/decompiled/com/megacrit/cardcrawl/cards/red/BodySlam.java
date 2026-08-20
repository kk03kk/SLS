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

public class BodySlam
extends AbstractCard {
    public static final String ID = "Body Slam";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Body Slam");

    public BodySlam() {
        super(ID, BodySlam.cardStrings.NAME, "red/attack/body_slam", 1, BodySlam.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.RED, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 0;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.baseDamage = p.currentBlock;
        this.calculateCardDamage(m);
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, DamageInfo.DamageType.NORMAL), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
        this.rawDescription = BodySlam.cardStrings.DESCRIPTION;
        this.initializeDescription();
    }

    @Override
    public void applyPowers() {
        this.baseDamage = AbstractDungeon.player.currentBlock;
        super.applyPowers();
        this.rawDescription = BodySlam.cardStrings.DESCRIPTION;
        this.rawDescription = this.rawDescription + BodySlam.cardStrings.UPGRADE_DESCRIPTION;
        this.initializeDescription();
    }

    @Override
    public void onMoveToDiscard() {
        this.rawDescription = BodySlam.cardStrings.DESCRIPTION;
        this.initializeDescription();
    }

    @Override
    public void calculateCardDamage(AbstractMonster mo) {
        super.calculateCardDamage(mo);
        this.rawDescription = BodySlam.cardStrings.DESCRIPTION;
        this.rawDescription = this.rawDescription + BodySlam.cardStrings.UPGRADE_DESCRIPTION;
        this.initializeDescription();
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(0);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new BodySlam();
    }
}


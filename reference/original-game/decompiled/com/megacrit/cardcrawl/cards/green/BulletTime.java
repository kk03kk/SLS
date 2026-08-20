/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.unique.ApplyBulletTimeAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.NoDrawPower;

public class BulletTime
extends AbstractCard {
    public static final String ID = "Bullet Time";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Bullet Time");

    public BulletTime() {
        super(ID, BulletTime.cardStrings.NAME, "green/skill/bullet_time", 3, BulletTime.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction(p, p, new NoDrawPower(p), 1));
        this.addToBot(new ApplyBulletTimeAction());
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBaseCost(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new BulletTime();
    }
}


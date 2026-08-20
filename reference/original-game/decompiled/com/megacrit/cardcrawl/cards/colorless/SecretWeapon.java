/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.colorless;

import com.megacrit.cardcrawl.actions.unique.AttackFromDeckToHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class SecretWeapon
extends AbstractCard {
    public static final String ID = "Secret Weapon";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Secret Weapon");

    public SecretWeapon() {
        super(ID, SecretWeapon.cardStrings.NAME, "colorless/skill/secret_weapon", 0, SecretWeapon.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.COLORLESS, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.NONE);
        this.exhaust = true;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new AttackFromDeckToHandAction(1));
    }

    @Override
    public boolean canUse(AbstractPlayer p, AbstractMonster m) {
        boolean canUse = super.canUse(p, m);
        if (!canUse) {
            return false;
        }
        boolean hasAttack = false;
        for (AbstractCard c : p.drawPile.group) {
            if (c.type != AbstractCard.CardType.ATTACK) continue;
            hasAttack = true;
        }
        if (!hasAttack) {
            this.cantUseMessage = SecretWeapon.cardStrings.EXTENDED_DESCRIPTION[0];
            canUse = false;
        }
        return canUse;
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.exhaust = false;
            this.rawDescription = SecretWeapon.cardStrings.UPGRADE_DESCRIPTION;
            this.initializeDescription();
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new SecretWeapon();
    }
}


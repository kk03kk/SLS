/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.green;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.CorpseExplosionPower;
import com.megacrit.cardcrawl.powers.PoisonPower;

public class CorpseExplosion
extends AbstractCard {
    public static final String ID = "Corpse Explosion";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Corpse Explosion");

    public CorpseExplosion() {
        super(ID, CorpseExplosion.cardStrings.NAME, "green/skill/corpse_explosion", 2, CorpseExplosion.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.GREEN, AbstractCard.CardRarity.RARE, AbstractCard.CardTarget.ENEMY);
        this.magicNumber = this.baseMagicNumber = 6;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new ApplyPowerAction((AbstractCreature)m, (AbstractCreature)p, (AbstractPower)new PoisonPower(m, p, this.magicNumber), this.magicNumber, AbstractGameAction.AttackEffect.POISON));
        this.addToBot(new ApplyPowerAction((AbstractCreature)m, (AbstractCreature)p, (AbstractPower)new CorpseExplosionPower(m), 1, AbstractGameAction.AttackEffect.POISON));
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
        return new CorpseExplosion();
    }
}


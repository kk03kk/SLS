/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.SearingBlowEffect;

public class SearingBlow
extends AbstractCard {
    public static final String ID = "Searing Blow";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Searing Blow");

    public SearingBlow() {
        this(0);
    }

    public SearingBlow(int upgrades) {
        super(ID, SearingBlow.cardStrings.NAME, "red/attack/searing_blow", 2, SearingBlow.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.RED, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 12;
        this.timesUpgraded = upgrades;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (m != null) {
            this.addToBot(new VFXAction(new SearingBlowEffect(m.hb.cX, m.hb.cY, this.timesUpgraded), 0.2f));
        }
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
    }

    @Override
    public void upgrade() {
        this.upgradeDamage(4 + this.timesUpgraded);
        ++this.timesUpgraded;
        this.upgraded = true;
        this.name = SearingBlow.cardStrings.NAME + "+" + this.timesUpgraded;
        this.initializeTitle();
    }

    @Override
    public boolean canUpgrade() {
        return true;
    }

    @Override
    public AbstractCard makeCopy() {
        return new SearingBlow(this.timesUpgraded);
    }
}


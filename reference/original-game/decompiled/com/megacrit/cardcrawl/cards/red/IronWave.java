/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.IronWaveEffect;

public class IronWave
extends AbstractCard {
    public static final String ID = "Iron Wave";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Iron Wave");

    public IronWave() {
        super(ID, IronWave.cardStrings.NAME, "red/attack/iron_wave", 1, IronWave.cardStrings.DESCRIPTION, AbstractCard.CardType.ATTACK, AbstractCard.CardColor.RED, AbstractCard.CardRarity.COMMON, AbstractCard.CardTarget.ENEMY);
        this.baseDamage = 5;
        this.baseBlock = 5;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        this.addToBot(new GainBlockAction((AbstractCreature)p, p, this.block));
        this.addToBot(new WaitAction(0.1f));
        if (p != null && m != null) {
            this.addToBot(new VFXAction(new IronWaveEffect(p.hb.cX, p.hb.cY, m.hb.cX), 0.5f));
        }
        this.addToBot(new DamageAction((AbstractCreature)m, new DamageInfo(p, this.damage, this.damageTypeForTurn), AbstractGameAction.AttackEffect.SLASH_VERTICAL));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeDamage(2);
            this.upgradeBlock(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new IronWave();
    }
}


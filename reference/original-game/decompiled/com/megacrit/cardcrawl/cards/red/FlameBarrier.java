/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards.red;

import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.FlameBarrierPower;
import com.megacrit.cardcrawl.vfx.combat.FlameBarrierEffect;

public class FlameBarrier
extends AbstractCard {
    public static final String ID = "Flame Barrier";
    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings("Flame Barrier");

    public FlameBarrier() {
        super(ID, FlameBarrier.cardStrings.NAME, "red/skill/flame_barrier", 2, FlameBarrier.cardStrings.DESCRIPTION, AbstractCard.CardType.SKILL, AbstractCard.CardColor.RED, AbstractCard.CardRarity.UNCOMMON, AbstractCard.CardTarget.SELF);
        this.baseBlock = 12;
        this.magicNumber = this.baseMagicNumber = 4;
    }

    @Override
    public void use(AbstractPlayer p, AbstractMonster m) {
        if (Settings.FAST_MODE) {
            this.addToBot(new VFXAction(p, new FlameBarrierEffect(p.hb.cX, p.hb.cY), 0.1f));
        } else {
            this.addToBot(new VFXAction(p, new FlameBarrierEffect(p.hb.cX, p.hb.cY), 0.5f));
        }
        this.addToBot(new GainBlockAction((AbstractCreature)p, p, this.block));
        this.addToBot(new ApplyPowerAction(p, p, new FlameBarrierPower(p, this.magicNumber), this.magicNumber));
    }

    @Override
    public void upgrade() {
        if (!this.upgraded) {
            this.upgradeName();
            this.upgradeBlock(4);
            this.upgradeMagicNumber(2);
        }
    }

    @Override
    public AbstractCard makeCopy() {
        return new FlameBarrier();
    }
}


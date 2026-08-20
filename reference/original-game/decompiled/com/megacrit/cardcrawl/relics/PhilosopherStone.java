/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class PhilosopherStone
extends AbstractRelic {
    public static final String ID = "Philosopher's Stone";
    public static final int STR = 1;

    public PhilosopherStone() {
        super(ID, "philosopherStone.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 1 + this.DESCRIPTIONS[1];
    }

    @Override
    public void updateDescription(AbstractPlayer.PlayerClass c) {
        this.description = this.getUpdatedDescription();
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public void atBattleStart() {
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            this.addToTop(new RelicAboveCreatureAction(m, this));
            m.addPower(new StrengthPower(m, 1));
        }
        AbstractDungeon.onModifyPower();
    }

    @Override
    public void onSpawnMonster(AbstractMonster monster) {
        monster.addPower(new StrengthPower(monster, 1));
        AbstractDungeon.onModifyPower();
    }

    @Override
    public void onEquip() {
        ++AbstractDungeon.player.energy.energyMaster;
    }

    @Override
    public void onUnequip() {
        --AbstractDungeon.player.energy.energyMaster;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new PhilosopherStone();
    }
}


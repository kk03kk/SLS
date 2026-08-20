/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class GremlinHorn
extends AbstractRelic {
    public static final String ID = "Gremlin Horn";

    public GremlinHorn() {
        super(ID, "gremlin_horn.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.HEAVY);
        this.energyBased = true;
    }

    @Override
    public String getUpdatedDescription() {
        if (AbstractDungeon.player != null) {
            return this.setDescription(AbstractDungeon.player.chosenClass);
        }
        return this.setDescription(null);
    }

    private String setDescription(AbstractPlayer.PlayerClass c) {
        return this.DESCRIPTIONS[0] + this.DESCRIPTIONS[1];
    }

    @Override
    public void updateDescription(AbstractPlayer.PlayerClass c) {
        this.description = this.setDescription(c);
        this.tips.clear();
        this.tips.add(new PowerTip(this.name, this.description));
        this.initializeTips();
    }

    @Override
    public void onMonsterDeath(AbstractMonster m) {
        if (m.currentHealth == 0 && !AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.flash();
            this.addToBot(new RelicAboveCreatureAction(m, this));
            this.addToBot(new GainEnergyAction(1));
            this.addToBot(new DrawCardAction(AbstractDungeon.player, 1));
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new GremlinHorn();
    }
}


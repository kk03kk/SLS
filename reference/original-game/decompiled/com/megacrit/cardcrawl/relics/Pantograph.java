/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Pantograph
extends AbstractRelic {
    public static final String ID = "Pantograph";
    private static final int HEAL_AMT = 25;

    public Pantograph() {
        super(ID, "pantograph.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 25 + this.DESCRIPTIONS[1];
    }

    @Override
    public void atBattleStart() {
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (m.type != AbstractMonster.EnemyType.BOSS) continue;
            this.flash();
            this.addToTop(new HealAction(AbstractDungeon.player, AbstractDungeon.player, 25, 0.0f));
            this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            return;
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Pantograph();
    }
}


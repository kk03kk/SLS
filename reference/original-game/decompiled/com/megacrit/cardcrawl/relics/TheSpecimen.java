/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerToRandomEnemyAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.PoisonPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TheSpecimen
extends AbstractRelic {
    private static final Logger logger = LogManager.getLogger(TheSpecimen.class.getName());
    public static final String ID = "The Specimen";

    public TheSpecimen() {
        super(ID, "the_specimen.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onMonsterDeath(AbstractMonster m) {
        if (m.hasPower("Poison")) {
            int amount = m.getPower((String)"Poison").amount;
            if (!AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
                this.flash();
                this.addToBot(new RelicAboveCreatureAction(m, this));
                this.addToBot(new ApplyPowerToRandomEnemyAction(AbstractDungeon.player, new PoisonPower(null, AbstractDungeon.player, amount), amount, false, AbstractGameAction.AttackEffect.POISON));
            } else {
                logger.info("no target for the specimen");
            }
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new TheSpecimen();
    }
}


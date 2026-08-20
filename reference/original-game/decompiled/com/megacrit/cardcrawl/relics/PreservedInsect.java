/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class PreservedInsect
extends AbstractRelic {
    public static final String ID = "PreservedInsect";
    private float MODIFIER_AMT = 0.25f;

    public PreservedInsect() {
        super(ID, "insect.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 25 + this.DESCRIPTIONS[1];
    }

    @Override
    public void atBattleStart() {
        if (AbstractDungeon.getCurrRoom().eliteTrigger) {
            this.flash();
            for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
                if (m.currentHealth <= (int)((float)m.maxHealth * (1.0f - this.MODIFIER_AMT))) continue;
                m.currentHealth = (int)((float)m.maxHealth * (1.0f - this.MODIFIER_AMT));
                m.healthBarUpdatedEvent();
            }
            this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
        }
    }

    @Override
    public boolean canSpawn() {
        return Settings.isEndless || AbstractDungeon.floorNum <= 52;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new PreservedInsect();
    }
}


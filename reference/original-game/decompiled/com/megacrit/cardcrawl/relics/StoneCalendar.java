/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAllEnemiesAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class StoneCalendar
extends AbstractRelic {
    public static final String ID = "StoneCalendar";
    private static final int TURNS = 7;
    private static final int DMG = 52;

    public StoneCalendar() {
        super(ID, "calendar.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 7 + this.DESCRIPTIONS[1] + 52 + this.DESCRIPTIONS[2];
    }

    @Override
    public void atBattleStart() {
        this.counter = 0;
    }

    @Override
    public void atTurnStart() {
        ++this.counter;
        if (this.counter == 7) {
            this.beginLongPulse();
        }
    }

    @Override
    public void onPlayerEndTurn() {
        if (this.counter == 7) {
            this.flash();
            this.addToBot(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.addToBot(new DamageAllEnemiesAction(null, DamageInfo.createDamageMatrix(52, true), DamageInfo.DamageType.THORNS, AbstractGameAction.AttackEffect.BLUNT_HEAVY));
            this.stopPulse();
            this.grayscale = true;
        }
    }

    @Override
    public void justEnteredRoom(AbstractRoom room) {
        this.grayscale = false;
    }

    @Override
    public void onVictory() {
        this.counter = -1;
        this.stopPulse();
    }

    @Override
    public AbstractRelic makeCopy() {
        return new StoneCalendar();
    }
}


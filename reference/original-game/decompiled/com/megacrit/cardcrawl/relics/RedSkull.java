/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class RedSkull
extends AbstractRelic {
    public static final String ID = "Red Skull";
    private static final int STR_AMT = 3;
    private boolean isActive = false;

    public RedSkull() {
        super(ID, "red_skull.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 3 + this.DESCRIPTIONS[1];
    }

    @Override
    public void atBattleStart() {
        this.isActive = false;
        this.addToBot(new AbstractGameAction(){

            @Override
            public void update() {
                if (!RedSkull.this.isActive && AbstractDungeon.player.isBloodied) {
                    RedSkull.this.flash();
                    RedSkull.this.pulse = true;
                    AbstractDungeon.player.addPower(new StrengthPower(AbstractDungeon.player, 3));
                    this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, RedSkull.this));
                    RedSkull.this.isActive = true;
                    AbstractDungeon.onModifyPower();
                }
                this.isDone = true;
            }
        });
    }

    @Override
    public void onBloodied() {
        this.flash();
        this.pulse = true;
        if (!this.isActive && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            AbstractPlayer p = AbstractDungeon.player;
            this.addToTop(new ApplyPowerAction(p, p, new StrengthPower(p, 3), 3));
            this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            this.isActive = true;
            AbstractDungeon.player.hand.applyPowers();
        }
    }

    @Override
    public void onNotBloodied() {
        if (this.isActive && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            AbstractPlayer p = AbstractDungeon.player;
            this.addToTop(new ApplyPowerAction(p, p, new StrengthPower(p, -3), -3));
        }
        this.stopPulse();
        this.isActive = false;
        AbstractDungeon.player.hand.applyPowers();
    }

    @Override
    public void onVictory() {
        this.pulse = false;
        this.isActive = false;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new RedSkull();
    }
}


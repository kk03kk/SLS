/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class Pocketwatch
extends AbstractRelic {
    public static final String ID = "Pocketwatch";
    private static final int AMT = 3;
    private boolean firstTurn = true;

    public Pocketwatch() {
        super(ID, "pocketwatch.png", AbstractRelic.RelicTier.RARE, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void atBattleStart() {
        this.counter = 0;
        this.firstTurn = true;
    }

    @Override
    public void atTurnStartPostDraw() {
        if (this.counter <= 3 && !this.firstTurn) {
            this.addToBot(new DrawCardAction(AbstractDungeon.player, 3));
        } else {
            this.firstTurn = false;
        }
        this.counter = 0;
        this.beginLongPulse();
    }

    @Override
    public void onPlayCard(AbstractCard card, AbstractMonster m) {
        ++this.counter;
        if (this.counter > 3) {
            this.stopPulse();
        }
    }

    @Override
    public void onVictory() {
        this.counter = -1;
        this.stopPulse();
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Pocketwatch();
    }
}


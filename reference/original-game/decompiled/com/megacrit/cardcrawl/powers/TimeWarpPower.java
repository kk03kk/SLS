/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.TimeWarpTurnEndEffect;

public class TimeWarpPower
extends AbstractPower {
    public static final String POWER_ID = "Time Warp";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Time Warp");
    public static final String NAME = TimeWarpPower.powerStrings.NAME;
    public static final String[] DESC = TimeWarpPower.powerStrings.DESCRIPTIONS;
    private static final int STR_AMT = 2;
    private static final int COUNTDOWN_AMT = 12;

    public TimeWarpPower(AbstractCreature owner) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = 0;
        this.updateDescription();
        this.loadRegion("time");
        this.type = AbstractPower.PowerType.BUFF;
    }

    @Override
    public void playApplyPowerSfx() {
        CardCrawlGame.sound.play("POWER_TIME_WARP", 0.05f);
    }

    @Override
    public void updateDescription() {
        this.description = DESC[0] + 12 + DESC[1] + 2 + DESC[2];
    }

    @Override
    public void onAfterUseCard(AbstractCard card, UseCardAction action) {
        this.flashWithoutSound();
        ++this.amount;
        if (this.amount == 12) {
            this.amount = 0;
            this.playApplyPowerSfx();
            AbstractDungeon.actionManager.callEndTurnEarlySequence();
            CardCrawlGame.sound.play("POWER_TIME_WARP", 0.05f);
            AbstractDungeon.effectsQueue.add(new BorderFlashEffect(Color.GOLD, true));
            AbstractDungeon.topLevelEffectsQueue.add(new TimeWarpTurnEndEffect());
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                this.addToBot(new ApplyPowerAction(m, m, new StrengthPower(m, 2), 2));
            }
        }
        this.updateDescription();
    }
}


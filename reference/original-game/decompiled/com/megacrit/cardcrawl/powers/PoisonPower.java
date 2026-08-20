/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.unique.PoisonLoseHpAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class PoisonPower
extends AbstractPower {
    public static final String POWER_ID = "Poison";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("Poison");
    public static final String NAME = PoisonPower.powerStrings.NAME;
    public static final String[] DESCRIPTIONS = PoisonPower.powerStrings.DESCRIPTIONS;
    private AbstractCreature source;

    public PoisonPower(AbstractCreature owner, AbstractCreature source, int poisonAmt) {
        this.name = NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.source = source;
        this.amount = poisonAmt;
        if (this.amount >= 9999) {
            this.amount = 9999;
        }
        this.updateDescription();
        this.loadRegion("poison");
        this.type = AbstractPower.PowerType.DEBUFF;
        this.isTurnBased = true;
    }

    @Override
    public void playApplyPowerSfx() {
        CardCrawlGame.sound.play("POWER_POISON", 0.05f);
    }

    @Override
    public void updateDescription() {
        this.description = this.owner == null || this.owner.isPlayer ? DESCRIPTIONS[0] + this.amount + DESCRIPTIONS[1] : DESCRIPTIONS[2] + this.amount + DESCRIPTIONS[1];
    }

    @Override
    public void stackPower(int stackAmount) {
        super.stackPower(stackAmount);
        if (this.amount > 98 && AbstractDungeon.player.chosenClass == AbstractPlayer.PlayerClass.THE_SILENT) {
            UnlockTracker.unlockAchievement("CATALYST");
        }
    }

    @Override
    public void atStartOfTurn() {
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT && !AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.flashWithoutSound();
            this.addToBot(new PoisonLoseHpAction(this.owner, this.source, this.amount, AbstractGameAction.AttackEffect.POISON));
        }
    }
}


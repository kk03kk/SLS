/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.beyond;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.FadingPower;
import com.megacrit.cardcrawl.powers.ShiftingPower;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class Transient
extends AbstractMonster {
    public static final String ID = "Transient";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("Transient");
    public static final String NAME = Transient.monsterStrings.NAME;
    public static final String[] MOVES = Transient.monsterStrings.MOVES;
    public static final String[] DIALOG = Transient.monsterStrings.DIALOG;
    private static final int HP = 999;
    private int count = 0;
    private static final int DEATH_DMG = 30;
    private static final int INCREMENT_DMG = 10;
    private static final int A_2_DEATH_DMG = 40;
    private int startingDeathDmg;
    private static final byte ATTACK = 1;

    public Transient() {
        super(NAME, ID, 999, 0.0f, -15.0f, 370.0f, 340.0f, null, 0.0f, 20.0f);
        this.loadAnimation("images/monsters/theForest/transient/skeleton.atlas", "images/monsters/theForest/transient/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        this.gold = 1;
        this.dialogX = -100.0f * Settings.scale;
        this.dialogY -= 20.0f * Settings.scale;
        this.startingDeathDmg = AbstractDungeon.ascensionLevel >= 2 ? 40 : 30;
        this.damage.add(new DamageInfo(this, this.startingDeathDmg));
        this.damage.add(new DamageInfo(this, this.startingDeathDmg + 10));
        this.damage.add(new DamageInfo(this, this.startingDeathDmg + 20));
        this.damage.add(new DamageInfo(this, this.startingDeathDmg + 30));
        this.damage.add(new DamageInfo(this, this.startingDeathDmg + 40));
        this.damage.add(new DamageInfo(this, this.startingDeathDmg + 50));
        this.damage.add(new DamageInfo(this, this.startingDeathDmg + 60));
    }

    @Override
    public void usePreBattleAction() {
        if (AbstractDungeon.ascensionLevel >= 17) {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new FadingPower(this, 6)));
        } else {
            AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new FadingPower(this, 5)));
        }
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new ShiftingPower(this)));
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.4f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(this.count), AbstractGameAction.AttackEffect.BLUNT_HEAVY));
                ++this.count;
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK, this.startingDeathDmg + this.count * 10);
            }
        }
    }

    @Override
    public void damage(DamageInfo info) {
        super.damage(info);
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hurt", false);
            this.state.addAnimation(0, "Idle", true, 0.0f);
        }
    }

    @Override
    public void changeState(String key) {
        switch (key) {
            case "ATTACK": {
                this.state.setAnimation(0, "Attack", false);
                this.state.addAnimation(0, "Idle", true, 0.0f);
                break;
            }
        }
    }

    @Override
    public void die() {
        super.die();
        UnlockTracker.unlockAchievement("TRANSIENT");
    }

    @Override
    protected void getMove(int num) {
        this.setMove((byte)1, AbstractMonster.Intent.ATTACK, this.startingDeathDmg + this.count * 10);
    }
}


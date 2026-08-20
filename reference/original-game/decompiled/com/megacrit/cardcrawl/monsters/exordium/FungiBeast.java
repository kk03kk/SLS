/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters.exordium;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.common.ChangeStateAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RollMoveAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.MonsterStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.SporeCloudPower;
import com.megacrit.cardcrawl.powers.StrengthPower;

public class FungiBeast
extends AbstractMonster {
    public static final String ID = "FungiBeast";
    public static final String DOUBLE_ENCOUNTER = "TwoFungiBeasts";
    private static final MonsterStrings monsterStrings = CardCrawlGame.languagePack.getMonsterStrings("FungiBeast");
    public static final String NAME = FungiBeast.monsterStrings.NAME;
    public static final String[] MOVES = FungiBeast.monsterStrings.MOVES;
    public static final String[] DIALOG = FungiBeast.monsterStrings.DIALOG;
    private static final int HP_MIN = 22;
    private static final int HP_MAX = 28;
    private static final int A_2_HP_MIN = 24;
    private static final int A_2_HP_MAX = 28;
    private static final float HB_X = 0.0f;
    private static final float HB_Y = -16.0f;
    private static final float HB_W = 260.0f;
    private static final float HB_H = 170.0f;
    private int biteDamage;
    private int strAmt;
    private static final int BITE_DMG = 6;
    private static final int GROW_STR = 3;
    private static final int A_2_GROW_STR = 4;
    private static final byte BITE = 1;
    private static final byte GROW = 2;
    private static final int VULN_AMT = 2;

    public FungiBeast(float x, float y) {
        super(NAME, ID, 28, 0.0f, -16.0f, 260.0f, 170.0f, null, x, y);
        if (AbstractDungeon.ascensionLevel >= 7) {
            this.setHp(24, 28);
        } else {
            this.setHp(22, 28);
        }
        if (AbstractDungeon.ascensionLevel >= 2) {
            this.strAmt = 4;
            this.biteDamage = 6;
        } else {
            this.strAmt = 3;
            this.biteDamage = 6;
        }
        this.loadAnimation("images/monsters/theBottom/fungi/skeleton.atlas", "images/monsters/theBottom/fungi/skeleton.json", 1.0f);
        AnimationState.TrackEntry e = this.state.setAnimation(0, "Idle", true);
        e.setTime(e.getEndTime() * MathUtils.random());
        e.setTimeScale(MathUtils.random(0.7f, 1.0f));
        this.damage.add(new DamageInfo(this, this.biteDamage));
    }

    @Override
    public void usePreBattleAction() {
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new SporeCloudPower(this, 2)));
    }

    @Override
    public void takeTurn() {
        switch (this.nextMove) {
            case 1: {
                AbstractDungeon.actionManager.addToBottom(new ChangeStateAction(this, "ATTACK"));
                AbstractDungeon.actionManager.addToBottom(new WaitAction(0.5f));
                AbstractDungeon.actionManager.addToBottom(new DamageAction((AbstractCreature)AbstractDungeon.player, (DamageInfo)this.damage.get(0), AbstractGameAction.AttackEffect.BLUNT_LIGHT));
                break;
            }
            case 2: {
                if (AbstractDungeon.ascensionLevel >= 17) {
                    AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new StrengthPower(this, this.strAmt + 1), this.strAmt + 1));
                    break;
                }
                AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(this, this, new StrengthPower(this, this.strAmt), this.strAmt));
            }
        }
        AbstractDungeon.actionManager.addToBottom(new RollMoveAction(this));
    }

    @Override
    protected void getMove(int num) {
        if (num < 60) {
            if (this.lastTwoMoves((byte)1)) {
                this.setMove(MOVES[0], (byte)2, AbstractMonster.Intent.BUFF);
            } else {
                this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
            }
        } else if (this.lastMove((byte)2)) {
            this.setMove((byte)1, AbstractMonster.Intent.ATTACK, ((DamageInfo)this.damage.get((int)0)).base);
        } else {
            this.setMove(MOVES[0], (byte)2, AbstractMonster.Intent.BUFF);
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
    public void damage(DamageInfo info) {
        super.damage(info);
        if (info.owner != null && info.type != DamageInfo.DamageType.THORNS && info.output > 0) {
            this.state.setAnimation(0, "Hit", false);
            this.state.addAnimation(0, "Idle", true, 0.0f);
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class SetMoveAction
extends AbstractGameAction {
    private AbstractMonster monster;
    private byte theNextMove;
    private AbstractMonster.Intent theNextIntent;
    private int theNextDamage;
    private String theNextName;
    private int theMultiplier;
    private boolean isMultiplier = false;

    public SetMoveAction(AbstractMonster monster, String moveName, byte nextMove, AbstractMonster.Intent intent, int baseDamage, int multiplierAmt, boolean multiplier) {
        this.theNextName = moveName;
        this.theNextMove = nextMove;
        this.theNextIntent = intent;
        this.theNextDamage = baseDamage;
        this.monster = monster;
        this.theMultiplier = multiplierAmt;
        this.isMultiplier = multiplier;
    }

    public SetMoveAction(AbstractMonster monster, byte nextMove, AbstractMonster.Intent intent, int baseDamage, int multiplierAmt, boolean multiplier) {
        this(monster, null, nextMove, intent, baseDamage, multiplierAmt, multiplier);
    }

    public SetMoveAction(AbstractMonster monster, String moveName, byte nextMove, AbstractMonster.Intent intent, int baseDamage) {
        this(monster, moveName, nextMove, intent, baseDamage, 0, false);
    }

    public SetMoveAction(AbstractMonster monster, String moveName, byte nextMove, AbstractMonster.Intent intent) {
        this(monster, moveName, nextMove, intent, -1);
    }

    public SetMoveAction(AbstractMonster monster, byte nextMove, AbstractMonster.Intent intent, int baseDamage) {
        this(monster, null, nextMove, intent, baseDamage);
    }

    public SetMoveAction(AbstractMonster monster, byte nextMove, AbstractMonster.Intent intent) {
        this(monster, null, nextMove, intent, -1);
    }

    @Override
    public void update() {
        this.monster.setMove(this.theNextName, this.theNextMove, this.theNextIntent, this.theNextDamage, this.theMultiplier, this.isMultiplier);
        this.isDone = true;
    }
}


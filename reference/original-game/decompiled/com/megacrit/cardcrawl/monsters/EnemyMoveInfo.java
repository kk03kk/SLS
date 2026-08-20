/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.monsters;

import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class EnemyMoveInfo {
    public byte nextMove;
    public AbstractMonster.Intent intent;
    public int baseDamage;
    public int multiplier;
    public boolean isMultiDamage;

    public EnemyMoveInfo(byte nextMove, AbstractMonster.Intent intent, int intentBaseDmg, int multiplier, boolean isMultiDamage) {
        this.nextMove = nextMove;
        this.intent = intent;
        this.baseDamage = intentBaseDmg;
        this.multiplier = multiplier;
        this.isMultiDamage = isMultiDamage;
    }
}


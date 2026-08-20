/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.cards;

import com.megacrit.cardcrawl.cards.AbstractCard;

public class CardModUNUSED {
    public String key;
    private EffectType type;
    public DurationType dur;
    private int amount;
    private boolean applied = false;

    public CardModUNUSED(EffectType type, DurationType dur, int amount, String key) {
        this.type = type;
        this.dur = dur;
        this.amount = amount;
        this.key = key;
    }

    public void apply(AbstractCard card) {
        if (!this.applied) {
            this.applied = true;
            switch (this.type) {
                case DAMAGE: {
                    card.damage += this.amount;
                    break;
                }
            }
        }
    }

    public int applyDamageMod(int baseDamage) {
        return baseDamage + this.amount;
    }

    public void unapply(AbstractCard card) {
        switch (this.type) {
            case DAMAGE: {
                card.damage -= this.amount;
                break;
            }
        }
    }

    public static enum DurationType {
        ONE_TURN,
        COMBAT,
        ATTACKS_PLAYED,
        CARDS_PLAYED;

    }

    public static enum EffectType {
        DAMAGE;

    }
}


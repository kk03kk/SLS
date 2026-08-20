/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.GetAllInBattleInstances;
import java.util.UUID;

public class ModifyBlockAction
extends AbstractGameAction {
    UUID uuid;

    public ModifyBlockAction(UUID targetUUID, int amount) {
        this.setValues(this.target, this.source, amount);
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.uuid = targetUUID;
    }

    @Override
    public void update() {
        for (AbstractCard c : GetAllInBattleInstances.get(this.uuid)) {
            c.baseBlock += this.amount;
            if (c.baseDamage >= 0) continue;
            c.baseDamage = 0;
        }
        this.isDone = true;
    }
}


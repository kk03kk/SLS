/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

public class AddCardToDeckAction
extends AbstractGameAction {
    AbstractCard cardToObtain;

    public AddCardToDeckAction(AbstractCard card) {
        this.cardToObtain = card;
        this.duration = 0.5f;
    }

    @Override
    public void update() {
        AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(this.cardToObtain, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
        this.isDone = true;
    }
}


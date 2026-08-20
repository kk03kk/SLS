/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;
import com.megacrit.cardcrawl.vfx.combat.SanctityEffect;

public class SanctityAction
extends AbstractGameAction {
    private int amtToDraw;

    public SanctityAction(int amount) {
        this.amtToDraw = amount;
    }

    @Override
    public void update() {
        if (AbstractDungeon.actionManager.cardsPlayedThisCombat.size() >= 2 && AbstractDungeon.actionManager.cardsPlayedThisCombat.get((int)(AbstractDungeon.actionManager.cardsPlayedThisCombat.size() - 2)).type == AbstractCard.CardType.SKILL) {
            this.addToTop(new DrawCardAction(this.amtToDraw));
            this.addToTop(new VFXAction(new SanctityEffect(AbstractDungeon.player.hb.cX, AbstractDungeon.player.hb.cY)));
            this.addToTop(new SFXAction("HEAL_1"));
            this.addToTop(new VFXAction(new BorderFlashEffect(Color.GOLD, true), 0.1f));
        }
        this.isDone = true;
    }
}


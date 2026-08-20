/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.GainEnergyAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.combat.MiracleEffect;

public class FollowUpAction
extends AbstractGameAction {
    @Override
    public void update() {
        if (AbstractDungeon.actionManager.cardsPlayedThisCombat.size() >= 2 && AbstractDungeon.actionManager.cardsPlayedThisCombat.get((int)(AbstractDungeon.actionManager.cardsPlayedThisCombat.size() - 2)).type == AbstractCard.CardType.ATTACK) {
            this.addToTop(new GainEnergyAction(1));
            if (Settings.FAST_MODE) {
                this.addToTop(new VFXAction(new MiracleEffect(Color.CYAN, Color.PURPLE, "ATTACK_MAGIC_SLOW_1"), 0.0f));
            } else {
                this.addToTop(new VFXAction(new MiracleEffect(Color.CYAN, Color.PURPLE, "ATTACK_MAGIC_SLOW_1"), 0.3f));
            }
        }
        this.isDone = true;
    }
}


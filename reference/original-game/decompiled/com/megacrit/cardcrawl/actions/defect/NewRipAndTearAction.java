/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.AttackDamageRandomEnemyAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.combat.RipAndTearEffect;

public class NewRipAndTearAction
extends AttackDamageRandomEnemyAction {
    public NewRipAndTearAction(AbstractCard card) {
        super(card);
    }

    @Override
    public void update() {
        if (!Settings.FAST_MODE) {
            this.addToTop(new WaitAction(0.1f));
        }
        super.update();
        if (Settings.FAST_MODE) {
            this.addToTop(new WaitAction(0.05f));
        } else {
            this.addToTop(new WaitAction(0.2f));
        }
        if (this.target != null) {
            this.addToTop(new VFXAction(new RipAndTearEffect(this.target.hb.cX, this.target.hb.cY, Color.RED, Color.GOLD)));
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.animations.VFXAction;
import com.megacrit.cardcrawl.actions.common.AttackDamageRandomEnemyAction;
import com.megacrit.cardcrawl.actions.utility.SFXAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;
import com.megacrit.cardcrawl.vfx.combat.LightningEffect;

public class NewThunderStrikeAction
extends AttackDamageRandomEnemyAction {
    public NewThunderStrikeAction(AbstractCard card) {
        super(card);
    }

    @Override
    public void update() {
        if (!Settings.FAST_MODE) {
            this.addToTop(new WaitAction(0.1f));
        }
        super.update();
        if (this.target != null) {
            this.addToTop(new VFXAction(new LightningEffect(this.target.drawX, this.target.drawY)));
            this.addToTop(new VFXAction(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, this.attackEffect)));
            this.addToTop(new SFXAction("ORB_LIGHTNING_EVOKE", 0.1f));
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.common;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;

public class ObtainPotionAction
extends AbstractGameAction {
    private AbstractPotion potion;
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("AbstractPotion");
    public static final String[] TEXT = ObtainPotionAction.uiStrings.TEXT;

    public ObtainPotionAction(AbstractPotion potion) {
        this.actionType = AbstractGameAction.ActionType.SPECIAL;
        this.duration = Settings.ACTION_DUR_XFAST;
        this.potion = potion;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_XFAST) {
            if (AbstractDungeon.player.hasRelic("Sozu")) {
                AbstractDungeon.player.getRelic("Sozu").flash();
            } else {
                AbstractDungeon.player.obtainPotion(this.potion);
            }
        }
        this.tickDuration();
    }
}


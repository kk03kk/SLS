/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.vfx.ThoughtBubble;

public class SpotWeaknessAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("OpeningAction");
    public static final String[] TEXT = SpotWeaknessAction.uiStrings.TEXT;
    private int damageIncrease;
    private AbstractMonster targetMonster;

    public SpotWeaknessAction(int damageIncrease, AbstractMonster m) {
        this.duration = 0.0f;
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.damageIncrease = damageIncrease;
        this.targetMonster = m;
    }

    @Override
    public void update() {
        if (this.targetMonster != null && this.targetMonster.getIntentBaseDmg() >= 0) {
            this.addToBot(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player, new StrengthPower(AbstractDungeon.player, this.damageIncrease), this.damageIncrease));
        } else {
            AbstractDungeon.effectList.add(new ThoughtBubble(AbstractDungeon.player.dialogX, AbstractDungeon.player.dialogY, 3.0f, TEXT[0], true));
        }
        this.isDone = true;
    }
}


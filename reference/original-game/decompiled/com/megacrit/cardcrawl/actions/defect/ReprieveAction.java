/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.FocusPower;
import com.megacrit.cardcrawl.vfx.ThoughtBubble;

public class ReprieveAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ReprieveAction");
    public static final String[] TEXT = ReprieveAction.uiStrings.TEXT;
    private int focusIncrease;
    private AbstractMonster targetMonster;

    public ReprieveAction(int increaseAmt, AbstractMonster m) {
        this.duration = 0.0f;
        this.actionType = AbstractGameAction.ActionType.WAIT;
        this.focusIncrease = increaseAmt;
        this.targetMonster = m;
    }

    @Override
    public void update() {
        if (this.targetMonster != null && this.targetMonster.intent != AbstractMonster.Intent.ATTACK && this.targetMonster.intent != AbstractMonster.Intent.ATTACK_BUFF && this.targetMonster.intent != AbstractMonster.Intent.ATTACK_DEBUFF && this.targetMonster.intent != AbstractMonster.Intent.ATTACK_DEFEND) {
            this.addToBot(new ApplyPowerAction(AbstractDungeon.player, AbstractDungeon.player, new FocusPower(AbstractDungeon.player, this.focusIncrease), this.focusIncrease));
        } else {
            AbstractDungeon.effectList.add(new ThoughtBubble(AbstractDungeon.player.dialogX, AbstractDungeon.player.dialogY, 3.0f, TEXT[0], true));
        }
        this.isDone = true;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.MakeTempCardInHandAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

public class DivinePunishmentAction
extends AbstractGameAction {
    private AbstractCard card;
    private boolean freeToPlayOnce;
    private int energyOnUse;

    public DivinePunishmentAction(AbstractCard cardToGain, boolean freeToPlayOnce, int energyOnUse) {
        this.card = cardToGain;
        this.freeToPlayOnce = freeToPlayOnce;
        this.energyOnUse = energyOnUse;
        this.duration = Settings.ACTION_DUR_XFAST;
        this.actionType = AbstractGameAction.ActionType.SPECIAL;
    }

    @Override
    public void update() {
        AbstractPlayer p = AbstractDungeon.player;
        int effect = EnergyPanel.totalCount;
        if (this.energyOnUse != -1) {
            effect = this.energyOnUse;
        }
        if (p.hasRelic("Chemical X")) {
            effect += 2;
            p.getRelic("Chemical X").flash();
        }
        if (effect > 0) {
            for (int i = 0; i < effect; ++i) {
                this.addToBot(new MakeTempCardInHandAction(this.card.makeStatEquivalentCopy()));
            }
            if (!this.freeToPlayOnce) {
                p.energy.use(EnergyPanel.totalCount);
            }
        }
        this.isDone = true;
    }
}


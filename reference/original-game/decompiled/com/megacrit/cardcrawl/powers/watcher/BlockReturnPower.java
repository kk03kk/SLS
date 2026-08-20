/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.powers.watcher;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;

public class BlockReturnPower
extends AbstractPower {
    public static final String POWER_ID = "BlockReturnPower";
    private static final PowerStrings powerStrings = CardCrawlGame.languagePack.getPowerStrings("BlockReturnPower");

    public BlockReturnPower(AbstractCreature owner, int blockAmt) {
        this.name = BlockReturnPower.powerStrings.NAME;
        this.ID = POWER_ID;
        this.owner = owner;
        this.amount = blockAmt;
        this.updateDescription();
        this.loadRegion("talk_to_hand");
        this.type = AbstractPower.PowerType.DEBUFF;
    }

    @Override
    public void stackPower(int stackAmount) {
        this.fontScale = 8.0f;
        this.amount += stackAmount;
        this.updateDescription();
    }

    @Override
    public int onAttacked(DamageInfo info, int damageAmount) {
        if (info.type != DamageInfo.DamageType.THORNS && info.type != DamageInfo.DamageType.HP_LOSS && info.owner != null && info.owner != this.owner) {
            this.flash();
            this.addToTop(new GainBlockAction((AbstractCreature)AbstractDungeon.player, this.amount, Settings.FAST_MODE));
        }
        return damageAmount;
    }

    @Override
    public void updateDescription() {
        this.description = BlockReturnPower.powerStrings.DESCRIPTIONS[0] + this.amount + BlockReturnPower.powerStrings.DESCRIPTIONS[1];
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;

public class Cauldron
extends AbstractRelic {
    public static final String ID = "Cauldron";

    public Cauldron() {
        super(ID, "cauldron.png", AbstractRelic.RelicTier.SHOP, AbstractRelic.LandingSound.HEAVY);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onEquip() {
        for (int i = 0; i < 5; ++i) {
            AbstractDungeon.getCurrRoom().addPotionToRewards(PotionHelper.getRandomPotion());
        }
        AbstractDungeon.combatRewardScreen.open(this.DESCRIPTIONS[1]);
        AbstractDungeon.getCurrRoom().rewardPopOutTimer = 0.0f;
        int remove = -1;
        for (int i = 0; i < AbstractDungeon.combatRewardScreen.rewards.size(); ++i) {
            if (AbstractDungeon.combatRewardScreen.rewards.get((int)i).type != RewardItem.RewardType.CARD) continue;
            remove = i;
            break;
        }
        if (remove != -1) {
            AbstractDungeon.combatRewardScreen.rewards.remove(remove);
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Cauldron();
    }
}


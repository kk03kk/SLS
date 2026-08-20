/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;

public class ToxicEgg2
extends AbstractRelic {
    public static final String ID = "Toxic Egg 2";

    public ToxicEgg2() {
        super(ID, "toxicEgg.png", AbstractRelic.RelicTier.UNCOMMON, AbstractRelic.LandingSound.SOLID);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onEquip() {
        for (RewardItem reward : AbstractDungeon.combatRewardScreen.rewards) {
            if (reward.cards == null) continue;
            for (AbstractCard c : reward.cards) {
                this.onPreviewObtainCard(c);
            }
        }
    }

    @Override
    public void onPreviewObtainCard(AbstractCard c) {
        this.onObtainCard(c);
    }

    @Override
    public void onObtainCard(AbstractCard c) {
        if (c.type == AbstractCard.CardType.SKILL && c.canUpgrade() && !c.upgraded) {
            c.upgrade();
        }
    }

    @Override
    public boolean canSpawn() {
        return Settings.isEndless || AbstractDungeon.floorNum <= 48;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new ToxicEgg2();
    }
}


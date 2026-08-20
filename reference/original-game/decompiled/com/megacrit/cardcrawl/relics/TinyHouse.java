/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.UpgradeShineEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardBrieflyEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class TinyHouse
extends AbstractRelic {
    public static final String ID = "Tiny House";
    private static final int GOLD_AMT = 50;
    private static final int HP_AMT = 5;

    public TinyHouse() {
        super(ID, "tinyHouse.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 50 + this.DESCRIPTIONS[1] + 5 + this.DESCRIPTIONS[2];
    }

    @Override
    public void onEquip() {
        ArrayList<AbstractCard> upgradableCards = new ArrayList<AbstractCard>();
        for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
            if (!c.canUpgrade()) continue;
            upgradableCards.add(c);
        }
        Collections.shuffle(upgradableCards, new Random(AbstractDungeon.miscRng.randomLong()));
        if (!upgradableCards.isEmpty()) {
            if (upgradableCards.size() == 1) {
                ((AbstractCard)upgradableCards.get(0)).upgrade();
                AbstractDungeon.player.bottledCardUpgradeCheck((AbstractCard)upgradableCards.get(0));
                AbstractDungeon.topLevelEffects.add(new ShowCardBrieflyEffect(((AbstractCard)upgradableCards.get(0)).makeStatEquivalentCopy()));
                AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
            } else {
                ((AbstractCard)upgradableCards.get(0)).upgrade();
                AbstractDungeon.player.bottledCardUpgradeCheck((AbstractCard)upgradableCards.get(0));
                AbstractDungeon.player.bottledCardUpgradeCheck((AbstractCard)upgradableCards.get(1));
                AbstractDungeon.topLevelEffects.add(new ShowCardBrieflyEffect(((AbstractCard)upgradableCards.get(0)).makeStatEquivalentCopy(), (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
            }
        }
        AbstractDungeon.player.increaseMaxHp(5, true);
        AbstractDungeon.getCurrRoom().addGoldToRewards(50);
        AbstractDungeon.getCurrRoom().addPotionToRewards(PotionHelper.getRandomPotion(AbstractDungeon.miscRng));
        AbstractDungeon.combatRewardScreen.open(this.DESCRIPTIONS[3]);
        AbstractDungeon.getCurrRoom().rewardPopOutTimer = 0.0f;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new TinyHouse();
    }
}


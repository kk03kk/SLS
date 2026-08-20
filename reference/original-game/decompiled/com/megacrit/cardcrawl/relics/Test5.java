/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.UpgradeShineEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardBrieflyEffect;
import java.util.ArrayList;
import java.util.Collections;

public class Test5
extends AbstractRelic {
    public static final String ID = "Test 5";
    private static final int CARD_AMT = 2;

    public Test5() {
        super(ID, "test5.png", AbstractRelic.RelicTier.COMMON, AbstractRelic.LandingSound.SOLID);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 2 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onEquip() {
        ArrayList<AbstractCard> upgradableCards = new ArrayList<AbstractCard>();
        for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
            if (!c.canUpgrade() || c.type != AbstractCard.CardType.SKILL) continue;
            upgradableCards.add(c);
        }
        Collections.shuffle(upgradableCards);
        if (!upgradableCards.isEmpty()) {
            if (upgradableCards.size() == 1) {
                ((AbstractCard)upgradableCards.get(0)).upgrade();
                AbstractDungeon.player.bottledCardUpgradeCheck((AbstractCard)upgradableCards.get(0));
                AbstractDungeon.topLevelEffects.add(new ShowCardBrieflyEffect(((AbstractCard)upgradableCards.get(0)).makeStatEquivalentCopy()));
                AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
            } else {
                ((AbstractCard)upgradableCards.get(0)).upgrade();
                ((AbstractCard)upgradableCards.get(1)).upgrade();
                AbstractDungeon.player.bottledCardUpgradeCheck((AbstractCard)upgradableCards.get(0));
                AbstractDungeon.player.bottledCardUpgradeCheck((AbstractCard)upgradableCards.get(1));
                AbstractDungeon.topLevelEffects.add(new ShowCardBrieflyEffect(((AbstractCard)upgradableCards.get(0)).makeStatEquivalentCopy(), (float)Settings.WIDTH / 2.0f - AbstractCard.IMG_WIDTH / 2.0f - 20.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f));
                AbstractDungeon.topLevelEffects.add(new ShowCardBrieflyEffect(((AbstractCard)upgradableCards.get(1)).makeStatEquivalentCopy(), (float)Settings.WIDTH / 2.0f + AbstractCard.IMG_WIDTH / 2.0f + 20.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f));
                AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
            }
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Test5();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.curses.CurseOfTheBell;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.unlock.UnlockTracker;

public class CallingBell
extends AbstractRelic {
    public static final String ID = "Calling Bell";
    private boolean cardsReceived = true;

    public CallingBell() {
        super(ID, "bell.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.SOLID);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onEquip() {
        this.cardsReceived = false;
        CardGroup group = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        CurseOfTheBell bellCurse = new CurseOfTheBell();
        UnlockTracker.markCardAsSeen(bellCurse.cardID);
        group.addToBottom(((AbstractCard)bellCurse).makeCopy());
        AbstractDungeon.gridSelectScreen.openConfirmationGrid(group, this.DESCRIPTIONS[1]);
        CardCrawlGame.sound.playA("BELL", MathUtils.random(-0.2f, -0.3f));
    }

    @Override
    public void update() {
        super.update();
        if (!this.cardsReceived && !AbstractDungeon.isScreenUp) {
            AbstractDungeon.combatRewardScreen.open();
            AbstractDungeon.combatRewardScreen.rewards.clear();
            AbstractDungeon.combatRewardScreen.rewards.add(new RewardItem(AbstractDungeon.returnRandomScreenlessRelic(AbstractRelic.RelicTier.COMMON)));
            AbstractDungeon.combatRewardScreen.rewards.add(new RewardItem(AbstractDungeon.returnRandomScreenlessRelic(AbstractRelic.RelicTier.UNCOMMON)));
            AbstractDungeon.combatRewardScreen.rewards.add(new RewardItem(AbstractDungeon.returnRandomScreenlessRelic(AbstractRelic.RelicTier.RARE)));
            AbstractDungeon.combatRewardScreen.positionRewards();
            AbstractDungeon.overlayMenu.proceedButton.setLabel(this.DESCRIPTIONS[2]);
            this.cardsReceived = true;
            AbstractDungeon.getCurrRoom().rewardPopOutTimer = 0.25f;
        }
        if (this.hb.hovered && InputHelper.justClickedLeft) {
            CardCrawlGame.sound.playA("BELL", MathUtils.random(-0.2f, -0.3f));
            this.flash();
        }
    }

    @Override
    public AbstractRelic makeCopy() {
        return new CallingBell();
    }
}


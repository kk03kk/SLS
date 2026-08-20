/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import java.util.ArrayList;

public class Astrolabe
extends AbstractRelic {
    public static final String ID = "Astrolabe";
    private boolean cardsSelected = true;

    public Astrolabe() {
        super(ID, "astrolabe.png", AbstractRelic.RelicTier.BOSS, AbstractRelic.LandingSound.CLINK);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0];
    }

    @Override
    public void onEquip() {
        this.cardsSelected = false;
        CardGroup tmp = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
        for (AbstractCard card : AbstractDungeon.player.masterDeck.getPurgeableCards().group) {
            tmp.addToTop(card);
        }
        if (tmp.group.isEmpty()) {
            this.cardsSelected = true;
            return;
        }
        if (tmp.group.size() <= 3) {
            this.giveCards(tmp.group);
        } else if (!AbstractDungeon.isScreenUp) {
            AbstractDungeon.gridSelectScreen.open(tmp, 3, this.DESCRIPTIONS[1] + this.name + LocalizedStrings.PERIOD, false, false, false, false);
        } else {
            AbstractDungeon.dynamicBanner.hide();
            AbstractDungeon.previousScreen = AbstractDungeon.screen;
            AbstractDungeon.gridSelectScreen.open(tmp, 3, this.DESCRIPTIONS[1] + this.name + LocalizedStrings.PERIOD, false, false, false, false);
        }
    }

    @Override
    public void update() {
        super.update();
        if (!this.cardsSelected && AbstractDungeon.gridSelectScreen.selectedCards.size() == 3) {
            this.giveCards(AbstractDungeon.gridSelectScreen.selectedCards);
        }
    }

    public void giveCards(ArrayList<AbstractCard> group) {
        this.cardsSelected = true;
        float displayCount = 0.0f;
        for (AbstractCard card : group) {
            card.untip();
            card.unhover();
            AbstractDungeon.player.masterDeck.removeCard(card);
            AbstractDungeon.transformCard(card, true, AbstractDungeon.miscRng);
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.TRANSFORM || AbstractDungeon.transformedCard == null) continue;
            AbstractDungeon.topLevelEffectsQueue.add(new ShowCardAndObtainEffect(AbstractDungeon.getTransformedCard(), (float)Settings.WIDTH / 3.0f + displayCount, (float)Settings.HEIGHT / 2.0f, false));
            displayCount += (float)Settings.WIDTH / 6.0f;
        }
        AbstractDungeon.gridSelectScreen.selectedCards.clear();
        AbstractDungeon.getCurrRoom().rewardPopOutTimer = 0.25f;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Astrolabe();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect;
import java.util.ArrayList;

public class ForeignInfluenceAction
extends AbstractGameAction {
    private boolean retrieveCard = false;
    private boolean upgraded;

    public ForeignInfluenceAction(boolean upgraded) {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.upgraded = upgraded;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            AbstractDungeon.cardRewardScreen.customCombatOpen(this.generateCardChoices(), CardRewardScreen.TEXT[1], true);
            this.tickDuration();
            return;
        }
        if (!this.retrieveCard) {
            if (AbstractDungeon.cardRewardScreen.discoveryCard != null) {
                AbstractCard disCard = AbstractDungeon.cardRewardScreen.discoveryCard.makeStatEquivalentCopy();
                if (this.upgraded) {
                    disCard.setCostForTurn(0);
                }
                disCard.current_x = -1000.0f * Settings.xScale;
                if (AbstractDungeon.player.hand.size() < 10) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(disCard, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                } else {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(disCard, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                }
                AbstractDungeon.cardRewardScreen.discoveryCard = null;
            }
            this.retrieveCard = true;
        }
        this.tickDuration();
    }

    private ArrayList<AbstractCard> generateCardChoices() {
        ArrayList<AbstractCard> derp = new ArrayList<AbstractCard>();
        while (derp.size() != 3) {
            boolean dupe = false;
            int roll = AbstractDungeon.cardRandomRng.random(99);
            AbstractCard.CardRarity cardRarity = roll < 55 ? AbstractCard.CardRarity.COMMON : (roll < 85 ? AbstractCard.CardRarity.UNCOMMON : AbstractCard.CardRarity.RARE);
            AbstractCard tmp = CardLibrary.getAnyColorCard(AbstractCard.CardType.ATTACK, cardRarity);
            for (AbstractCard c : derp) {
                if (!c.cardID.equals(tmp.cardID)) continue;
                dupe = true;
                break;
            }
            if (dupe) continue;
            derp.add(tmp.makeCopy());
        }
        return derp;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect;
import java.util.ArrayList;

public class ChooseOneColorless
extends AbstractGameAction {
    private boolean retrieveCard = false;

    public ChooseOneColorless() {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = 1;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            AbstractDungeon.cardRewardScreen.customCombatOpen(this.generateCardChoices(), CardRewardScreen.TEXT[1], false);
            this.tickDuration();
            return;
        }
        if (!this.retrieveCard) {
            if (AbstractDungeon.cardRewardScreen.discoveryCard != null) {
                AbstractCard disCard = AbstractDungeon.cardRewardScreen.discoveryCard.makeStatEquivalentCopy();
                if (AbstractDungeon.player.hasPower("MasterRealityPower")) {
                    disCard.upgrade();
                }
                disCard.current_x = -1000.0f * Settings.xScale;
                if (this.amount == 1) {
                    if (AbstractDungeon.player.hand.size() < 10) {
                        AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(disCard, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                    } else {
                        AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(disCard, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                    }
                } else if (AbstractDungeon.player.hand.size() + this.amount <= 10) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(disCard, (float)Settings.WIDTH / 2.0f - AbstractCard.IMG_WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                } else if (AbstractDungeon.player.hand.size() == 9) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(disCard, (float)Settings.WIDTH / 2.0f - AbstractCard.IMG_WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                } else {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(disCard, (float)Settings.WIDTH / 2.0f - AbstractCard.IMG_WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
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
            AbstractCard tmp = AbstractDungeon.returnTrulyRandomColorlessCardInCombat();
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


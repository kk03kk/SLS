/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDiscardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToHandEffect;
import java.util.ArrayList;

public class DiscoveryAction
extends AbstractGameAction {
    private boolean retrieveCard = false;
    private boolean returnColorless = false;
    private AbstractCard.CardType cardType = null;

    public DiscoveryAction() {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = 1;
    }

    public DiscoveryAction(AbstractCard.CardType type, int amount) {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = amount;
        this.cardType = type;
    }

    public DiscoveryAction(boolean colorless, int amount) {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
        this.amount = amount;
        this.returnColorless = colorless;
    }

    @Override
    public void update() {
        ArrayList<AbstractCard> generatedCards = this.returnColorless ? this.generateColorlessCardChoices() : this.generateCardChoices(this.cardType);
        if (this.duration == Settings.ACTION_DUR_FAST) {
            AbstractDungeon.cardRewardScreen.customCombatOpen(generatedCards, CardRewardScreen.TEXT[1], this.cardType != null);
            this.tickDuration();
            return;
        }
        if (!this.retrieveCard) {
            if (AbstractDungeon.cardRewardScreen.discoveryCard != null) {
                AbstractCard disCard = AbstractDungeon.cardRewardScreen.discoveryCard.makeStatEquivalentCopy();
                AbstractCard disCard2 = AbstractDungeon.cardRewardScreen.discoveryCard.makeStatEquivalentCopy();
                if (AbstractDungeon.player.hasPower("MasterRealityPower")) {
                    disCard.upgrade();
                    disCard2.upgrade();
                }
                disCard.setCostForTurn(0);
                disCard2.setCostForTurn(0);
                disCard.current_x = -1000.0f * Settings.xScale;
                disCard2.current_x = -1000.0f * Settings.xScale + AbstractCard.IMG_HEIGHT_S;
                if (this.amount == 1) {
                    if (AbstractDungeon.player.hand.size() < 10) {
                        AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(disCard, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                    } else {
                        AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(disCard, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                    }
                    disCard2 = null;
                } else if (AbstractDungeon.player.hand.size() + this.amount <= 10) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(disCard, (float)Settings.WIDTH / 2.0f - AbstractCard.IMG_WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(disCard2, (float)Settings.WIDTH / 2.0f + AbstractCard.IMG_WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                } else if (AbstractDungeon.player.hand.size() == 9) {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToHandEffect(disCard, (float)Settings.WIDTH / 2.0f - AbstractCard.IMG_WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(disCard2, (float)Settings.WIDTH / 2.0f + AbstractCard.IMG_WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                } else {
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(disCard, (float)Settings.WIDTH / 2.0f - AbstractCard.IMG_WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                    AbstractDungeon.effectList.add(new ShowCardAndAddToDiscardEffect(disCard2, (float)Settings.WIDTH / 2.0f + AbstractCard.IMG_WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                }
                AbstractDungeon.cardRewardScreen.discoveryCard = null;
            }
            this.retrieveCard = true;
        }
        this.tickDuration();
    }

    private ArrayList<AbstractCard> generateColorlessCardChoices() {
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

    private ArrayList<AbstractCard> generateCardChoices(AbstractCard.CardType type) {
        ArrayList<AbstractCard> derp = new ArrayList<AbstractCard>();
        while (derp.size() != 3) {
            boolean dupe = false;
            AbstractCard tmp = null;
            tmp = type == null ? AbstractDungeon.returnTrulyRandomCardInCombat() : AbstractDungeon.returnTrulyRandomCardInCombat(type);
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


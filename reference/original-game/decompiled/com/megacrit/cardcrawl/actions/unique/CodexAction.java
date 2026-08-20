/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.screens.CardRewardScreen;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndAddToDrawPileEffect;
import java.util.ArrayList;

public class CodexAction
extends AbstractGameAction {
    public static int numPlaced;
    private boolean retrieveCard = false;

    public CodexAction() {
        this.actionType = AbstractGameAction.ActionType.CARD_MANIPULATION;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (AbstractDungeon.getMonsters().areMonstersBasicallyDead()) {
            this.isDone = true;
            return;
        }
        if (this.duration == Settings.ACTION_DUR_FAST) {
            AbstractDungeon.cardRewardScreen.customCombatOpen(this.generateCardChoices(), CardRewardScreen.TEXT[1], true);
            this.tickDuration();
            return;
        }
        if (!this.retrieveCard) {
            if (AbstractDungeon.cardRewardScreen.discoveryCard != null) {
                AbstractCard codexCard = AbstractDungeon.cardRewardScreen.discoveryCard.makeStatEquivalentCopy();
                codexCard.current_x = -1000.0f * Settings.xScale;
                AbstractDungeon.effectList.add(new ShowCardAndAddToDrawPileEffect(codexCard, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f, true));
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
            AbstractCard tmp = AbstractDungeon.returnTrulyRandomCardInCombat();
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


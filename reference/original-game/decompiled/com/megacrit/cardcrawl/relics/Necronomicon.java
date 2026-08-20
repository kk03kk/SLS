/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.relics;

import com.megacrit.cardcrawl.actions.common.RelicAboveCreatureAction;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardQueueItem;
import com.megacrit.cardcrawl.cards.curses.Necronomicurse;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

public class Necronomicon
extends AbstractRelic {
    public static final String ID = "Necronomicon";
    private static final int COST_THRESHOLD = 2;
    private boolean activated = true;

    public Necronomicon() {
        super(ID, "necronomicon.png", AbstractRelic.RelicTier.SPECIAL, AbstractRelic.LandingSound.FLAT);
    }

    @Override
    public String getUpdatedDescription() {
        return this.DESCRIPTIONS[0] + 2 + this.DESCRIPTIONS[1];
    }

    @Override
    public void onEquip() {
        CardCrawlGame.sound.play("NECRONOMICON");
        this.description = this.DESCRIPTIONS[0] + 2 + this.DESCRIPTIONS[2];
        AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(new Necronomicurse(), (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
        UnlockTracker.markCardAsSeen("Necronomicurse");
    }

    @Override
    public void onUnequip() {
        AbstractCard cardToRemove = null;
        for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
            if (!(c instanceof Necronomicurse)) continue;
            cardToRemove = c;
            break;
        }
        if (cardToRemove != null) {
            AbstractDungeon.player.masterDeck.group.remove(cardToRemove);
        }
    }

    @Override
    public void onUseCard(AbstractCard card, UseCardAction action) {
        if (card.type == AbstractCard.CardType.ATTACK && (card.costForTurn >= 2 && !card.freeToPlayOnce || card.cost == -1 && card.energyOnUse >= 2) && this.activated) {
            this.activated = false;
            this.flash();
            AbstractMonster m = null;
            if (action.target != null) {
                m = (AbstractMonster)action.target;
            }
            this.addToTop(new RelicAboveCreatureAction(AbstractDungeon.player, this));
            AbstractCard tmp = card.makeSameInstanceOf();
            tmp.current_x = card.current_x;
            tmp.current_y = card.current_y;
            tmp.target_x = (float)Settings.WIDTH / 2.0f - 300.0f * Settings.scale;
            tmp.target_y = (float)Settings.HEIGHT / 2.0f;
            tmp.applyPowers();
            tmp.purgeOnUse = true;
            AbstractDungeon.actionManager.addCardQueueItem(new CardQueueItem(tmp, m, card.energyOnUse, true, true), true);
            this.pulse = false;
        }
    }

    @Override
    public void atTurnStart() {
        this.activated = true;
    }

    @Override
    public boolean checkTrigger() {
        return this.activated;
    }

    @Override
    public AbstractRelic makeCopy() {
        return new Necronomicon();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.HandCheckAction;
import com.megacrit.cardcrawl.actions.utility.ShowCardAction;
import com.megacrit.cardcrawl.actions.utility.ShowCardAndPoofAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;

public class UseCardAction
extends AbstractGameAction {
    private AbstractCard targetCard;
    public AbstractCreature target = null;
    public boolean exhaustCard;
    public boolean returnToHand;
    public boolean reboundCard = false;
    private static final float DUR = 0.15f;

    public UseCardAction(AbstractCard card, AbstractCreature target) {
        this.targetCard = card;
        this.target = target;
        if (card.exhaustOnUseOnce || card.exhaust) {
            this.exhaustCard = true;
        }
        this.setValues(AbstractDungeon.player, null, 1);
        this.duration = 0.15f;
        for (AbstractPower p : AbstractDungeon.player.powers) {
            if (card.dontTriggerOnUseCard) continue;
            p.onUseCard(card, this);
        }
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            if (card.dontTriggerOnUseCard) continue;
            r.onUseCard(card, this);
        }
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            if (card.dontTriggerOnUseCard) continue;
            c.triggerOnCardPlayed(card);
        }
        for (AbstractCard c : AbstractDungeon.player.discardPile.group) {
            if (card.dontTriggerOnUseCard) continue;
            c.triggerOnCardPlayed(card);
        }
        for (AbstractCard c : AbstractDungeon.player.drawPile.group) {
            if (card.dontTriggerOnUseCard) continue;
            c.triggerOnCardPlayed(card);
        }
        for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
            for (AbstractPower p : m.powers) {
                if (card.dontTriggerOnUseCard) continue;
                p.onUseCard(card, this);
            }
        }
        this.actionType = this.exhaustCard ? AbstractGameAction.ActionType.EXHAUST : AbstractGameAction.ActionType.USE;
    }

    public UseCardAction(AbstractCard targetCard) {
        this(targetCard, null);
    }

    @Override
    public void update() {
        if (this.duration == 0.15f) {
            for (AbstractPower p : AbstractDungeon.player.powers) {
                if (this.targetCard.dontTriggerOnUseCard) continue;
                p.onAfterUseCard(this.targetCard, this);
            }
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                for (AbstractPower p : m.powers) {
                    if (this.targetCard.dontTriggerOnUseCard) continue;
                    p.onAfterUseCard(this.targetCard, this);
                }
            }
            this.targetCard.freeToPlayOnce = false;
            this.targetCard.isInAutoplay = false;
            if (this.targetCard.purgeOnUse) {
                this.addToTop(new ShowCardAndPoofAction(this.targetCard));
                this.isDone = true;
                AbstractDungeon.player.cardInUse = null;
                return;
            }
            if (this.targetCard.type == AbstractCard.CardType.POWER) {
                this.addToTop(new ShowCardAction(this.targetCard));
                if (Settings.FAST_MODE) {
                    this.addToTop(new WaitAction(0.1f));
                } else {
                    this.addToTop(new WaitAction(0.7f));
                }
                AbstractDungeon.player.hand.empower(this.targetCard);
                this.isDone = true;
                AbstractDungeon.player.hand.applyPowers();
                AbstractDungeon.player.hand.glowCheck();
                AbstractDungeon.player.cardInUse = null;
                return;
            }
            AbstractDungeon.player.cardInUse = null;
            boolean spoonProc = false;
            if (this.exhaustCard && AbstractDungeon.player.hasRelic("Strange Spoon") && this.targetCard.type != AbstractCard.CardType.POWER) {
                spoonProc = AbstractDungeon.cardRandomRng.randomBoolean();
            }
            if (!this.exhaustCard || spoonProc) {
                if (spoonProc) {
                    AbstractDungeon.player.getRelic("Strange Spoon").flash();
                }
                if (this.reboundCard) {
                    AbstractDungeon.player.hand.moveToDeck(this.targetCard, false);
                } else if (this.targetCard.shuffleBackIntoDrawPile) {
                    AbstractDungeon.player.hand.moveToDeck(this.targetCard, true);
                } else if (this.targetCard.returnToHand) {
                    AbstractDungeon.player.hand.moveToHand(this.targetCard);
                    AbstractDungeon.player.onCardDrawOrDiscard();
                } else {
                    AbstractDungeon.player.hand.moveToDiscardPile(this.targetCard);
                }
            } else {
                AbstractDungeon.player.hand.moveToExhaustPile(this.targetCard);
                CardCrawlGame.dungeon.checkForPactAchievement();
            }
            this.targetCard.exhaustOnUseOnce = false;
            this.targetCard.dontTriggerOnUseCard = false;
            this.addToBot(new HandCheckAction());
        }
        this.tickDuration();
    }
}


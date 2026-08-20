/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.unique;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ExhaustSpecificCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class ImmolateAction
extends AbstractGameAction {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ImmolateAction");
    public static final String[] TEXT = ImmolateAction.uiStrings.TEXT;
    public int[] damage;

    public ImmolateAction(AbstractCreature source, int[] amount, DamageInfo.DamageType type) {
        this.setValues(null, source, amount[0]);
        this.damage = amount;
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.damageType = type;
        this.attackEffect = AbstractGameAction.AttackEffect.FIRE;
        this.duration = Settings.ACTION_DUR_FAST;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (AbstractDungeon.player.hand.size() == 0) {
                this.isDone = true;
                return;
            }
            if (AbstractDungeon.player.hand.size() == 1) {
                AbstractCard card = AbstractDungeon.player.hand.getBottomCard();
                if (card.type == AbstractCard.CardType.CURSE || card.type == AbstractCard.CardType.STATUS) {
                    this.dealDamage();
                }
                this.addToTop(new ExhaustSpecificCardAction(card, AbstractDungeon.player.hand));
                this.isDone = true;
                return;
            }
            AbstractDungeon.handCardSelectScreen.open(TEXT[0], 1, false);
            this.tickDuration();
            return;
        }
        if (!AbstractDungeon.handCardSelectScreen.wereCardsRetrieved) {
            for (AbstractCard c : AbstractDungeon.handCardSelectScreen.selectedCards.group) {
                if (c.type == AbstractCard.CardType.CURSE || c.type == AbstractCard.CardType.STATUS) {
                    this.dealDamage();
                }
                this.addToTop(new ExhaustSpecificCardAction(c, AbstractDungeon.handCardSelectScreen.selectedCards));
            }
            AbstractDungeon.handCardSelectScreen.wereCardsRetrieved = true;
        }
        this.tickDuration();
    }

    public void dealDamage() {
        boolean playedMusic = false;
        int temp = AbstractDungeon.getCurrRoom().monsters.monsters.size();
        for (int i = 0; i < temp; ++i) {
            if (AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).isDying || AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).isEscaping) continue;
            if (playedMusic) {
                AbstractDungeon.effectList.add(new FlashAtkImgEffect(AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).hb.cX, AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).hb.cY, this.attackEffect, true));
                continue;
            }
            playedMusic = true;
            AbstractDungeon.effectList.add(new FlashAtkImgEffect(AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).hb.cX, AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).hb.cY, this.attackEffect));
        }
        for (AbstractPower p : AbstractDungeon.player.powers) {
            p.onDamageAllEnemies(this.damage);
        }
        int temp2 = AbstractDungeon.getCurrRoom().monsters.monsters.size();
        for (int i = 0; i < temp2; ++i) {
            if (AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).isDying || AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).isEscaping) continue;
            AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).tint.color = Color.RED.cpy();
            AbstractDungeon.getCurrRoom().monsters.monsters.get((int)i).tint.changeColor(Color.WHITE.cpy());
            AbstractDungeon.getCurrRoom().monsters.monsters.get(i).damage(new DamageInfo(this.source, this.damage[i], this.damageType));
        }
        if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
            AbstractDungeon.actionManager.clearPostCombatActions();
        }
    }
}


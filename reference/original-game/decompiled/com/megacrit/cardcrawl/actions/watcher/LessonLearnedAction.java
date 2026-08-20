/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.watcher;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.utility.WaitAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.UpgradeShineEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardBrieflyEffect;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;
import java.util.ArrayList;

public class LessonLearnedAction
extends AbstractGameAction {
    private DamageInfo info;
    private AbstractCard theCard = null;

    public LessonLearnedAction(AbstractCreature target, DamageInfo info) {
        this.info = info;
        this.setValues(target, info);
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
        this.duration = Settings.ACTION_DUR_MED;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_MED && this.target != null) {
            AbstractDungeon.effectList.add(new FlashAtkImgEffect(this.target.hb.cX, this.target.hb.cY, AbstractGameAction.AttackEffect.NONE));
            this.target.damage(this.info);
            if (!(!((AbstractMonster)this.target).isDying && this.target.currentHealth > 0 || this.target.halfDead || this.target.hasPower("Minion"))) {
                ArrayList<AbstractCard> possibleCards = new ArrayList<AbstractCard>();
                for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
                    if (!c.canUpgrade()) continue;
                    possibleCards.add(c);
                }
                if (!possibleCards.isEmpty()) {
                    this.theCard = (AbstractCard)possibleCards.get(AbstractDungeon.miscRng.random(0, possibleCards.size() - 1));
                    this.theCard.upgrade();
                    AbstractDungeon.player.bottledCardUpgradeCheck(this.theCard);
                }
            }
            if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
                AbstractDungeon.actionManager.clearPostCombatActions();
            }
        }
        this.tickDuration();
        if (this.isDone && this.theCard != null) {
            AbstractDungeon.effectsQueue.add(new UpgradeShineEffect((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
            AbstractDungeon.topLevelEffectsQueue.add(new ShowCardBrieflyEffect(this.theCard.makeStatEquivalentCopy()));
            this.addToTop(new WaitAction(Settings.ACTION_DUR_MED));
        }
    }
}


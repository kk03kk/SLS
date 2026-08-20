/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.deprecated;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DEPRECATEDDamagePerCardAction
extends AbstractGameAction {
    private static final Logger logger = LogManager.getLogger(DEPRECATEDDamagePerCardAction.class.getName());
    private DamageInfo info;
    private String cardName;

    public DEPRECATEDDamagePerCardAction(AbstractCreature target, DamageInfo info, String cardName, AbstractGameAction.AttackEffect effect) {
        this.info = info;
        this.cardName = cardName;
        this.attackEffect = effect;
        this.setValues(target, info);
        this.actionType = AbstractGameAction.ActionType.DAMAGE;
    }

    @Override
    public void update() {
        if (!this.isDone) {
            this.isDone = true;
            for (AbstractCard c : AbstractDungeon.player.hand.group) {
                if (!c.originalName.equals(this.cardName)) continue;
                logger.info("QUEUED DAMAGE...");
                AbstractDungeon.actionManager.addToTop(new DamageAction(this.target, this.info, this.attackEffect));
            }
        }
    }
}


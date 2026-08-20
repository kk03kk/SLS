/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.utility;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ResetFlagsAction
extends AbstractGameAction {
    private static final Logger logger = LogManager.getLogger(ResetFlagsAction.class.getName());
    private AbstractCard card;

    public ResetFlagsAction(AbstractCard card) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.card = card;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            logger.info("Resetting flags");
            this.card = this.card.makeStatEquivalentCopy();
            this.isDone = true;
        }
    }
}


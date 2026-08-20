/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.actions.defect;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;

public class ChannelAction
extends AbstractGameAction {
    private AbstractOrb orbType;
    private boolean autoEvoke = false;

    public ChannelAction(AbstractOrb newOrbType) {
        this(newOrbType, true);
    }

    public ChannelAction(AbstractOrb newOrbType, boolean autoEvoke) {
        this.duration = Settings.ACTION_DUR_FAST;
        this.orbType = newOrbType;
        this.autoEvoke = autoEvoke;
    }

    @Override
    public void update() {
        if (this.duration == Settings.ACTION_DUR_FAST) {
            if (this.autoEvoke) {
                AbstractDungeon.player.channelOrb(this.orbType);
            } else {
                for (AbstractOrb o : AbstractDungeon.player.orbs) {
                    if (!(o instanceof EmptyOrbSlot)) continue;
                    AbstractDungeon.player.channelOrb(this.orbType);
                    break;
                }
            }
            if (Settings.FAST_MODE) {
                this.isDone = true;
                return;
            }
        }
        this.tickDuration();
    }
}


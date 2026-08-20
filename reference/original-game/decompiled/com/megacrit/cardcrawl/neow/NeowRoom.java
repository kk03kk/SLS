/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.neow;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class NeowRoom
extends AbstractRoom {
    public NeowRoom(boolean isDone) {
        this.phase = AbstractRoom.RoomPhase.EVENT;
        this.event = new NeowEvent(isDone);
        this.event.onEnterRoom();
    }

    @Override
    public void onPlayerEntry() {
        AbstractDungeon.overlayMenu.proceedButton.hide();
    }

    @Override
    public void update() {
        super.update();
        if (!AbstractDungeon.isScreenUp) {
            this.event.update();
        }
        if (this.event.waitTimer == 0.0f && !this.event.hasFocus && this.phase != AbstractRoom.RoomPhase.COMBAT) {
            this.phase = AbstractRoom.RoomPhase.COMPLETE;
            this.event.reopen();
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        super.render(sb);
        this.event.render(sb);
    }

    @Override
    public void renderAboveTopPanel(SpriteBatch sb) {
        super.renderAboveTopPanel(sb);
        if (this.event != null) {
            this.event.renderAboveTopPanel(sb);
        }
    }
}


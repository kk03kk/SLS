/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rooms;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.random.Random;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class EventRoom
extends AbstractRoom {
    public EventRoom() {
        this.phase = AbstractRoom.RoomPhase.EVENT;
        this.mapSymbol = "?";
        this.mapImg = ImageMaster.MAP_NODE_EVENT;
        this.mapImgOutline = ImageMaster.MAP_NODE_EVENT_OUTLINE;
    }

    @Override
    public void onPlayerEntry() {
        AbstractDungeon.overlayMenu.proceedButton.hide();
        Random eventRngDuplicate = new Random(Settings.seed, AbstractDungeon.eventRng.counter);
        this.event = AbstractDungeon.generateEvent(eventRngDuplicate);
        this.event.onEnterRoom();
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
        if (this.event != null) {
            this.event.render(sb);
        }
        super.render(sb);
    }

    @Override
    public void renderAboveTopPanel(SpriteBatch sb) {
        super.renderAboveTopPanel(sb);
        if (this.event != null) {
            this.event.renderAboveTopPanel(sb);
        }
    }
}


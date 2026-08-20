/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rooms;

import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class EmptyRoom
extends AbstractRoom {
    public EmptyRoom() {
        this.phase = AbstractRoom.RoomPhase.COMPLETE;
    }

    @Override
    public void onPlayerEntry() {
    }
}


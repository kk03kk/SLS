/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rooms;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.cutscenes.Cutscene;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;

public class TrueVictoryRoom
extends AbstractRoom {
    public Cutscene cutscene;

    public TrueVictoryRoom() {
        this.phase = AbstractRoom.RoomPhase.INCOMPLETE;
        this.cutscene = new Cutscene(AbstractDungeon.player.chosenClass);
        AbstractDungeon.overlayMenu.proceedButton.hideInstantly();
    }

    @Override
    public void onPlayerEntry() {
        AbstractDungeon.isScreenUp = true;
        AbstractDungeon.overlayMenu.proceedButton.hide();
        GameCursor.hidden = true;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NO_INTERACT;
    }

    @Override
    public void update() {
        super.update();
        this.cutscene.update();
    }

    @Override
    public void render(SpriteBatch sb) {
        super.render(sb);
        this.cutscene.render(sb);
    }

    @Override
    public void renderAboveTopPanel(SpriteBatch sb) {
        super.renderAboveTopPanel(sb);
        this.cutscene.renderAbove(sb);
    }

    @Override
    public void dispose() {
        super.dispose();
        this.cutscene.dispose();
    }
}


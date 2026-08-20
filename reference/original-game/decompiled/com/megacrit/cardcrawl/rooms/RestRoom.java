/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rooms;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.CampfireUI;

public class RestRoom
extends AbstractRoom {
    public long fireSoundId;
    public static long lastFireSoundId = 0L;
    public CampfireUI campfireUI;

    public RestRoom() {
        this.phase = AbstractRoom.RoomPhase.INCOMPLETE;
        this.mapSymbol = "R";
        this.mapImg = ImageMaster.MAP_NODE_REST;
        this.mapImgOutline = ImageMaster.MAP_NODE_REST_OUTLINE;
    }

    @Override
    public void onPlayerEntry() {
        if (!AbstractDungeon.id.equals("TheEnding")) {
            CardCrawlGame.music.silenceBGM();
        }
        lastFireSoundId = this.fireSoundId = CardCrawlGame.sound.playAndLoop("REST_FIRE_WET");
        this.campfireUI = new CampfireUI();
        for (AbstractRelic r : AbstractDungeon.player.relics) {
            r.onEnterRestRoom();
        }
    }

    @Override
    public AbstractCard.CardRarity getCardRarity(int roll) {
        return this.getCardRarity(roll, false);
    }

    @Override
    public void update() {
        super.update();
        if (this.campfireUI != null) {
            this.campfireUI.update();
        }
    }

    public void fadeIn() {
        if (!AbstractDungeon.id.equals("TheEnding")) {
            CardCrawlGame.music.unsilenceBGM();
        }
    }

    public void cutFireSound() {
        CardCrawlGame.sound.fadeOut("REST_FIRE_WET", ((RestRoom)AbstractDungeon.getCurrRoom()).fireSoundId);
    }

    public void updateAmbience() {
        CardCrawlGame.sound.adjustVolume("REST_FIRE_WET", this.fireSoundId);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.campfireUI != null) {
            this.campfireUI.render(sb);
        }
        super.render(sb);
    }
}


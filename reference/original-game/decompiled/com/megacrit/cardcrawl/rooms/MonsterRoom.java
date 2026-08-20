/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rooms;

import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.rooms.MonsterRoomElite;
import com.megacrit.cardcrawl.screens.DiscardPileViewScreen;

public class MonsterRoom
extends AbstractRoom {
    public DiscardPileViewScreen discardPileViewScreen;
    public static final float COMBAT_WAIT_TIME = 0.1f;

    public MonsterRoom() {
        this.phase = AbstractRoom.RoomPhase.COMBAT;
        this.mapSymbol = "M";
        this.mapImg = ImageMaster.MAP_NODE_ENEMY;
        this.mapImgOutline = ImageMaster.MAP_NODE_ENEMY_OUTLINE;
        this.discardPileViewScreen = new DiscardPileViewScreen();
    }

    @Override
    public void dropReward() {
        if (ModHelper.isModEnabled("Vintage") && !(AbstractDungeon.getCurrRoom() instanceof MonsterRoomElite) && !(AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss)) {
            AbstractRelic.RelicTier tier = this.returnRandomRelicTier();
            this.addRelicToRewards(tier);
        }
    }

    private AbstractRelic.RelicTier returnRandomRelicTier() {
        int roll = AbstractDungeon.relicRng.random(0, 99);
        if (roll < 50) {
            return AbstractRelic.RelicTier.COMMON;
        }
        if (roll > 85) {
            return AbstractRelic.RelicTier.RARE;
        }
        return AbstractRelic.RelicTier.UNCOMMON;
    }

    @Override
    public void onPlayerEntry() {
        this.playBGM(null);
        if (this.monsters == null) {
            this.monsters = CardCrawlGame.dungeon.getMonsterForRoomCreation();
            this.monsters.init();
        }
        waitTimer = 0.1f;
    }

    public void setMonster(MonsterGroup m) {
        this.monsters = m;
    }
}


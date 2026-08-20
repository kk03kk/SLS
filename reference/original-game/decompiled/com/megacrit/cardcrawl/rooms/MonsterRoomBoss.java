/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rooms;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.MonsterRoom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MonsterRoomBoss
extends MonsterRoom {
    private static final Logger logger = LogManager.getLogger(MonsterRoomBoss.class.getName());

    public MonsterRoomBoss() {
        this.mapSymbol = "B";
    }

    @Override
    public void onPlayerEntry() {
        this.monsters = CardCrawlGame.dungeon.getBoss();
        logger.info("BOSSES: " + AbstractDungeon.bossList.size());
        CardCrawlGame.metricData.path_taken.add("BOSS");
        CardCrawlGame.music.silenceBGM();
        AbstractDungeon.bossList.remove(0);
        if (this.monsters != null) {
            this.monsters.init();
        }
        waitTimer = 0.1f;
    }

    @Override
    public AbstractCard.CardRarity getCardRarity(int roll) {
        return AbstractCard.CardRarity.RARE;
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rooms;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.rewards.chests.AbstractChest;
import com.megacrit.cardcrawl.rewards.chests.BossChest;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.scene.SpookierChestEffect;

public class TreasureRoomBoss
extends AbstractRoom {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("TreasureRoomBoss");
    public static final String[] TEXT = TreasureRoomBoss.uiStrings.TEXT;
    public AbstractChest chest;
    private float shinyTimer = 0.0f;
    private static final float SHINY_INTERVAL = 0.02f;
    public boolean choseRelic = false;

    public TreasureRoomBoss() {
        CardCrawlGame.nextDungeon = this.getNextDungeonName();
        this.phase = AbstractDungeon.actNum < 4 || !AbstractPlayer.customMods.contains("Blight Chests") ? AbstractRoom.RoomPhase.COMPLETE : AbstractRoom.RoomPhase.INCOMPLETE;
        this.mapImg = ImageMaster.MAP_NODE_TREASURE;
        this.mapImgOutline = ImageMaster.MAP_NODE_TREASURE_OUTLINE;
    }

    private String getNextDungeonName() {
        switch (AbstractDungeon.id) {
            case "Exordium": {
                return "TheCity";
            }
            case "TheCity": {
                return "TheBeyond";
            }
            case "TheBeyond": {
                if (Settings.isEndless) {
                    return "Exordium";
                }
                return null;
            }
        }
        return null;
    }

    @Override
    public void onPlayerEntry() {
        CardCrawlGame.music.silenceBGM();
        if (AbstractDungeon.actNum < 4 || !AbstractPlayer.customMods.contains("Blight Chests")) {
            AbstractDungeon.overlayMenu.proceedButton.setLabel(TEXT[0]);
        }
        this.playBGM("SHRINE");
        this.chest = new BossChest();
    }

    @Override
    public void update() {
        super.update();
        this.chest.update();
        this.updateShiny();
    }

    private void updateShiny() {
        if (!this.chest.isOpen) {
            this.shinyTimer -= Gdx.graphics.getDeltaTime();
            if (this.shinyTimer < 0.0f && !Settings.DISABLE_EFFECTS) {
                this.shinyTimer = 0.02f;
                AbstractDungeon.effectList.add(new SpookierChestEffect());
                AbstractDungeon.effectList.add(new SpookierChestEffect());
            }
        }
    }

    @Override
    public void renderAboveTopPanel(SpriteBatch sb) {
        super.renderAboveTopPanel(sb);
    }

    @Override
    public void render(SpriteBatch sb) {
        this.chest.render(sb);
        super.render(sb);
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rooms;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.rewards.chests.AbstractChest;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.ChestShineEffect;
import com.megacrit.cardcrawl.vfx.scene.SpookyChestEffect;

public class TreasureRoom
extends AbstractRoom {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("TreasureRoom");
    public static final String[] TEXT = TreasureRoom.uiStrings.TEXT;
    public AbstractChest chest;
    private float shinyTimer = 0.0f;
    private static final float SHINY_INTERVAL = 0.2f;

    public TreasureRoom() {
        this.phase = AbstractRoom.RoomPhase.COMPLETE;
        this.mapSymbol = "T";
        this.mapImg = ImageMaster.MAP_NODE_TREASURE;
        this.mapImgOutline = ImageMaster.MAP_NODE_TREASURE_OUTLINE;
    }

    @Override
    public void onPlayerEntry() {
        this.playBGM(null);
        this.chest = AbstractDungeon.getRandomChest();
        AbstractDungeon.overlayMenu.proceedButton.setLabel(TEXT[0]);
    }

    @Override
    public void update() {
        super.update();
        if (this.chest != null) {
            this.chest.update();
        }
        this.updateShiny();
    }

    private void updateShiny() {
        if (!this.chest.isOpen) {
            this.shinyTimer -= Gdx.graphics.getDeltaTime();
            if (this.shinyTimer < 0.0f && !Settings.DISABLE_EFFECTS) {
                this.shinyTimer = 0.2f;
                AbstractDungeon.topLevelEffects.add(new ChestShineEffect());
                AbstractDungeon.effectList.add(new SpookyChestEffect());
                AbstractDungeon.effectList.add(new SpookyChestEffect());
            }
        }
    }

    @Override
    public void renderAboveTopPanel(SpriteBatch sb) {
        super.renderAboveTopPanel(sb);
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.chest != null) {
            this.chest.render(sb);
        }
        super.render(sb);
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rewards.chests;

import com.megacrit.cardcrawl.blights.AbstractBlight;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.BlightHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.Matryoshka;
import com.megacrit.cardcrawl.rewards.chests.AbstractChest;
import java.util.ArrayList;

public class BossChest
extends AbstractChest {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("BossChest");
    public static final String[] TEXT = BossChest.uiStrings.TEXT;
    public ArrayList<AbstractRelic> relics = new ArrayList();
    public ArrayList<AbstractBlight> blights = new ArrayList();

    public BossChest() {
        this.img = ImageMaster.BOSS_CHEST;
        this.openedImg = ImageMaster.BOSS_CHEST_OPEN;
        this.hb = new Hitbox(256.0f * Settings.scale, 200.0f * Settings.scale);
        this.hb.move(CHEST_LOC_X, CHEST_LOC_Y - 100.0f * Settings.scale);
        if (AbstractDungeon.actNum < 4 || !AbstractPlayer.customMods.contains("Blight Chests")) {
            this.relics.clear();
            for (int i = 0; i < 3; ++i) {
                this.relics.add(AbstractDungeon.returnRandomRelic(AbstractRelic.RelicTier.BOSS));
            }
        } else {
            this.blights.clear();
            this.blights.add(BlightHelper.getRandomBlight());
            ArrayList<String> exclusion = new ArrayList<String>();
            exclusion.add(this.blights.get((int)0).blightID);
            this.blights.add(BlightHelper.getRandomChestBlight(exclusion));
        }
    }

    @Override
    public void open(boolean bossChest) {
        if (AbstractDungeon.actNum < 4 || !AbstractPlayer.customMods.contains("Blight Chests")) {
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                if (r instanceof Matryoshka) continue;
                r.onChestOpen(true);
            }
            AbstractDungeon.overlayMenu.proceedButton.setLabel(TEXT[0]);
            CardCrawlGame.sound.play("CHEST_OPEN");
            AbstractDungeon.bossRelicScreen.open(this.relics);
        } else {
            CardCrawlGame.sound.play("CHEST_OPEN");
            AbstractDungeon.bossRelicScreen.openBlight(this.blights);
        }
    }

    @Override
    public void close() {
        CardCrawlGame.sound.play("CHEST_OPEN");
        this.isOpen = false;
    }
}


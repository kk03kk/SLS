/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rooms;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.shop.Merchant;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.vfx.cardManip.PurgeCardEffect;

public class ShopRoom
extends AbstractRoom {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("ShopRoom");
    public static final String[] TEXT = ShopRoom.uiStrings.TEXT;
    public int shopRarityBonus = 6;
    public Merchant merchant;

    public ShopRoom() {
        this.phase = AbstractRoom.RoomPhase.COMPLETE;
        this.merchant = null;
        this.mapSymbol = "$";
        this.mapImg = ImageMaster.MAP_NODE_MERCHANT;
        this.mapImgOutline = ImageMaster.MAP_NODE_MERCHANT_OUTLINE;
        this.baseRareCardChance = 9;
        this.baseUncommonCardChance = 37;
    }

    public void setMerchant(Merchant merc) {
        this.merchant = merc;
    }

    @Override
    public void onPlayerEntry() {
        if (!AbstractDungeon.id.equals("TheEnding")) {
            this.playBGM("SHOP");
        }
        AbstractDungeon.overlayMenu.proceedButton.setLabel(TEXT[0]);
        this.setMerchant(new Merchant());
    }

    @Override
    public AbstractCard.CardRarity getCardRarity(int roll) {
        return this.getCardRarity(roll, false);
    }

    @Override
    public void update() {
        super.update();
        if (this.merchant != null) {
            this.merchant.update();
        }
        this.updatePurge();
    }

    private void updatePurge() {
        if (!AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) {
            ShopScreen.purgeCard();
            for (AbstractCard card : AbstractDungeon.gridSelectScreen.selectedCards) {
                CardCrawlGame.metricData.addPurgedItem(card.getMetricID());
                AbstractDungeon.topLevelEffects.add(new PurgeCardEffect(card, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                AbstractDungeon.player.masterDeck.removeCard(card);
            }
            AbstractDungeon.gridSelectScreen.selectedCards.clear();
            AbstractDungeon.shopScreen.purgeAvailable = false;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (this.merchant != null) {
            this.merchant.render(sb);
        }
        super.render(sb);
        this.renderTips(sb);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (this.merchant != null) {
            this.merchant.dispose();
            this.merchant = null;
        }
    }
}


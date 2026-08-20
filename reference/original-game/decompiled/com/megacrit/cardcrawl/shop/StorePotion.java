/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.shop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.shop.ShopScreen;

public class StorePotion {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("StorePotion");
    public static final String[] TEXT = StorePotion.uiStrings.TEXT;
    public AbstractPotion potion;
    private ShopScreen shopScreen;
    public int price;
    private int slot;
    public boolean isPurchased = false;
    private static final float RELIC_GOLD_OFFSET_X = -56.0f * Settings.scale;
    private static final float RELIC_GOLD_OFFSET_Y = -100.0f * Settings.scale;
    private static final float RELIC_PRICE_OFFSET_X = 14.0f * Settings.scale;
    private static final float RELIC_PRICE_OFFSET_Y = -62.0f * Settings.scale;
    private static final float GOLD_IMG_WIDTH = (float)ImageMaster.UI_GOLD.getWidth() * Settings.scale;

    public StorePotion(AbstractPotion potion, int slot, ShopScreen screenRef) {
        this.potion = potion;
        this.price = potion.getPrice();
        this.slot = slot;
        this.shopScreen = screenRef;
    }

    public void update(float rugY) {
        if (this.potion != null) {
            if (!this.isPurchased) {
                this.potion.posX = 1000.0f * Settings.xScale + 150.0f * (float)this.slot * Settings.xScale;
                this.potion.posY = rugY + 200.0f * Settings.yScale;
                this.potion.hb.move(this.potion.posX, this.potion.posY);
                this.potion.hb.update();
                if (this.potion.hb.hovered) {
                    this.shopScreen.moveHand(this.potion.posX - 190.0f * Settings.scale, this.potion.posY - 70.0f * Settings.scale);
                    if (InputHelper.justClickedLeft) {
                        this.potion.hb.clickStarted = true;
                    }
                }
            }
            if (this.potion.hb.clicked || this.potion.hb.hovered && CInputActionSet.select.isJustPressed()) {
                this.potion.hb.clicked = false;
                if (!Settings.isTouchScreen) {
                    this.purchasePotion();
                } else if (AbstractDungeon.shopScreen.touchPotion == null) {
                    if (AbstractDungeon.player.gold < this.price) {
                        this.shopScreen.playCantBuySfx();
                        this.shopScreen.createSpeech(ShopScreen.getCantBuyMsg());
                    } else {
                        AbstractDungeon.shopScreen.confirmButton.hideInstantly();
                        AbstractDungeon.shopScreen.confirmButton.show();
                        AbstractDungeon.shopScreen.confirmButton.isDisabled = false;
                        AbstractDungeon.shopScreen.confirmButton.hb.clickStarted = false;
                        AbstractDungeon.shopScreen.touchPotion = this;
                    }
                }
            }
        }
    }

    public void purchasePotion() {
        if (AbstractDungeon.player.hasRelic("Sozu")) {
            AbstractDungeon.player.getRelic("Sozu").flash();
            return;
        }
        if (AbstractDungeon.player.gold >= this.price) {
            if (AbstractDungeon.player.obtainPotion(this.potion)) {
                AbstractDungeon.player.loseGold(this.price);
                CardCrawlGame.sound.play("SHOP_PURCHASE", 0.1f);
                CardCrawlGame.metricData.addShopPurchaseData(this.potion.ID);
                this.shopScreen.playBuySfx();
                this.shopScreen.createSpeech(ShopScreen.getBuyMsg());
                if (AbstractDungeon.player.hasRelic("The Courier")) {
                    this.potion = AbstractDungeon.returnRandomPotion();
                    this.price = this.potion.getPrice();
                    this.shopScreen.getNewPrice(this);
                } else {
                    this.isPurchased = true;
                }
                return;
            }
            this.shopScreen.createSpeech(TEXT[0]);
            AbstractDungeon.topPanel.flashRed();
        } else {
            this.shopScreen.playCantBuySfx();
            this.shopScreen.createSpeech(ShopScreen.getCantBuyMsg());
        }
    }

    public void hide() {
        if (this.potion != null) {
            this.potion.posY = (float)Settings.HEIGHT + 200.0f * Settings.scale;
        }
    }

    public void render(SpriteBatch sb) {
        if (this.potion != null) {
            this.potion.shopRender(sb);
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.UI_GOLD, this.potion.posX + RELIC_GOLD_OFFSET_X, this.potion.posY + RELIC_GOLD_OFFSET_Y, GOLD_IMG_WIDTH, GOLD_IMG_WIDTH);
            Color color = Color.WHITE;
            if (this.price > AbstractDungeon.player.gold) {
                color = Color.SALMON;
            }
            FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipHeaderFont, Integer.toString(this.price), this.potion.posX + RELIC_PRICE_OFFSET_X, this.potion.posY + RELIC_PRICE_OFFSET_Y, color);
        }
    }
}


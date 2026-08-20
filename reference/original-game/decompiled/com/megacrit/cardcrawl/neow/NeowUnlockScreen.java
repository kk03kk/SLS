/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.neow;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.ui.buttons.UnlockConfirmButton;
import com.megacrit.cardcrawl.unlock.AbstractUnlock;
import com.megacrit.cardcrawl.unlock.UnlockTracker;
import com.megacrit.cardcrawl.vfx.ConeEffect;
import com.megacrit.cardcrawl.vfx.RoomShineEffect;
import com.megacrit.cardcrawl.vfx.RoomShineEffect2;
import java.util.ArrayList;
import java.util.Iterator;

public class NeowUnlockScreen {
    public ArrayList<AbstractUnlock> unlockBundle;
    private ArrayList<ConeEffect> cones = new ArrayList();
    private static final int CONE_AMT = 30;
    private float shinyTimer = 0.0f;
    private static final float SHINY_INTERVAL = 0.2f;
    public UnlockConfirmButton button = new UnlockConfirmButton();
    public long id;
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("UnlockScreen");
    public static final String[] TEXT = NeowUnlockScreen.uiStrings.TEXT;

    public void open(ArrayList<AbstractUnlock> unlock, boolean isVictory) {
        int i;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NEOW_UNLOCK;
        this.unlockBundle = unlock;
        this.button.show();
        this.id = CardCrawlGame.sound.play("UNLOCK_SCREEN");
        this.cones.clear();
        for (i = 0; i < 30; ++i) {
            this.cones.add(new ConeEffect());
        }
        switch (this.unlockBundle.get((int)0).type) {
            case CARD: {
                for (i = 0; i < this.unlockBundle.size(); ++i) {
                    UnlockTracker.unlockCard(this.unlockBundle.get((int)i).card.cardID);
                    AbstractDungeon.dynamicBanner.appearInstantly(TEXT[0]);
                    this.unlockBundle.get((int)i).card.targetDrawScale = 1.0f;
                    this.unlockBundle.get((int)i).card.drawScale = 0.01f;
                    this.unlockBundle.get((int)i).card.current_x = (float)Settings.WIDTH * (0.25f * (float)(i + 1));
                    this.unlockBundle.get((int)i).card.current_y = (float)Settings.HEIGHT / 2.0f;
                    this.unlockBundle.get((int)i).card.target_x = (float)Settings.WIDTH * (0.25f * (float)(i + 1));
                    this.unlockBundle.get((int)i).card.target_y = (float)Settings.HEIGHT / 2.0f - 30.0f * Settings.scale;
                }
                break;
            }
            case RELIC: {
                for (i = 0; i < this.unlockBundle.size(); ++i) {
                    UnlockTracker.hardUnlockOverride(this.unlockBundle.get((int)i).relic.relicId);
                    UnlockTracker.markRelicAsSeen(this.unlockBundle.get((int)i).relic.relicId);
                    this.unlockBundle.get((int)i).relic.loadLargeImg();
                    AbstractDungeon.dynamicBanner.appearInstantly(TEXT[1]);
                    this.unlockBundle.get((int)i).relic.currentX = (float)Settings.WIDTH * (0.25f * (float)(i + 1));
                    this.unlockBundle.get((int)i).relic.currentY = (float)Settings.HEIGHT / 2.0f;
                    this.unlockBundle.get((int)i).relic.hb.move(this.unlockBundle.get((int)i).relic.currentX, this.unlockBundle.get((int)i).relic.currentY);
                }
                break;
            }
            case CHARACTER: {
                this.unlockBundle.get(0).onUnlockScreenOpen();
                AbstractDungeon.dynamicBanner.appearInstantly(TEXT[2]);
                break;
            }
            case MISC: {
                break;
            }
        }
    }

    public void reOpen() {
        int i;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.NEOW_UNLOCK;
        this.button.show();
        this.id = CardCrawlGame.sound.play("UNLOCK_SCREEN");
        this.cones.clear();
        for (i = 0; i < 30; ++i) {
            this.cones.add(new ConeEffect());
        }
        switch (this.unlockBundle.get((int)0).type) {
            case CARD: {
                for (i = 0; i < this.unlockBundle.size(); ++i) {
                    UnlockTracker.unlockCard(this.unlockBundle.get((int)i).card.cardID);
                    AbstractDungeon.dynamicBanner.appearInstantly(TEXT[0]);
                    this.unlockBundle.get((int)i).card.targetDrawScale = 1.0f;
                    this.unlockBundle.get((int)i).card.drawScale = 0.01f;
                    this.unlockBundle.get((int)i).card.current_x = (float)Settings.WIDTH * (0.25f * (float)(i + 1));
                    this.unlockBundle.get((int)i).card.current_y = (float)Settings.HEIGHT / 2.0f;
                    this.unlockBundle.get((int)i).card.target_x = (float)Settings.WIDTH * (0.25f * (float)(i + 1));
                    this.unlockBundle.get((int)i).card.target_y = (float)Settings.HEIGHT / 2.0f - 30.0f * Settings.scale;
                }
                break;
            }
            case RELIC: {
                for (i = 0; i < this.unlockBundle.size(); ++i) {
                    AbstractDungeon.dynamicBanner.appearInstantly(TEXT[1]);
                    this.unlockBundle.get((int)i).relic.currentX = (float)Settings.WIDTH * (0.25f * (float)(i + 1));
                    this.unlockBundle.get((int)i).relic.currentY = (float)Settings.HEIGHT / 2.0f;
                    this.unlockBundle.get((int)i).relic.hb.move(this.unlockBundle.get((int)i).relic.currentX, this.unlockBundle.get((int)i).relic.currentY);
                }
                break;
            }
            case CHARACTER: {
                this.unlockBundle.get(0).onUnlockScreenOpen();
                AbstractDungeon.dynamicBanner.appearInstantly(TEXT[2]);
                break;
            }
            case MISC: {
                break;
            }
        }
    }

    public void update() {
        this.shinyTimer -= Gdx.graphics.getDeltaTime();
        if (this.shinyTimer < 0.0f) {
            this.shinyTimer = 0.2f;
            AbstractDungeon.topLevelEffects.add(new RoomShineEffect());
            AbstractDungeon.topLevelEffects.add(new RoomShineEffect());
            AbstractDungeon.topLevelEffects.add(new RoomShineEffect2());
        }
        switch (this.unlockBundle.get((int)0).type) {
            case CARD: {
                this.updateConeEffect();
                for (int i = 0; i < this.unlockBundle.size(); ++i) {
                    this.unlockBundle.get((int)i).card.update();
                    this.unlockBundle.get((int)i).card.updateHoverLogic();
                    this.unlockBundle.get((int)i).card.targetDrawScale = 1.0f;
                }
                break;
            }
            case RELIC: {
                this.updateConeEffect();
                for (int i = 0; i < this.unlockBundle.size(); ++i) {
                    this.unlockBundle.get((int)i).relic.update();
                }
                break;
            }
            case CHARACTER: {
                this.updateConeEffect();
                this.unlockBundle.get((int)0).player.update();
                break;
            }
        }
        this.button.update();
    }

    private void updateConeEffect() {
        Iterator<ConeEffect> e = this.cones.iterator();
        while (e.hasNext()) {
            ConeEffect d = e.next();
            d.update();
            if (!d.isDone) continue;
            e.remove();
        }
        if (this.cones.size() < 30) {
            this.cones.add(new ConeEffect());
        }
    }

    public void render(SpriteBatch sb) {
        sb.setColor(new Color(0.05f, 0.15f, 0.18f, 1.0f));
        sb.draw(ImageMaster.WHITE_SQUARE_IMG, 0.0f, 0.0f, (float)Settings.WIDTH, (float)Settings.HEIGHT);
        sb.setBlendFunction(770, 1);
        for (ConeEffect e : this.cones) {
            e.render(sb);
        }
        sb.setBlendFunction(770, 771);
        switch (this.unlockBundle.get((int)0).type) {
            case CARD: {
                for (int i = 0; i < this.unlockBundle.size(); ++i) {
                    this.unlockBundle.get((int)i).card.renderHoverShadow(sb);
                    this.unlockBundle.get((int)i).card.render(sb);
                    this.unlockBundle.get((int)i).card.renderCardTip(sb);
                }
                sb.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
                sb.draw(ImageMaster.UNLOCK_TEXT_BG, (float)Settings.WIDTH / 2.0f - 500.0f, (float)Settings.HEIGHT / 2.0f - 330.0f * Settings.scale - 130.0f, 500.0f, 130.0f, 1000.0f, 260.0f, Settings.scale * 1.2f, Settings.scale * 0.8f, 0.0f, 0, 0, 1000, 260, false, false);
                FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, TEXT[3], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f - 330.0f * Settings.scale, Settings.CREAM_COLOR);
                break;
            }
            case CHARACTER: {
                this.unlockBundle.get(0).render(sb);
                this.unlockBundle.get((int)0).player.renderPlayerImage(sb);
                break;
            }
            case RELIC: {
                for (int i = 0; i < this.unlockBundle.size(); ++i) {
                    if (RelicLibrary.redList.contains(this.unlockBundle.get((int)i).relic)) {
                        this.unlockBundle.get((int)i).relic.render(sb, false, Settings.RED_RELIC_COLOR);
                    } else if (RelicLibrary.greenList.contains(this.unlockBundle.get((int)i).relic)) {
                        this.unlockBundle.get((int)i).relic.render(sb, false, Settings.GREEN_RELIC_COLOR);
                    } else if (RelicLibrary.blueList.contains(this.unlockBundle.get((int)i).relic)) {
                        this.unlockBundle.get((int)i).relic.render(sb, false, Settings.BLUE_RELIC_COLOR);
                    } else if (RelicLibrary.whiteList.contains(this.unlockBundle.get((int)i).relic)) {
                        this.unlockBundle.get((int)i).relic.render(sb, false, Settings.PURPLE_RELIC_COLOR);
                    } else {
                        this.unlockBundle.get((int)i).relic.render(sb, false, Settings.TWO_THIRDS_TRANSPARENT_BLACK_COLOR);
                    }
                    sb.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
                    sb.draw(ImageMaster.UNLOCK_TEXT_BG, (float)Settings.WIDTH / 2.0f - 500.0f, (float)Settings.HEIGHT / 2.0f - 330.0f * Settings.scale - 130.0f, 500.0f, 130.0f, 1000.0f, 260.0f, Settings.scale * 1.2f, Settings.scale * 0.8f, 0.0f, 0, 0, 1000, 260, false, false);
                    FontHelper.renderFontCentered(sb, FontHelper.losePowerFont, this.unlockBundle.get((int)i).relic.name, (float)Settings.WIDTH * (0.25f * (float)(i + 1)), (float)Settings.HEIGHT / 2.0f - 150.0f * Settings.scale, Settings.GOLD_COLOR, 1.2f);
                }
                FontHelper.renderFontCentered(sb, FontHelper.panelNameFont, TEXT[3], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f - 330.0f * Settings.scale, Settings.CREAM_COLOR);
                break;
            }
        }
        this.button.render(sb);
    }
}


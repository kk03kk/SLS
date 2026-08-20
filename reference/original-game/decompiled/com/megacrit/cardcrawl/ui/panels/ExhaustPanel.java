/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.ui.panels.AbstractPanel;
import com.megacrit.cardcrawl.vfx.ExhaustPileParticle;

public class ExhaustPanel
extends AbstractPanel {
    private static final TutorialStrings tutorialStrings = CardCrawlGame.languagePack.getTutorialString("Exhaust Tip");
    public static final String[] MSG = ExhaustPanel.tutorialStrings.TEXT;
    public static final String[] LABEL = ExhaustPanel.tutorialStrings.LABEL;
    public static float fontScale = 1.0f;
    public static final float FONT_POP_SCALE = 2.0f;
    private static final float COUNT_CIRCLE_W = 128.0f * Settings.scale;
    public static int totalCount = 0;
    private Hitbox hb = new Hitbox(0.0f, 0.0f, 100.0f * Settings.scale, 100.0f * Settings.scale);
    public static float energyVfxTimer = 0.0f;
    public static final float ENERGY_VFX_TIME = 2.0f;

    public ExhaustPanel() {
        super((float)Settings.WIDTH - 70.0f * Settings.scale, 184.0f * Settings.scale, (float)Settings.WIDTH + 100.0f * Settings.scale, 184.0f * Settings.scale, 0.0f, 0.0f, null, false);
    }

    @Override
    public void updatePositions() {
        super.updatePositions();
        if (!this.isHidden && AbstractDungeon.player.exhaustPile.size() > 0) {
            this.hb.update();
            this.updateVfx();
        }
        if (this.hb.hovered && (!AbstractDungeon.isScreenUp || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.EXHAUST_VIEW || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.HAND_SELECT || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD && AbstractDungeon.overlayMenu.combatPanelsShown)) {
            AbstractDungeon.overlayMenu.hoveredTip = true;
            if (InputHelper.justClickedLeft) {
                this.hb.clickStarted = true;
            }
        }
        if ((this.hb.clicked || InputActionSet.exhaustPile.isJustPressed() || CInputActionSet.pageRightViewExhaust.isJustPressed()) && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.EXHAUST_VIEW) {
            this.hb.clicked = false;
            this.hb.hovered = false;
            CardCrawlGame.sound.play("DECK_CLOSE");
            AbstractDungeon.closeCurrentScreen();
            return;
        }
        if ((this.hb.clicked || InputActionSet.exhaustPile.isJustPressed() || CInputActionSet.pageRightViewExhaust.isJustPressed()) && AbstractDungeon.overlayMenu.combatPanelsShown && AbstractDungeon.getMonsters() != null && !AbstractDungeon.getMonsters().areMonstersDead() && !AbstractDungeon.player.isDead && !AbstractDungeon.player.exhaustPile.isEmpty()) {
            this.hb.clicked = false;
            this.hb.hovered = false;
            if (AbstractDungeon.isScreenUp) {
                if (AbstractDungeon.previousScreen == null) {
                    AbstractDungeon.previousScreen = AbstractDungeon.screen;
                }
            } else {
                AbstractDungeon.previousScreen = null;
            }
            this.openExhaustPile();
        }
    }

    private void openExhaustPile() {
        if (AbstractDungeon.player.hoveredCard != null) {
            AbstractDungeon.player.releaseCard();
        }
        AbstractDungeon.dynamicBanner.hide();
        AbstractDungeon.exhaustPileViewScreen.open();
        this.hb.hovered = false;
        InputHelper.justClickedLeft = false;
    }

    private void updateVfx() {
        if ((energyVfxTimer -= Gdx.graphics.getDeltaTime()) < 0.0f && !Settings.hideLowerElements) {
            AbstractDungeon.effectList.add(new ExhaustPileParticle(this.current_x, this.current_y));
            energyVfxTimer = 0.05f;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!AbstractDungeon.player.exhaustPile.isEmpty()) {
            this.hb.move(this.current_x, this.current_y);
            String msg = Integer.toString(AbstractDungeon.player.exhaustPile.size());
            sb.setColor(Settings.TWO_THIRDS_TRANSPARENT_BLACK_COLOR);
            sb.draw(ImageMaster.DECK_COUNT_CIRCLE, this.current_x - COUNT_CIRCLE_W / 2.0f, this.current_y - COUNT_CIRCLE_W / 2.0f, COUNT_CIRCLE_W, COUNT_CIRCLE_W);
            FontHelper.renderFontCentered(sb, FontHelper.turnNumFont, msg, this.current_x, this.current_y + 2.0f * Settings.scale, Settings.PURPLE_COLOR);
            if (Settings.isControllerMode) {
                sb.setColor(Color.WHITE);
                sb.draw(CInputActionSet.pageRightViewExhaust.getKeyImg(), this.current_x - 32.0f + 30.0f * Settings.scale, this.current_y - 32.0f - 30.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 0.75f, Settings.scale * 0.75f, 0.0f, 0, 0, 64, 64, false, false);
            }
            this.hb.render(sb);
            if (this.hb.hovered && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT && !AbstractDungeon.isScreenUp) {
                if (Settings.isConsoleBuild) {
                    TipHelper.renderGenericTip(1550.0f * Settings.scale, 450.0f * Settings.scale, LABEL[0] + " (" + InputActionSet.exhaustPile.getKeyString() + ")", MSG[1]);
                } else {
                    TipHelper.renderGenericTip(1550.0f * Settings.scale, 450.0f * Settings.scale, LABEL[0] + " (" + InputActionSet.exhaustPile.getKeyString() + ")", MSG[0]);
                }
            }
        }
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.ui.panels.AbstractPanel;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.BobEffect;
import com.megacrit.cardcrawl.vfx.DiscardGlowEffect;
import com.megacrit.cardcrawl.vfx.ThoughtBubble;
import java.util.ArrayList;
import java.util.Iterator;

public class DiscardPilePanel
extends AbstractPanel {
    private static final TutorialStrings tutorialStrings = CardCrawlGame.languagePack.getTutorialString("Discard Tip");
    public static final String[] MSG = DiscardPilePanel.tutorialStrings.TEXT;
    public static final String[] LABEL = DiscardPilePanel.tutorialStrings.LABEL;
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("DiscardPilePanel");
    public static final String[] TEXT = DiscardPilePanel.uiStrings.TEXT;
    private static final int RAW_W = 128;
    private float scale = 1.0f;
    private static final float COUNT_CIRCLE_W = 128.0f * Settings.scale;
    private static final float DECK_X = 180.0f * Settings.scale - 64.0f;
    private static final float DECK_Y = 70.0f * Settings.scale - 64.0f;
    private static final float COUNT_X = 134.0f * Settings.scale;
    private static final float COUNT_Y = 48.0f * Settings.scale;
    private static final float COUNT_OFFSET_X = 70.0f * Settings.scale;
    private static final float COUNT_OFFSET_Y = -18.0f * Settings.scale;
    private Color glowColor = Color.WHITE.cpy();
    private float glowAlpha = 0.0f;
    private GlyphLayout gl = new GlyphLayout();
    private BobEffect bob = new BobEffect(1.0f);
    private ArrayList<DiscardGlowEffect> vfxAbove = new ArrayList();
    private ArrayList<DiscardGlowEffect> vfxBelow = new ArrayList();
    private static final float DECK_TIP_X = 1550.0f * Settings.xScale;
    private static final float DECK_TIP_Y = 470.0f * Settings.scale;
    private static final float HITBOX_W = 120.0f * Settings.scale;
    private static final float HITBOX_W2 = 450.0f * Settings.xScale;
    private Hitbox hb = new Hitbox((float)Settings.WIDTH - HITBOX_W, 0.0f, HITBOX_W, HITBOX_W);
    private Hitbox bannerHb = new Hitbox((float)Settings.WIDTH - HITBOX_W2, 0.0f, HITBOX_W2, HITBOX_W);

    public DiscardPilePanel() {
        super((float)Settings.WIDTH - 256.0f * Settings.scale, 0.0f, Settings.WIDTH, -300.0f * Settings.scale, 8.0f * Settings.xScale, 0.0f, null, true);
    }

    @Override
    public void updatePositions() {
        float tmp;
        super.updatePositions();
        this.bob.update();
        this.updateVfx();
        if (!this.isHidden) {
            this.hb.update();
            this.bannerHb.update();
            this.updatePop();
        }
        if (this.hb.hovered && (!AbstractDungeon.isScreenUp || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DISCARD_VIEW || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.HAND_SELECT || AbstractDungeon.screen == AbstractDungeon.CurrentScreen.CARD_REWARD && AbstractDungeon.overlayMenu.combatPanelsShown)) {
            AbstractDungeon.overlayMenu.hoveredTip = true;
            if (InputHelper.justClickedLeft) {
                this.hb.clickStarted = true;
            }
        }
        if ((this.hb.clicked || InputActionSet.discardPile.isJustPressed() || CInputActionSet.discardPile.isJustPressed()) && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DISCARD_VIEW) {
            this.hb.clicked = false;
            this.hb.hovered = false;
            this.bannerHb.hovered = false;
            CardCrawlGame.sound.play("DECK_CLOSE");
            if (AbstractDungeon.previousScreen == AbstractDungeon.CurrentScreen.DISCARD_VIEW) {
                AbstractDungeon.previousScreen = null;
            }
            AbstractDungeon.closeCurrentScreen();
            return;
        }
        this.glowAlpha += Gdx.graphics.getDeltaTime() * 3.0f;
        if (this.glowAlpha < 0.0f) {
            this.glowAlpha *= -1.0f;
        }
        this.glowColor.a = (tmp = MathUtils.cos(this.glowAlpha)) < 0.0f ? -tmp / 2.0f : tmp / 2.0f;
        if ((this.hb.clicked || InputActionSet.discardPile.isJustPressed() || CInputActionSet.discardPile.isJustPressed()) && AbstractDungeon.overlayMenu.combatPanelsShown && AbstractDungeon.getMonsters() != null && !AbstractDungeon.getMonsters().areMonstersDead() && !AbstractDungeon.player.isDead) {
            this.hb.clicked = false;
            this.hb.hovered = false;
            this.bannerHb.hovered = false;
            AbstractDungeon.dynamicBanner.hide();
            if (AbstractDungeon.player.hoveredCard != null) {
                AbstractDungeon.player.releaseCard();
            }
            if (AbstractDungeon.isScreenUp) {
                if (AbstractDungeon.previousScreen == null) {
                    AbstractDungeon.previousScreen = AbstractDungeon.screen;
                }
            } else {
                AbstractDungeon.previousScreen = null;
            }
            this.openDiscardPile();
        }
    }

    private void openDiscardPile() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p.discardPile.size() != 0) {
            AbstractDungeon.discardPileViewScreen.open();
        } else {
            AbstractDungeon.effectList.add(new ThoughtBubble(p.dialogX, p.dialogY, 3.0f, TEXT[0], true));
        }
        this.hb.hovered = false;
        InputHelper.justClickedLeft = false;
    }

    private void updateVfx() {
        AbstractGameEffect e;
        Iterator<DiscardGlowEffect> i = this.vfxAbove.iterator();
        while (i.hasNext()) {
            e = i.next();
            e.update();
            if (!e.isDone) continue;
            i.remove();
        }
        i = this.vfxBelow.iterator();
        while (i.hasNext()) {
            e = i.next();
            e.update();
            if (!e.isDone) continue;
            i.remove();
        }
        if (this.vfxAbove.size() < 9 && !Settings.DISABLE_EFFECTS) {
            this.vfxAbove.add(new DiscardGlowEffect(true));
        }
        if (this.vfxBelow.size() < 9 && !Settings.DISABLE_EFFECTS) {
            this.vfxBelow.add(new DiscardGlowEffect(false));
        }
    }

    private void updatePop() {
        this.scale = MathHelper.scaleLerpSnap(this.scale, Settings.scale);
    }

    public void pop() {
        this.scale = 1.75f * Settings.scale;
    }

    @Override
    public void render(SpriteBatch sb) {
        this.renderButton(sb);
        String msg = Integer.toString(AbstractDungeon.player.discardPile.size());
        this.gl.setText(FontHelper.turnNumFont, msg);
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.DECK_COUNT_CIRCLE, this.current_x + COUNT_OFFSET_X, this.current_y + COUNT_OFFSET_Y, COUNT_CIRCLE_W, COUNT_CIRCLE_W);
        if (Settings.isControllerMode) {
            sb.draw(CInputActionSet.discardPile.getKeyImg(), this.current_x - 32.0f + 220.0f * Settings.scale, this.current_y - 32.0f + 40.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale * 0.75f, Settings.scale * 0.75f, 0.0f, 0, 0, 64, 64, false, false);
        }
        FontHelper.renderFontCentered(sb, FontHelper.turnNumFont, msg, this.current_x + COUNT_X, this.current_y + COUNT_Y);
        if (!this.isHidden) {
            this.hb.render(sb);
            if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DISCARD_VIEW) {
                this.bannerHb.render(sb);
            }
        }
        if (!this.isHidden && this.hb != null && this.hb.hovered && !AbstractDungeon.isScreenUp && AbstractDungeon.getMonsters() != null && !AbstractDungeon.getMonsters().areMonstersDead()) {
            if (Settings.isConsoleBuild) {
                TipHelper.renderGenericTip(DECK_TIP_X, DECK_TIP_Y, LABEL[0] + " (" + InputActionSet.discardPile.getKeyString() + ")", MSG[1]);
            } else {
                TipHelper.renderGenericTip(DECK_TIP_X, DECK_TIP_Y, LABEL[0] + " (" + InputActionSet.discardPile.getKeyString() + ")", MSG[0]);
            }
        } else {
            this.hb.hovered = false;
        }
    }

    private void renderButton(SpriteBatch sb) {
        if (this.hb.hovered || this.bannerHb.hovered && AbstractDungeon.screen == AbstractDungeon.CurrentScreen.DISCARD_VIEW) {
            this.scale = 1.2f * Settings.scale;
        }
        for (DiscardGlowEffect e : this.vfxBelow) {
            e.render(sb, this.current_x - 1664.0f * Settings.scale, this.current_y + this.bob.y * 0.5f);
        }
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.DISCARD_BTN_BASE, this.current_x + DECK_X, this.current_y + DECK_Y + this.bob.y / 2.0f, 64.0f, 64.0f, 128.0f, 128.0f, this.scale, this.scale, 0.0f, 0, 0, 128, 128, false, false);
        for (DiscardGlowEffect e : this.vfxAbove) {
            e.render(sb, this.current_x - 1664.0f * Settings.scale, this.current_y + this.bob.y * 0.5f);
        }
    }
}


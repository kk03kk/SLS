/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.MasterDeckSortHeader;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBar;
import com.megacrit.cardcrawl.screens.mainMenu.ScrollBarListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MasterDeckViewScreen
implements ScrollBarListener {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("MasterDeckViewScreen");
    public static final String[] TEXT = MasterDeckViewScreen.uiStrings.TEXT;
    private static float drawStartX;
    private static float drawStartY;
    private static float padX;
    private static float padY;
    private static final float SCROLL_BAR_THRESHOLD;
    private static final int CARDS_PER_LINE = 5;
    private boolean grabbedScreen = false;
    private float grabStartY = 0.0f;
    private float currentDiffY = 0.0f;
    private float scrollLowerBound = -Settings.DEFAULT_SCROLL_LIMIT;
    private float scrollUpperBound = Settings.DEFAULT_SCROLL_LIMIT;
    private static final String HEADER_INFO;
    private AbstractCard hoveredCard = null;
    private AbstractCard clickStartedCard = null;
    private int prevDeckSize = 0;
    private ScrollBar scrollBar;
    private AbstractCard controllerCard = null;
    private MasterDeckSortHeader sortHeader;
    private int headerIndex = -1;
    private Comparator<AbstractCard> sortOrder = null;
    private ArrayList<AbstractCard> tmpSortedDeck = null;
    private float tmpHeaderPosition = Float.NEGATIVE_INFINITY;
    private int headerScrollLockRemainingFrames = 0;
    private boolean justSorted = false;

    public MasterDeckViewScreen() {
        drawStartX = Settings.WIDTH;
        drawStartX -= 5.0f * AbstractCard.IMG_WIDTH * 0.75f;
        drawStartX -= 4.0f * Settings.CARD_VIEW_PAD_X;
        drawStartX /= 2.0f;
        drawStartX += AbstractCard.IMG_WIDTH * 0.75f / 2.0f;
        padX = AbstractCard.IMG_WIDTH * 0.75f + Settings.CARD_VIEW_PAD_X;
        padY = AbstractCard.IMG_HEIGHT * 0.75f + Settings.CARD_VIEW_PAD_Y;
        this.scrollBar = new ScrollBar(this);
        this.scrollBar.move(0.0f, -30.0f * Settings.scale);
        this.sortHeader = new MasterDeckSortHeader(this);
    }

    public void update() {
        this.updateControllerInput();
        if (Settings.isControllerMode && this.controllerCard != null && !CardCrawlGame.isPopupOpen && !AbstractDungeon.topPanel.selectPotionMode) {
            if ((float)Gdx.input.getY() > (float)Settings.HEIGHT * 0.7f) {
                this.currentDiffY += Settings.SCROLL_SPEED;
            } else if ((float)Gdx.input.getY() < (float)Settings.HEIGHT * 0.3f) {
                this.currentDiffY -= Settings.SCROLL_SPEED;
            }
        }
        boolean isDraggingScrollBar = false;
        if (this.shouldShowScrollBar()) {
            isDraggingScrollBar = this.scrollBar.update();
        }
        if (!isDraggingScrollBar) {
            this.updateScrolling();
        }
        this.updatePositions();
        this.sortHeader.update();
        this.updateClicking();
        if (Settings.isControllerMode && this.controllerCard != null) {
            Gdx.input.setCursorPosition((int)this.controllerCard.hb.cX, (int)((float)Settings.HEIGHT - this.controllerCard.hb.cY));
        }
    }

    private void updateControllerInput() {
        if (!Settings.isControllerMode || AbstractDungeon.topPanel.selectPotionMode) {
            return;
        }
        CardGroup deck = AbstractDungeon.player.masterDeck;
        boolean anyHovered = false;
        int index = 0;
        if (this.tmpSortedDeck == null) {
            this.tmpSortedDeck = deck.group;
        }
        for (AbstractCard c : this.tmpSortedDeck) {
            if (c.hb.hovered) {
                anyHovered = true;
                break;
            }
            ++index;
        }
        boolean bl = anyHovered = anyHovered || this.headerIndex >= 0;
        if (!anyHovered) {
            if (this.tmpSortedDeck.size() > 0) {
                Gdx.input.setCursorPosition((int)this.tmpSortedDeck.get((int)0).hb.cX, (int)this.tmpSortedDeck.get((int)0).hb.cY);
                this.controllerCard = this.tmpSortedDeck.get(0);
            }
        } else if (this.headerIndex >= 0) {
            if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                this.headerIndex = -1;
                this.sortHeader.selectionIndex = -1;
                this.controllerCard = this.tmpSortedDeck.get(0);
                Gdx.input.setCursorPosition((int)this.tmpSortedDeck.get((int)0).hb.cX, Settings.HEIGHT - (int)this.tmpSortedDeck.get((int)0).hb.cY);
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                if (this.headerIndex > 0) {
                    this.selectSortButton(--this.headerIndex);
                }
            } else if ((CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) && this.headerIndex < this.sortHeader.buttons.length - 1) {
                this.selectSortButton(++this.headerIndex);
            }
        } else if ((CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) && deck.size() > 5) {
            if ((index -= 5) < 0) {
                this.headerIndex = 0;
                this.selectSortButton(0);
                return;
            }
            Gdx.input.setCursorPosition((int)this.tmpSortedDeck.get((int)index).hb.cX, Settings.HEIGHT - (int)this.tmpSortedDeck.get((int)index).hb.cY);
            this.controllerCard = this.tmpSortedDeck.get(index);
        } else if ((CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) && deck.size() > 5) {
            index = index < deck.size() - 5 ? (index += 5) : (index %= 5);
            Gdx.input.setCursorPosition((int)this.tmpSortedDeck.get((int)index).hb.cX, Settings.HEIGHT - (int)this.tmpSortedDeck.get((int)index).hb.cY);
            this.controllerCard = this.tmpSortedDeck.get(index);
        } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
            if (index % 5 > 0) {
                --index;
            } else if ((index += 4) > deck.size() - 1) {
                index = deck.size() - 1;
            }
            Gdx.input.setCursorPosition((int)this.tmpSortedDeck.get((int)index).hb.cX, Settings.HEIGHT - (int)this.tmpSortedDeck.get((int)index).hb.cY);
            this.controllerCard = this.tmpSortedDeck.get(index);
        } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
            if (index % 5 < 4) {
                if (++index > deck.size() - 1) {
                    index -= deck.size() % 5;
                }
            } else if ((index -= 4) < 0) {
                index = 0;
            }
            Gdx.input.setCursorPosition((int)this.tmpSortedDeck.get((int)index).hb.cX, Settings.HEIGHT - (int)this.tmpSortedDeck.get((int)index).hb.cY);
            this.controllerCard = this.tmpSortedDeck.get(index);
        }
    }

    public void open() {
        if (Settings.isControllerMode) {
            Gdx.input.setCursorPosition(10, Settings.HEIGHT / 2);
            this.controllerCard = null;
        }
        AbstractDungeon.player.releaseCard();
        CardCrawlGame.sound.play("DECK_OPEN");
        this.currentDiffY = this.scrollLowerBound;
        this.grabStartY = this.scrollLowerBound;
        this.grabbedScreen = false;
        this.hideCards();
        AbstractDungeon.dynamicBanner.hide();
        AbstractDungeon.isScreenUp = true;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.MASTER_DECK_VIEW;
        AbstractDungeon.overlayMenu.proceedButton.hide();
        AbstractDungeon.overlayMenu.hideCombatPanels();
        AbstractDungeon.overlayMenu.showBlackScreen();
        AbstractDungeon.overlayMenu.cancelButton.show(TEXT[1]);
        this.calculateScrollBounds();
    }

    private void updatePositions() {
        AbstractCard c;
        this.hoveredCard = null;
        int lineNum = 0;
        ArrayList<AbstractCard> cards = AbstractDungeon.player.masterDeck.group;
        if (this.sortOrder != null) {
            cards = new ArrayList<AbstractCard>(cards);
            Collections.sort(cards, this.sortOrder);
            this.tmpSortedDeck = cards;
        } else {
            this.tmpSortedDeck = null;
        }
        if (this.justSorted && this.headerScrollLockRemainingFrames <= 0 && (c = this.highestYPosition(cards)) != null) {
            this.tmpHeaderPosition = c.current_y;
        }
        for (int i = 0; i < cards.size(); ++i) {
            int mod = i % 5;
            if (mod == 0 && i != 0) {
                ++lineNum;
            }
            cards.get((int)i).target_x = drawStartX + (float)mod * padX;
            cards.get((int)i).target_y = drawStartY + this.currentDiffY - (float)lineNum * padY;
            cards.get(i).update();
            cards.get(i).updateHoverLogic();
            if (!cards.get((int)i).hb.hovered) continue;
            this.hoveredCard = cards.get(i);
        }
        c = this.highestYPosition(cards);
        if (this.justSorted && c != null) {
            int lerps = 0;
            float lerpY = c.current_y;
            float lerpTarget = c.target_y;
            while (lerpY != lerpTarget) {
                lerpY = MathHelper.cardLerpSnap(lerpY, lerpTarget);
                ++lerps;
            }
            this.headerScrollLockRemainingFrames = lerps;
        }
        this.headerScrollLockRemainingFrames -= Settings.FAST_MODE ? 2 : 1;
        if (cards.size() > 0 && this.sortHeader != null && c != null) {
            this.sortHeader.updateScrollPosition(this.headerScrollLockRemainingFrames <= 0 ? c.current_y : this.tmpHeaderPosition);
            this.justSorted = false;
        }
    }

    private AbstractCard highestYPosition(List<AbstractCard> cards) {
        if (cards == null) {
            return null;
        }
        float highestY = 0.0f;
        AbstractCard retVal = null;
        for (AbstractCard card : cards) {
            if (!(card.current_y > highestY)) continue;
            highestY = card.current_y;
            retVal = card;
        }
        return retVal;
    }

    private void updateScrolling() {
        int y = InputHelper.mY;
        if (!this.grabbedScreen) {
            if (InputHelper.scrolledDown) {
                this.currentDiffY += Settings.SCROLL_SPEED;
            } else if (InputHelper.scrolledUp) {
                this.currentDiffY -= Settings.SCROLL_SPEED;
            }
            if (InputHelper.justClickedLeft) {
                this.grabbedScreen = true;
                this.grabStartY = (float)y - this.currentDiffY;
            }
        } else if (InputHelper.isMouseDown) {
            this.currentDiffY = (float)y - this.grabStartY;
        } else {
            this.grabbedScreen = false;
        }
        if (this.prevDeckSize != AbstractDungeon.player.masterDeck.size()) {
            this.calculateScrollBounds();
        }
        this.resetScrolling();
        this.updateBarPosition();
    }

    private void updateClicking() {
        if (this.hoveredCard != null) {
            CardCrawlGame.cursor.changeType(GameCursor.CursorType.INSPECT);
            if (InputHelper.justClickedLeft) {
                this.clickStartedCard = this.hoveredCard;
            }
            if ((InputHelper.justReleasedClickLeft && this.hoveredCard == this.clickStartedCard || CInputActionSet.select.isJustPressed()) && this.headerIndex < 0) {
                InputHelper.justReleasedClickLeft = false;
                CardCrawlGame.cardPopup.open(this.hoveredCard, AbstractDungeon.player.masterDeck);
                this.clickStartedCard = null;
            }
        } else {
            this.clickStartedCard = null;
        }
    }

    private void calculateScrollBounds() {
        if (AbstractDungeon.player.masterDeck.size() > 10) {
            int scrollTmp = AbstractDungeon.player.masterDeck.size() / 5 - 2;
            if (AbstractDungeon.player.masterDeck.size() % 5 != 0) {
                ++scrollTmp;
            }
            this.scrollUpperBound = Settings.DEFAULT_SCROLL_LIMIT + (float)scrollTmp * padY;
        } else {
            this.scrollUpperBound = Settings.DEFAULT_SCROLL_LIMIT;
        }
        this.prevDeckSize = AbstractDungeon.player.masterDeck.size();
    }

    private void resetScrolling() {
        if (this.currentDiffY < this.scrollLowerBound) {
            this.currentDiffY = MathHelper.scrollSnapLerpSpeed(this.currentDiffY, this.scrollLowerBound);
        } else if (this.currentDiffY > this.scrollUpperBound) {
            this.currentDiffY = MathHelper.scrollSnapLerpSpeed(this.currentDiffY, this.scrollUpperBound);
        }
    }

    private void hideCards() {
        int lineNum = 0;
        ArrayList<AbstractCard> cards = AbstractDungeon.player.masterDeck.group;
        for (int i = 0; i < cards.size(); ++i) {
            int mod = i % 5;
            if (mod == 0 && i != 0) {
                ++lineNum;
            }
            cards.get((int)i).current_x = drawStartX + (float)mod * padX;
            cards.get((int)i).current_y = drawStartY + this.currentDiffY - (float)lineNum * padY - MathUtils.random(100.0f * Settings.scale, 200.0f * Settings.scale);
            cards.get((int)i).targetDrawScale = 0.75f;
            cards.get((int)i).drawScale = 0.75f;
            cards.get(i).setAngle(0.0f, true);
        }
    }

    public void render(SpriteBatch sb) {
        if (this.shouldShowScrollBar()) {
            this.scrollBar.render(sb);
        }
        if (this.hoveredCard == null) {
            AbstractDungeon.player.masterDeck.renderMasterDeck(sb);
        } else {
            AbstractDungeon.player.masterDeck.renderMasterDeckExceptOneCard(sb, this.hoveredCard);
            this.hoveredCard.renderHoverShadow(sb);
            this.hoveredCard.render(sb);
            if (this.hoveredCard.inBottleFlame) {
                AbstractRelic tmp = RelicLibrary.getRelic("Bottled Flame");
                float prevX = tmp.currentX;
                float prevY = tmp.currentY;
                tmp.currentX = this.hoveredCard.current_x + 130.0f * Settings.scale;
                tmp.currentY = this.hoveredCard.current_y + 182.0f * Settings.scale;
                tmp.scale = this.hoveredCard.drawScale * Settings.scale * 1.5f;
                tmp.render(sb);
                tmp.currentX = prevX;
                tmp.currentY = prevY;
                tmp = null;
            } else if (this.hoveredCard.inBottleLightning) {
                AbstractRelic tmp = RelicLibrary.getRelic("Bottled Lightning");
                float prevX = tmp.currentX;
                float prevY = tmp.currentY;
                tmp.currentX = this.hoveredCard.current_x + 130.0f * Settings.scale;
                tmp.currentY = this.hoveredCard.current_y + 182.0f * Settings.scale;
                tmp.scale = this.hoveredCard.drawScale * Settings.scale * 1.5f;
                tmp.render(sb);
                tmp.currentX = prevX;
                tmp.currentY = prevY;
                tmp = null;
            } else if (this.hoveredCard.inBottleTornado) {
                AbstractRelic tmp = RelicLibrary.getRelic("Bottled Tornado");
                float prevX = tmp.currentX;
                float prevY = tmp.currentY;
                tmp.currentX = this.hoveredCard.current_x + 130.0f * Settings.scale;
                tmp.currentY = this.hoveredCard.current_y + 182.0f * Settings.scale;
                tmp.scale = this.hoveredCard.drawScale * Settings.scale * 1.5f;
                tmp.render(sb);
                tmp.currentX = prevX;
                tmp.currentY = prevY;
                Object var2_4 = null;
            }
        }
        AbstractDungeon.player.masterDeck.renderTip(sb);
        FontHelper.renderDeckViewTip(sb, HEADER_INFO, 96.0f * Settings.scale, Settings.CREAM_COLOR);
        this.sortHeader.render(sb);
    }

    @Override
    public void scrolledUsingBar(float newPercent) {
        this.currentDiffY = MathHelper.valueFromPercentBetween(this.scrollLowerBound, this.scrollUpperBound, newPercent);
        this.updateBarPosition();
    }

    private void updateBarPosition() {
        float percent = MathHelper.percentFromValueBetween(this.scrollLowerBound, this.scrollUpperBound, this.currentDiffY);
        this.scrollBar.parentScrolledToPercent(percent);
    }

    private boolean shouldShowScrollBar() {
        return this.scrollUpperBound > SCROLL_BAR_THRESHOLD;
    }

    public void setSortOrder(Comparator<AbstractCard> sortOrder) {
        if (this.sortOrder != sortOrder) {
            this.justSorted = true;
        }
        this.sortOrder = sortOrder;
    }

    private void selectSortButton(int index) {
        Hitbox hb = this.sortHeader.buttons[this.headerIndex].hb;
        Gdx.input.setCursorPosition((int)hb.cX, Settings.HEIGHT - (int)hb.cY);
        this.controllerCard = null;
        this.sortHeader.selectionIndex = this.headerIndex;
    }

    static {
        drawStartY = (float)Settings.HEIGHT * 0.66f;
        SCROLL_BAR_THRESHOLD = 500.0f * Settings.scale;
        HEADER_INFO = TEXT[0];
    }
}


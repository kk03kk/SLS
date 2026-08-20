/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.screens.select;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.ui.buttons.CardSelectConfirmButton;
import com.megacrit.cardcrawl.ui.buttons.PeekButton;
import java.util.Iterator;

public class HandCardSelectScreen {
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("HandCardSelectScreen");
    public static final String[] TEXT = HandCardSelectScreen.uiStrings.TEXT;
    public int numCardsToSelect;
    public CardGroup selectedCards = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
    public AbstractCard hoveredCard;
    public AbstractCard upgradePreviewCard = null;
    public String selectionReason;
    public boolean wereCardsRetrieved = false;
    public boolean canPickZero = false;
    public boolean upTo = false;
    private String message = "";
    public CardSelectConfirmButton button = new CardSelectConfirmButton();
    private PeekButton peekButton = new PeekButton();
    private boolean anyNumber = false;
    private boolean forTransform = false;
    private boolean forUpgrade;
    public int numSelected = 0;
    public static final float MIN_HOVER_DIST = 64.0f * Settings.scale;
    private boolean waitThenClose = false;
    private float waitToCloseTimer = 0.0f;
    private CardGroup hand;
    public static final float HOVER_CARD_Y_POSITION = 210.0f * Settings.scale;
    private static final int ARROW_W = 64;
    private float arrowScale1 = 0.75f;
    private float arrowScale2 = 0.75f;
    private float arrowScale3 = 0.75f;
    private float arrowTimer = 0.0f;

    public void update() {
        this.updateControllerInput();
        this.peekButton.update();
        if (!PeekButton.isPeeking) {
            this.updateHand();
            this.updateSelectedCards();
            if (this.waitThenClose) {
                this.waitToCloseTimer -= Gdx.graphics.getDeltaTime();
                if (this.waitToCloseTimer < 0.0f) {
                    this.waitThenClose = false;
                    AbstractDungeon.closeCurrentScreen();
                    if (this.forTransform && this.selectedCards.size() == 1) {
                        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
                            AbstractDungeon.srcTransformCard(this.selectedCards.getBottomCard());
                        } else {
                            AbstractDungeon.transformCard(this.selectedCards.getBottomCard());
                        }
                        this.selectedCards.group.clear();
                    }
                }
            }
            if (Settings.FAST_HAND_CONF && this.numCardsToSelect == 1 && this.selectedCards.size() == 1 && !this.canPickZero && !this.waitThenClose) {
                InputHelper.justClickedLeft = false;
                this.waitToCloseTimer = 0.25f;
                this.waitThenClose = true;
                return;
            }
            this.button.update();
            if (this.button.hb.clicked || CInputActionSet.proceed.isJustPressed() || InputActionSet.confirm.isJustPressed()) {
                CInputActionSet.proceed.unpress();
                this.button.hb.clicked = false;
                if (this.canPickZero && this.selectedCards.size() == 0) {
                    InputHelper.justClickedLeft = false;
                    AbstractDungeon.closeCurrentScreen();
                    return;
                }
                if (this.anyNumber || this.upTo) {
                    InputHelper.justClickedLeft = false;
                    AbstractDungeon.closeCurrentScreen();
                    return;
                }
                if (this.selectedCards.size() == this.numCardsToSelect) {
                    InputHelper.justClickedLeft = false;
                    AbstractDungeon.closeCurrentScreen();
                    if (this.forTransform && this.selectedCards.size() == 1) {
                        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
                            AbstractDungeon.srcTransformCard(this.selectedCards.getBottomCard());
                        } else {
                            AbstractDungeon.transformCard(this.selectedCards.getBottomCard());
                        }
                        this.selectedCards.group.clear();
                    }
                    return;
                }
            }
        }
    }

    private void updateControllerInput() {
        if (!Settings.isControllerMode) {
            return;
        }
        boolean inHand = true;
        boolean anyHovered = false;
        int index = 0;
        int y = 0;
        for (AbstractCard c : this.selectedCards.group) {
            if (c.hb.hovered) {
                anyHovered = true;
                inHand = false;
                break;
            }
            ++index;
        }
        if (inHand) {
            index = 0;
            for (AbstractCard c : this.hand.group) {
                if (c == this.hoveredCard) {
                    anyHovered = true;
                    break;
                }
                ++index;
            }
        }
        if (!anyHovered) {
            if (!this.hand.group.isEmpty()) {
                this.setHoveredCard(this.hand.group.get(0));
                Gdx.input.setCursorPosition((int)this.hoveredCard.hb.cX, Settings.HEIGHT - (int)this.hoveredCard.hb.cY);
            } else if (!this.selectedCards.isEmpty()) {
                Gdx.input.setCursorPosition((int)this.selectedCards.group.get((int)0).hb.cX, Settings.HEIGHT - (int)this.selectedCards.group.get((int)0).hb.cY);
            }
        } else if (!inHand) {
            if ((CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) && !this.hand.group.isEmpty()) {
                index = 0;
                y = this.hand.group.get((int)index).hb.cY < 0.0f ? 1 : (int)this.hand.group.get((int)index).hb.cY;
                Gdx.input.setCursorPosition((int)this.hand.group.get((int)index).hb.cX, Settings.HEIGHT - y);
                this.setHoveredCard(this.hand.group.get(index));
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
                if (++index > this.selectedCards.size() - 1) {
                    index = 0;
                }
                Gdx.input.setCursorPosition((int)this.selectedCards.group.get((int)index).hb.cX, Settings.HEIGHT - (int)this.selectedCards.group.get((int)index).hb.cY);
                this.unhoverCard(this.hoveredCard);
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                if (--index < 0) {
                    index = this.selectedCards.size() - 1;
                }
                Gdx.input.setCursorPosition((int)this.selectedCards.group.get((int)index).hb.cX, Settings.HEIGHT - (int)this.selectedCards.group.get((int)index).hb.cY);
                this.unhoverCard(this.hoveredCard);
            } else if (CInputActionSet.select.isJustPressed()) {
                CInputActionSet.select.unpress();
                if (this.selectedCards.group.get((int)index).hb.hovered) {
                    AbstractCard tmp = this.selectedCards.group.get(index);
                    AbstractDungeon.player.hand.addToTop(tmp);
                    this.selectedCards.group.remove(tmp);
                    this.refreshSelectedCards();
                    this.updateMessage();
                    this.hand.refreshHandLayout();
                }
            }
        } else if ((CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) && !this.selectedCards.isEmpty()) {
            index = 0;
            y = this.selectedCards.group.get((int)index).hb.cY < 0.0f ? 1 : (int)this.selectedCards.group.get((int)index).hb.cY;
            this.unhoverCard(this.hoveredCard);
            Gdx.input.setCursorPosition((int)this.selectedCards.group.get((int)index).hb.cX, Settings.HEIGHT - y);
        } else if (this.hand.size() > 1 && (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed())) {
            if (--index < 0) {
                index = this.hand.size() - 1;
            }
            this.unhoverCard(this.hoveredCard);
            this.setHoveredCard(this.hand.group.get(index));
            Gdx.input.setCursorPosition((int)this.hoveredCard.hb.cX, Settings.HEIGHT - (int)this.hoveredCard.hb.cY);
        } else if (this.hand.size() > 1 && (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed())) {
            if (++index > this.hand.size() - 1) {
                index = 0;
            }
            this.unhoverCard(this.hoveredCard);
            this.setHoveredCard(this.hand.group.get(index));
            Gdx.input.setCursorPosition((int)this.hoveredCard.hb.cX, Settings.HEIGHT - (int)this.hoveredCard.hb.cY);
        } else if (this.hand.size() == 1 && this.hoveredCard == null) {
            this.setHoveredCard(this.hand.group.get(index));
        }
    }

    private void unhoverCard(AbstractCard card) {
        if (card == null) {
            return;
        }
        card.targetDrawScale = 0.7f;
        card.hoverTimer = 0.25f;
        card.unhover();
        card = null;
        this.hand.refreshHandLayout();
    }

    private void setHoveredCard(AbstractCard card) {
        this.hoveredCard = card;
        this.hoveredCard.current_y = HOVER_CARD_Y_POSITION;
        this.hoveredCard.target_y = HOVER_CARD_Y_POSITION;
        this.hoveredCard.drawScale = 1.0f;
        this.hoveredCard.targetDrawScale = 1.0f;
        this.hoveredCard.setAngle(0.0f, true);
        this.hand.hoverCardPush(this.hoveredCard);
    }

    private void updateHand() {
        if (!Settings.isControllerMode) {
            this.hoverCheck();
            this.unhoverCheck();
        }
        this.startDraggingCardCheck();
        this.hotkeyCheck();
    }

    private void refreshSelectedCards() {
        for (AbstractCard c : this.selectedCards.group) {
            c.target_y = (float)Settings.HEIGHT / 2.0f + 160.0f * Settings.scale;
        }
        switch (this.selectedCards.size()) {
            case 1: {
                if (this.forUpgrade) {
                    this.selectedCards.group.get((int)0).target_x = (float)Settings.WIDTH * 0.37f;
                    break;
                }
                this.selectedCards.group.get((int)0).target_x = (float)Settings.WIDTH / 2.0f;
                break;
            }
            case 2: {
                this.selectedCards.group.get((int)0).target_x = (float)Settings.WIDTH / 2.0f - 120.0f * Settings.scale;
                this.selectedCards.group.get((int)1).target_x = (float)Settings.WIDTH / 2.0f + 120.0f * Settings.scale;
                break;
            }
            case 3: {
                this.selectedCards.group.get((int)0).target_x = (float)Settings.WIDTH / 2.0f - 240.0f * Settings.scale;
                this.selectedCards.group.get((int)1).target_x = (float)Settings.WIDTH / 2.0f;
                this.selectedCards.group.get((int)2).target_x = (float)Settings.WIDTH / 2.0f + 240.0f * Settings.scale;
                break;
            }
            case 4: {
                this.selectedCards.group.get((int)0).target_x = (float)Settings.WIDTH / 2.0f - 360.0f * Settings.scale;
                this.selectedCards.group.get((int)1).target_x = (float)Settings.WIDTH / 2.0f - 120.0f * Settings.scale;
                this.selectedCards.group.get((int)2).target_x = (float)Settings.WIDTH / 2.0f + 120.0f * Settings.scale;
                this.selectedCards.group.get((int)3).target_x = (float)Settings.WIDTH / 2.0f + 360.0f * Settings.scale;
                break;
            }
            case 5: {
                this.selectedCards.group.get((int)0).target_x = (float)Settings.WIDTH / 2.0f - 360.0f * Settings.scale;
                this.selectedCards.group.get((int)1).target_x = (float)Settings.WIDTH / 2.0f - 180.0f * Settings.scale;
                this.selectedCards.group.get((int)2).target_x = (float)Settings.WIDTH / 2.0f;
                this.selectedCards.group.get((int)3).target_x = (float)Settings.WIDTH / 2.0f + 180.0f * Settings.scale;
                this.selectedCards.group.get((int)4).target_x = (float)Settings.WIDTH / 2.0f + 360.0f * Settings.scale;
                break;
            }
            case 6: {
                this.selectedCards.group.get((int)0).target_x = (float)Settings.WIDTH / 2.0f - 450.0f * Settings.scale;
                this.selectedCards.group.get((int)1).target_x = (float)Settings.WIDTH / 2.0f - 270.0f * Settings.scale;
                this.selectedCards.group.get((int)2).target_x = (float)Settings.WIDTH / 2.0f - 90.0f * Settings.scale;
                this.selectedCards.group.get((int)3).target_x = (float)Settings.WIDTH / 2.0f + 90.0f * Settings.scale;
                this.selectedCards.group.get((int)4).target_x = (float)Settings.WIDTH / 2.0f + 270.0f * Settings.scale;
                this.selectedCards.group.get((int)5).target_x = (float)Settings.WIDTH / 2.0f + 450.0f * Settings.scale;
                break;
            }
            case 7: {
                this.selectedCards.group.get((int)0).target_x = (float)Settings.WIDTH / 2.0f - 540.0f * Settings.scale;
                this.selectedCards.group.get((int)1).target_x = (float)Settings.WIDTH / 2.0f - 360.0f * Settings.scale;
                this.selectedCards.group.get((int)2).target_x = (float)Settings.WIDTH / 2.0f - 180.0f * Settings.scale;
                this.selectedCards.group.get((int)3).target_x = (float)Settings.WIDTH / 2.0f;
                this.selectedCards.group.get((int)4).target_x = (float)Settings.WIDTH / 2.0f + 180.0f * Settings.scale;
                this.selectedCards.group.get((int)5).target_x = (float)Settings.WIDTH / 2.0f + 360.0f * Settings.scale;
                this.selectedCards.group.get((int)6).target_x = (float)Settings.WIDTH / 2.0f + 540.0f * Settings.scale;
                break;
            }
            case 8: {
                this.selectedCards.group.get((int)0).target_x = (float)Settings.WIDTH / 2.0f - 630.0f * Settings.scale;
                this.selectedCards.group.get((int)1).target_x = (float)Settings.WIDTH / 2.0f - 450.0f * Settings.scale;
                this.selectedCards.group.get((int)2).target_x = (float)Settings.WIDTH / 2.0f - 270.0f * Settings.scale;
                this.selectedCards.group.get((int)3).target_x = (float)Settings.WIDTH / 2.0f - 90.0f * Settings.scale;
                this.selectedCards.group.get((int)4).target_x = (float)Settings.WIDTH / 2.0f + 90.0f * Settings.scale;
                this.selectedCards.group.get((int)5).target_x = (float)Settings.WIDTH / 2.0f + 270.0f * Settings.scale;
                this.selectedCards.group.get((int)6).target_x = (float)Settings.WIDTH / 2.0f + 450.0f * Settings.scale;
                this.selectedCards.group.get((int)7).target_x = (float)Settings.WIDTH / 2.0f + 630.0f * Settings.scale;
                break;
            }
            case 9: {
                this.selectedCards.group.get((int)0).target_x = (float)Settings.WIDTH / 2.0f - 720.0f * Settings.scale;
                this.selectedCards.group.get((int)1).target_x = (float)Settings.WIDTH / 2.0f - 540.0f * Settings.scale;
                this.selectedCards.group.get((int)2).target_x = (float)Settings.WIDTH / 2.0f - 360.0f * Settings.scale;
                this.selectedCards.group.get((int)3).target_x = (float)Settings.WIDTH / 2.0f - 180.0f * Settings.scale;
                this.selectedCards.group.get((int)4).target_x = (float)Settings.WIDTH / 2.0f;
                this.selectedCards.group.get((int)5).target_x = (float)Settings.WIDTH / 2.0f + 180.0f * Settings.scale;
                this.selectedCards.group.get((int)6).target_x = (float)Settings.WIDTH / 2.0f + 360.0f * Settings.scale;
                this.selectedCards.group.get((int)7).target_x = (float)Settings.WIDTH / 2.0f + 540.0f * Settings.scale;
                this.selectedCards.group.get((int)8).target_x = (float)Settings.WIDTH / 2.0f + 720.0f * Settings.scale;
                break;
            }
            case 10: {
                this.selectedCards.group.get((int)0).target_x = (float)Settings.WIDTH / 2.0f - 810.0f * Settings.scale;
                this.selectedCards.group.get((int)1).target_x = (float)Settings.WIDTH / 2.0f - 630.0f * Settings.scale;
                this.selectedCards.group.get((int)2).target_x = (float)Settings.WIDTH / 2.0f - 450.0f * Settings.scale;
                this.selectedCards.group.get((int)3).target_x = (float)Settings.WIDTH / 2.0f - 270.0f * Settings.scale;
                this.selectedCards.group.get((int)4).target_x = (float)Settings.WIDTH / 2.0f - 90.0f * Settings.scale;
                this.selectedCards.group.get((int)5).target_x = (float)Settings.WIDTH / 2.0f + 90.0f * Settings.scale;
                this.selectedCards.group.get((int)6).target_x = (float)Settings.WIDTH / 2.0f + 270.0f * Settings.scale;
                this.selectedCards.group.get((int)7).target_x = (float)Settings.WIDTH / 2.0f + 450.0f * Settings.scale;
                this.selectedCards.group.get((int)8).target_x = (float)Settings.WIDTH / 2.0f + 630.0f * Settings.scale;
                this.selectedCards.group.get((int)9).target_x = (float)Settings.WIDTH / 2.0f + 810.0f * Settings.scale;
                break;
            }
        }
        if (this.upTo) {
            this.button.enable();
        } else if (this.selectedCards.size() == this.numCardsToSelect) {
            this.button.enable();
        } else if (this.selectedCards.size() > 1 && this.anyNumber && !this.canPickZero) {
            this.button.enable();
        } else if (this.selectedCards.size() != this.numCardsToSelect && !this.anyNumber) {
            this.button.disable();
        } else if (this.anyNumber && this.canPickZero) {
            this.button.enable();
        }
    }

    private void hoverCheck() {
        if (this.hoveredCard == null) {
            this.hoveredCard = this.hand.getHoveredCard();
            if (this.hoveredCard != null) {
                this.hoveredCard.current_y = HOVER_CARD_Y_POSITION;
                this.hoveredCard.target_y = HOVER_CARD_Y_POSITION;
                this.hoveredCard.drawScale = 1.0f;
                this.hoveredCard.targetDrawScale = 1.0f;
                this.hoveredCard.setAngle(0.0f, true);
                this.hand.hoverCardPush(this.hoveredCard);
            }
        }
    }

    private void unhoverCheck() {
        if (this.hoveredCard != null && !this.hoveredCard.isHoveredInHand(1.0f)) {
            this.hoveredCard.targetDrawScale = 0.7f;
            this.hoveredCard.hoverTimer = 0.25f;
            this.hoveredCard.unhover();
            this.hoveredCard = null;
            this.hand.refreshHandLayout();
        }
    }

    private void startDraggingCardCheck() {
        if (this.hoveredCard != null && (InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed())) {
            CInputActionSet.select.unpress();
            if (!Settings.FAST_HAND_CONF || this.numCardsToSelect != 1 || this.selectedCards.size() != 1) {
                this.selectHoveredCard();
            }
        }
    }

    private void selectHoveredCard() {
        if (this.numCardsToSelect > this.selectedCards.group.size()) {
            InputHelper.justClickedLeft = false;
            CardCrawlGame.sound.play("CARD_OBTAIN");
            this.hand.removeCard(this.hoveredCard);
            this.hand.refreshHandLayout();
            this.hoveredCard.setAngle(0.0f, false);
            this.selectedCards.addToTop(this.hoveredCard);
            this.refreshSelectedCards();
            this.hoveredCard = null;
            this.updateMessage();
        } else if (this.numCardsToSelect == 1 && this.selectedCards.group.size() == 1) {
            InputHelper.justClickedLeft = false;
            CardCrawlGame.sound.play("CARD_OBTAIN");
            this.hand.removeCard(this.hoveredCard);
            this.hoveredCard.setAngle(0.0f, false);
            this.selectedCards.addToBottom(this.hoveredCard);
            this.refreshSelectedCards();
            this.hoveredCard = null;
            AbstractDungeon.player.hand.addToTop(this.selectedCards.getTopCard());
            this.selectedCards.removeTopCard();
            this.refreshSelectedCards();
            this.hand.refreshHandLayout();
            if (this.forUpgrade && this.selectedCards.size() == 1) {
                this.upgradePreviewCard = this.selectedCards.group.get(0).makeStatEquivalentCopy();
                this.upgradePreviewCard.upgrade();
                this.upgradePreviewCard.displayUpgrades();
                this.upgradePreviewCard.drawScale = 0.75f;
            }
        }
        InputHelper.moveCursorToNeutralPosition();
    }

    private void hotkeyCheck() {
        AbstractCard hotkeyCard = InputHelper.getCardSelectedByHotkey(this.hand);
        if (hotkeyCard != null) {
            this.hoveredCard = hotkeyCard;
            this.hoveredCard.setAngle(0.0f, false);
            this.selectHoveredCard();
        }
    }

    private void updateSelectedCards() {
        this.selectedCards.update();
        Iterator<AbstractCard> i = this.selectedCards.group.iterator();
        while (i.hasNext()) {
            AbstractCard e = i.next();
            e.current_x = MathHelper.cardLerpSnap(e.current_x, e.target_x);
            e.current_y = MathHelper.cardLerpSnap(e.current_y, e.target_y);
            e.hb.update();
            if (this.selectedCards.group.size() >= 5) {
                e.targetDrawScale = 0.5f;
                if (Math.abs(e.current_x - e.target_x) < MIN_HOVER_DIST && e.hb.hovered) {
                    e.targetDrawScale = 0.66f;
                }
            } else {
                e.targetDrawScale = 0.66f;
                if (this.forUpgrade) {
                    e.targetDrawScale = 0.75f;
                }
                if (Math.abs(e.current_x - e.target_x) < MIN_HOVER_DIST && e.hb.hovered) {
                    e.targetDrawScale = this.forUpgrade ? 0.85f : 0.75f;
                }
            }
            if (this.waitThenClose || !(Math.abs(e.current_x - e.target_x) < MIN_HOVER_DIST) || !e.hb.hovered || !InputHelper.justClickedLeft && !CInputActionSet.select.isJustPressed()) continue;
            InputHelper.justClickedLeft = false;
            AbstractDungeon.player.hand.addToTop(e);
            i.remove();
            this.refreshSelectedCards();
            this.updateMessage();
            if (!Settings.isControllerMode) break;
            this.hand.refreshHandLayout();
            break;
        }
        if (this.selectedCards.isEmpty() && !this.canPickZero) {
            this.button.disable();
        }
    }

    private void updateMessage() {
        if (this.selectedCards.group.size() == 0) {
            this.upgradePreviewCard = null;
            this.message = this.selectedCards.group.size() == this.numCardsToSelect ? (this.numCardsToSelect == 1 ? TEXT[0] + this.selectionReason : TEXT[1] + this.selectionReason) : (this.numCardsToSelect != 1 ? (!this.anyNumber ? TEXT[2] + this.numCardsToSelect + TEXT[3] + this.selectionReason : TEXT[4] + this.selectionReason) : TEXT[5] + this.selectionReason);
        } else if (this.selectedCards.group.size() != 0) {
            int numLeft = this.numCardsToSelect - this.selectedCards.group.size();
            if (this.selectedCards.group.size() == this.numCardsToSelect) {
                this.message = this.numCardsToSelect == 1 ? TEXT[0] + this.selectionReason : TEXT[1] + this.selectionReason;
                if (this.forUpgrade && this.selectedCards.size() == 1) {
                    if (this.upgradePreviewCard == null) {
                        this.upgradePreviewCard = this.selectedCards.group.get(0).makeStatEquivalentCopy();
                    }
                    this.upgradePreviewCard.upgrade();
                    this.upgradePreviewCard.displayUpgrades();
                    this.upgradePreviewCard.drawScale = 0.75f;
                    this.upgradePreviewCard.targetDrawScale = 0.75f;
                } else {
                    this.upgradePreviewCard = null;
                }
            } else if (numLeft != 1) {
                this.upgradePreviewCard = null;
                this.message = !this.anyNumber ? TEXT[2] + numLeft + TEXT[3] + this.selectionReason : TEXT[4] + this.selectionReason;
            } else {
                this.upgradePreviewCard = null;
                this.message = TEXT[5] + this.selectionReason;
            }
        }
    }

    public void reopen() {
        AbstractDungeon.overlayMenu.showBlackScreen(0.75f);
    }

    public void open(String msg, int amount, boolean anyNumber, boolean canPickZero, boolean forTransform, boolean forUpgrade, boolean upTo) {
        this.prep();
        this.numCardsToSelect = amount;
        this.canPickZero = canPickZero;
        this.anyNumber = anyNumber;
        this.selectionReason = msg;
        this.upTo = upTo;
        if (canPickZero) {
            this.button.isDisabled = true;
            this.button.enable();
        } else {
            this.button.isDisabled = false;
            this.button.disable();
        }
        this.forTransform = forTransform;
        this.forUpgrade = forUpgrade;
        if (!forUpgrade) {
            this.upgradePreviewCard = null;
        }
        this.button.hideInstantly();
        this.button.show();
        this.peekButton.hideInstantly();
        this.peekButton.show();
        this.updateMessage();
    }

    public void open(String msg, int amount, boolean anyNumber, boolean canPickZero, boolean forTransform, boolean forUpgrade) {
        this.open(msg, amount, anyNumber, canPickZero, forTransform, forUpgrade, false);
    }

    public void open(String msg, int amount, boolean anyNumber, boolean canPickZero, boolean forTransform) {
        this.open(msg, amount, anyNumber, canPickZero, forTransform, false);
    }

    public void open(String msg, int amount, boolean anyNumber, boolean canPickZero) {
        this.prep();
        this.numCardsToSelect = amount;
        this.canPickZero = canPickZero;
        this.anyNumber = anyNumber;
        this.selectionReason = msg;
        if (canPickZero) {
            this.button.isDisabled = true;
            this.button.enable();
        } else {
            this.button.isDisabled = false;
            this.button.disable();
        }
        this.button.hideInstantly();
        this.button.show();
        if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            this.peekButton.hideInstantly();
            this.peekButton.show();
        }
        this.updateMessage();
    }

    public void open(String msg, int amount, boolean anyNumber) {
        this.open(msg, amount, anyNumber, false);
    }

    public void render(SpriteBatch sb) {
        AbstractDungeon.player.hand.render(sb);
        AbstractDungeon.overlayMenu.energyPanel.render(sb);
        if (!PeekButton.isPeeking) {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, this.message, Settings.WIDTH / 2, (float)Settings.HEIGHT - 180.0f * Settings.scale, Settings.CREAM_COLOR);
            if (!Settings.FAST_HAND_CONF || this.numCardsToSelect != 1 || this.canPickZero) {
                this.button.render(sb);
            }
            this.selectedCards.render(sb);
            if (this.forUpgrade && this.upgradePreviewCard != null) {
                this.renderArrows(sb);
                this.upgradePreviewCard.current_x = (float)Settings.WIDTH * 0.63f;
                this.upgradePreviewCard.current_y = (float)Settings.HEIGHT / 2.0f + 160.0f * Settings.scale;
                this.upgradePreviewCard.target_x = (float)Settings.WIDTH * 0.63f;
                this.upgradePreviewCard.target_y = (float)Settings.HEIGHT / 2.0f + 160.0f * Settings.scale;
                this.upgradePreviewCard.displayUpgrades();
                boolean t1 = this.upgradePreviewCard.isDamageModified;
                boolean t2 = this.upgradePreviewCard.isBlockModified;
                boolean t3 = this.upgradePreviewCard.isMagicNumberModified;
                boolean t4 = this.upgradePreviewCard.isCostModified;
                this.upgradePreviewCard.applyPowers();
                if (!this.upgradePreviewCard.isDamageModified && t1) {
                    this.upgradePreviewCard.isDamageModified = true;
                }
                if (!this.upgradePreviewCard.isBlockModified && t2) {
                    this.upgradePreviewCard.isBlockModified = true;
                }
                if (!this.upgradePreviewCard.isMagicNumberModified && t3) {
                    this.upgradePreviewCard.isMagicNumberModified = true;
                }
                if (!this.upgradePreviewCard.isCostModified && t4) {
                    this.upgradePreviewCard.isCostModified = true;
                }
                this.upgradePreviewCard.render(sb);
                this.upgradePreviewCard.updateHoverLogic();
                this.upgradePreviewCard.renderCardTip(sb);
            }
        }
        this.peekButton.render(sb);
        AbstractDungeon.overlayMenu.combatDeckPanel.render(sb);
        AbstractDungeon.overlayMenu.discardPilePanel.render(sb);
        AbstractDungeon.overlayMenu.exhaustPanel.render(sb);
    }

    private void renderArrows(SpriteBatch sb) {
        float x = (float)Settings.WIDTH / 2.0f - 96.0f * Settings.scale - 10.0f * Settings.scale;
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.UPGRADE_ARROW, x, (float)Settings.HEIGHT / 2.0f + 120.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, this.arrowScale1 * Settings.scale, this.arrowScale1 * Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.UPGRADE_ARROW, x += 64.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f + 120.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, this.arrowScale2 * Settings.scale, this.arrowScale2 * Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        sb.draw(ImageMaster.UPGRADE_ARROW, x += 64.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f + 120.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, this.arrowScale3 * Settings.scale, this.arrowScale3 * Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
        this.arrowTimer += Gdx.graphics.getDeltaTime() * 2.0f;
        this.arrowScale1 = 0.8f + (MathUtils.cos(this.arrowTimer) + 1.0f) / 8.0f;
        this.arrowScale2 = 0.8f + (MathUtils.cos(this.arrowTimer - 0.8f) + 1.0f) / 8.0f;
        this.arrowScale3 = 0.8f + (MathUtils.cos(this.arrowTimer - 1.6f) + 1.0f) / 8.0f;
    }

    private void prep() {
        AbstractDungeon.player.resetControllerValues();
        for (AbstractCard c : AbstractDungeon.player.hand.group) {
            c.unhover();
        }
        this.upTo = false;
        this.forTransform = false;
        this.forUpgrade = false;
        this.canPickZero = false;
        AbstractDungeon.topPanel.unhoverHitboxes();
        AbstractDungeon.actionManager.cleanCardQueue();
        this.hand = AbstractDungeon.player.hand;
        AbstractDungeon.player.releaseCard();
        AbstractDungeon.getMonsters().hoveredMonster = null;
        this.waitThenClose = false;
        this.waitToCloseTimer = 0.0f;
        this.selectedCards.clear();
        this.hoveredCard = null;
        this.wereCardsRetrieved = false;
        AbstractDungeon.isScreenUp = true;
        AbstractDungeon.screen = AbstractDungeon.CurrentScreen.HAND_SELECT;
        AbstractDungeon.player.hand.stopGlowing();
        AbstractDungeon.player.hand.refreshHandLayout();
        AbstractDungeon.overlayMenu.showBlackScreen(0.75f);
        this.numSelected = 0;
        if (Settings.isControllerMode) {
            Gdx.input.setCursorPosition((int)this.hand.group.get((int)0).hb.cX, (int)this.hand.group.get((int)0).hb.cY);
        }
    }
}


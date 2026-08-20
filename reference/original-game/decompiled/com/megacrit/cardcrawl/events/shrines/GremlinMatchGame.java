/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.shrines;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.events.GenericEventDialog;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GremlinMatchGame
extends AbstractImageEvent {
    public static final String ID = "Match and Keep!";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Match and Keep!");
    public static final String NAME = GremlinMatchGame.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = GremlinMatchGame.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = GremlinMatchGame.eventStrings.OPTIONS;
    private AbstractCard chosenCard;
    private AbstractCard hoveredCard;
    private boolean cardFlipped = false;
    private boolean gameDone = false;
    private boolean cleanUpCalled = false;
    private int attemptCount = 5;
    private CardGroup cards = new CardGroup(CardGroup.CardGroupType.UNSPECIFIED);
    private float waitTimer = 0.0f;
    private int cardsMatched = 0;
    private CUR_SCREEN screen = CUR_SCREEN.INTRO;
    private static final String MSG_2 = DESCRIPTIONS[0];
    private static final String MSG_3 = DESCRIPTIONS[1];
    private List<String> matchedCards;

    public GremlinMatchGame() {
        super(NAME, DESCRIPTIONS[2], "images/events/matchAndKeep.jpg");
        this.cards.group = this.initializeCards();
        Collections.shuffle(this.cards.group, new Random(AbstractDungeon.miscRng.randomLong()));
        this.imageEventText.setDialogOption(OPTIONS[0]);
        this.matchedCards = new ArrayList<String>();
    }

    private ArrayList<AbstractCard> initializeCards() {
        ArrayList<AbstractCard> retVal = new ArrayList<AbstractCard>();
        ArrayList<AbstractCard> retVal2 = new ArrayList<AbstractCard>();
        if (AbstractDungeon.ascensionLevel >= 15) {
            retVal.add(AbstractDungeon.getCard(AbstractCard.CardRarity.RARE).makeCopy());
            retVal.add(AbstractDungeon.getCard(AbstractCard.CardRarity.UNCOMMON).makeCopy());
            retVal.add(AbstractDungeon.getCard(AbstractCard.CardRarity.COMMON).makeCopy());
            retVal.add(AbstractDungeon.returnRandomCurse());
            retVal.add(AbstractDungeon.returnRandomCurse());
        } else {
            retVal.add(AbstractDungeon.getCard(AbstractCard.CardRarity.RARE).makeCopy());
            retVal.add(AbstractDungeon.getCard(AbstractCard.CardRarity.UNCOMMON).makeCopy());
            retVal.add(AbstractDungeon.getCard(AbstractCard.CardRarity.COMMON).makeCopy());
            retVal.add(AbstractDungeon.returnColorlessCard(AbstractCard.CardRarity.UNCOMMON).makeCopy());
            retVal.add(AbstractDungeon.returnRandomCurse());
        }
        retVal.add(AbstractDungeon.player.getStartCardForEvent());
        for (AbstractCard c : retVal) {
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onPreviewObtainCard(c);
            }
            retVal2.add(c.makeStatEquivalentCopy());
        }
        retVal.addAll(retVal2);
        for (AbstractCard c : retVal) {
            c.target_x = c.current_x = (float)Settings.WIDTH / 2.0f;
            c.target_y = c.current_y = -300.0f * Settings.scale;
        }
        return retVal;
    }

    @Override
    public void update() {
        super.update();
        this.cards.update();
        if (this.screen == CUR_SCREEN.PLAY) {
            this.updateControllerInput();
            this.updateMatchGameLogic();
        } else if (this.screen == CUR_SCREEN.CLEAN_UP) {
            if (!this.cleanUpCalled) {
                this.cleanUpCalled = true;
                this.cleanUpCards();
            }
            if (this.waitTimer > 0.0f) {
                this.waitTimer -= Gdx.graphics.getDeltaTime();
                if (this.waitTimer < 0.0f) {
                    this.waitTimer = 0.0f;
                    this.screen = CUR_SCREEN.COMPLETE;
                    GenericEventDialog.show();
                    this.imageEventText.updateBodyText(MSG_3);
                    this.imageEventText.clearRemainingOptions();
                    this.imageEventText.setDialogOption(OPTIONS[1]);
                }
            }
        }
        if (!GenericEventDialog.waitForInput) {
            this.buttonEffect(GenericEventDialog.getSelectedOption());
        }
    }

    private void updateControllerInput() {
        if (!Settings.isControllerMode) {
            return;
        }
        boolean anyHovered = false;
        int index = 0;
        for (AbstractCard c : this.cards.group) {
            if (c.hb.hovered) {
                anyHovered = true;
                break;
            }
            ++index;
        }
        if (!anyHovered) {
            Gdx.input.setCursorPosition((int)this.cards.group.get((int)0).hb.cX, Settings.HEIGHT - (int)this.cards.group.get((int)0).hb.cY);
        } else {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.altUp.isJustPressed()) {
                float y = this.cards.group.get((int)index).hb.cY + 230.0f * Settings.scale;
                if (y > 865.0f * Settings.scale) {
                    y = 290.0f * Settings.scale;
                }
                Gdx.input.setCursorPosition((int)this.cards.group.get((int)index).hb.cX, (int)((float)Settings.HEIGHT - y));
            } else if (CInputActionSet.down.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                float y = this.cards.group.get((int)index).hb.cY - 230.0f * Settings.scale;
                if (y < 175.0f * Settings.scale) {
                    y = 750.0f * Settings.scale;
                }
                Gdx.input.setCursorPosition((int)this.cards.group.get((int)index).hb.cX, (int)((float)Settings.HEIGHT - y));
            } else if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
                float x = this.cards.group.get((int)index).hb.cX - 210.0f * Settings.scale;
                if (x < 530.0f * Settings.scale) {
                    x = 1270.0f * Settings.scale;
                }
                Gdx.input.setCursorPosition((int)x, Settings.HEIGHT - (int)this.cards.group.get((int)index).hb.cY);
            } else if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
                float x = this.cards.group.get((int)index).hb.cX + 210.0f * Settings.scale;
                if (x > 1375.0f * Settings.scale) {
                    x = 640.0f * Settings.scale;
                }
                Gdx.input.setCursorPosition((int)x, Settings.HEIGHT - (int)this.cards.group.get((int)index).hb.cY);
            }
            if (CInputActionSet.select.isJustPressed()) {
                CInputActionSet.select.unpress();
                InputHelper.justClickedLeft = true;
            }
        }
    }

    private void cleanUpCards() {
        for (AbstractCard c : this.cards.group) {
            c.targetDrawScale = 0.5f;
            c.target_x = (float)Settings.WIDTH / 2.0f;
            c.target_y = -300.0f * Settings.scale;
        }
    }

    private void updateMatchGameLogic() {
        if (this.waitTimer == 0.0f) {
            this.hoveredCard = null;
            for (AbstractCard c : this.cards.group) {
                c.hb.update();
                if (this.hoveredCard == null && c.hb.hovered) {
                    c.drawScale = 0.7f;
                    c.targetDrawScale = 0.7f;
                    this.hoveredCard = c;
                    if (!InputHelper.justClickedLeft || !this.hoveredCard.isFlipped) continue;
                    InputHelper.justClickedLeft = false;
                    this.hoveredCard.isFlipped = false;
                    if (!this.cardFlipped) {
                        this.cardFlipped = true;
                        this.chosenCard = this.hoveredCard;
                        continue;
                    }
                    this.cardFlipped = false;
                    if (this.chosenCard.cardID.equals(this.hoveredCard.cardID)) {
                        this.waitTimer = 1.0f;
                        this.chosenCard.targetDrawScale = 0.7f;
                        this.chosenCard.target_x = (float)Settings.WIDTH / 2.0f;
                        this.chosenCard.target_y = (float)Settings.HEIGHT / 2.0f;
                        this.hoveredCard.targetDrawScale = 0.7f;
                        this.hoveredCard.target_x = (float)Settings.WIDTH / 2.0f;
                        this.hoveredCard.target_y = (float)Settings.HEIGHT / 2.0f;
                        continue;
                    }
                    this.waitTimer = 1.25f;
                    this.chosenCard.targetDrawScale = 1.0f;
                    this.hoveredCard.targetDrawScale = 1.0f;
                    continue;
                }
                if (c == this.chosenCard) continue;
                c.targetDrawScale = 0.5f;
            }
        } else {
            this.waitTimer -= Gdx.graphics.getDeltaTime();
            if (this.waitTimer < 0.0f && !this.gameDone) {
                this.waitTimer = 0.0f;
                if (this.chosenCard.cardID.equals(this.hoveredCard.cardID)) {
                    ++this.cardsMatched;
                    this.cards.group.remove(this.chosenCard);
                    this.cards.group.remove(this.hoveredCard);
                    this.matchedCards.add(this.chosenCard.cardID);
                    AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(this.chosenCard.makeCopy(), (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                    this.chosenCard = null;
                    this.hoveredCard = null;
                } else {
                    this.chosenCard.isFlipped = true;
                    this.hoveredCard.isFlipped = true;
                    this.chosenCard.targetDrawScale = 0.5f;
                    this.hoveredCard.targetDrawScale = 0.5f;
                    this.chosenCard = null;
                    this.hoveredCard = null;
                }
                --this.attemptCount;
                if (this.attemptCount == 0) {
                    this.gameDone = true;
                    this.waitTimer = 1.0f;
                }
            } else if (this.gameDone) {
                this.screen = CUR_SCREEN.CLEAN_UP;
            }
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(MSG_2);
                        this.imageEventText.updateDialogOption(0, OPTIONS[2]);
                        this.screen = CUR_SCREEN.RULE_EXPLANATION;
                    }
                }
                break;
            }
            case RULE_EXPLANATION: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.removeDialogOption(0);
                        GenericEventDialog.hide();
                        this.screen = CUR_SCREEN.PLAY;
                        this.placeCards();
                    }
                }
                break;
            }
            case COMPLETE: {
                GremlinMatchGame.logMetricObtainCards(ID, this.cardsMatched + " cards matched", this.matchedCards);
                this.openMap();
                break;
            }
        }
    }

    private void placeCards() {
        for (int i = 0; i < this.cards.size(); ++i) {
            this.cards.group.get((int)i).target_x = (float)(i % 4) * 210.0f * Settings.xScale + 640.0f * Settings.xScale;
            this.cards.group.get((int)i).target_y = (float)(i % 3) * -230.0f * Settings.yScale + 750.0f * Settings.yScale;
            this.cards.group.get((int)i).targetDrawScale = 0.5f;
            this.cards.group.get((int)i).isFlipped = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        this.cards.render(sb);
        if (this.chosenCard != null) {
            this.chosenCard.render(sb);
        }
        if (this.hoveredCard != null) {
            this.hoveredCard.render(sb);
        }
        if (this.screen == CUR_SCREEN.PLAY) {
            FontHelper.renderSmartText(sb, FontHelper.panelNameFont, OPTIONS[3] + this.attemptCount, 780.0f * Settings.scale, 80.0f * Settings.scale, 2000.0f * Settings.scale, 0.0f, Color.WHITE);
        }
    }

    @Override
    public void renderAboveTopPanel(SpriteBatch sb) {
    }

    private static enum CUR_SCREEN {
        INTRO,
        RULE_EXPLANATION,
        PLAY,
        COMPLETE,
        CLEAN_UP;

    }
}


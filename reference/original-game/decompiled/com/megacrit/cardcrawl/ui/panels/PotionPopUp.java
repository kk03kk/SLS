/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.ui.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Bezier;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.megacrit.cardcrawl.actions.utility.HandCheckAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.GameCursor;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.shrines.WeMeetAgain;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.MathHelper;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.potions.SmokeBomb;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.ThoughtBubble;
import java.util.ArrayList;

public class PotionPopUp {
    private static final TutorialStrings tutorialStrings = CardCrawlGame.languagePack.getTutorialString("Potion Panel Tip");
    public static final String[] MSG = PotionPopUp.tutorialStrings.TEXT;
    public static final String[] LABEL = PotionPopUp.tutorialStrings.LABEL;
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("PotionPopUp");
    public static final String[] TEXT = PotionPopUp.uiStrings.TEXT;
    private int slot;
    private AbstractPotion potion;
    public boolean isHidden = true;
    public boolean targetMode = false;
    private Hitbox hbTop;
    private Hitbox hbBot;
    private Color topHoverColor = new Color(0.5f, 0.9f, 1.0f, 0.0f);
    private Color botHoverColor = new Color(1.0f, 0.4f, 0.3f, 0.0f);
    private float x;
    private float y;
    private static final int SEGMENTS = 20;
    private Vector2[] points = new Vector2[20];
    private Vector2 controlPoint;
    private float arrowScale;
    private float arrowScaleTimer = 0.0f;
    private static final float ARROW_TARGET_SCALE = 1.2f;
    private static final int TARGET_ARROW_W = 256;
    private AbstractMonster hoveredMonster = null;
    private boolean autoTargetFirst = false;

    public PotionPopUp() {
        this.hbTop = new Hitbox(286.0f * Settings.scale, 120.0f * Settings.scale);
        this.hbBot = new Hitbox(286.0f * Settings.scale, 90.0f * Settings.scale);
        for (int i = 0; i < this.points.length; ++i) {
            this.points[i] = new Vector2();
        }
    }

    public void open(int slot, AbstractPotion potion) {
        this.topHoverColor.a = 0.0f;
        this.botHoverColor.a = 0.0f;
        AbstractDungeon.topPanel.selectPotionMode = false;
        this.slot = slot;
        this.potion = potion;
        this.x = potion.posX;
        this.y = (float)Settings.HEIGHT - 230.0f * Settings.scale;
        this.isHidden = false;
        this.hbTop.move(this.x, this.y + 44.0f * Settings.scale);
        this.hbBot.move(this.x, this.y - 76.0f * Settings.scale);
        this.hbTop.clickStarted = false;
        this.hbBot.clickStarted = false;
        this.hbTop.clicked = false;
        this.hbBot.clicked = false;
    }

    public void close() {
        this.isHidden = true;
    }

    public void update() {
        if (!this.isHidden) {
            this.updateControllerInput();
            this.hbTop.update();
            this.hbBot.update();
            this.updateInput();
            if (this.potion != null) {
                TipHelper.queuePowerTips(this.x + 180.0f * Settings.scale, this.y + 70.0f * Settings.scale, this.potion.tips);
            }
        } else if (this.targetMode) {
            this.updateControllerTargetInput();
            this.updateTargetMode();
        }
    }

    private void updateControllerTargetInput() {
        if (!Settings.isControllerMode) {
            return;
        }
        int offsetEnemyIndex = 0;
        if (this.autoTargetFirst) {
            this.autoTargetFirst = false;
            ++offsetEnemyIndex;
        }
        if (CInputActionSet.left.isJustPressed() || CInputActionSet.altLeft.isJustPressed()) {
            --offsetEnemyIndex;
        }
        if (CInputActionSet.right.isJustPressed() || CInputActionSet.altRight.isJustPressed()) {
            ++offsetEnemyIndex;
        }
        if (offsetEnemyIndex != 0) {
            AbstractMonster newTarget;
            ArrayList<AbstractMonster> prefiltered = AbstractDungeon.getCurrRoom().monsters.monsters;
            ArrayList<AbstractMonster> sortedMonsters = new ArrayList<AbstractMonster>(AbstractDungeon.getCurrRoom().monsters.monsters);
            for (AbstractMonster mons : prefiltered) {
                if (!mons.isDying) continue;
                sortedMonsters.remove(mons);
            }
            sortedMonsters.sort(AbstractMonster.sortByHitbox);
            if (sortedMonsters.isEmpty()) {
                return;
            }
            for (AbstractMonster m : sortedMonsters) {
                if (!m.hb.hovered) continue;
                this.hoveredMonster = m;
                break;
            }
            if (this.hoveredMonster == null) {
                newTarget = offsetEnemyIndex == 1 ? sortedMonsters.get(0) : sortedMonsters.get(sortedMonsters.size() - 1);
            } else {
                int currentTargetIndex = sortedMonsters.indexOf(this.hoveredMonster);
                int newTargetIndex = currentTargetIndex + offsetEnemyIndex;
                newTargetIndex = (newTargetIndex + sortedMonsters.size()) % sortedMonsters.size();
                newTarget = sortedMonsters.get(newTargetIndex);
            }
            if (newTarget != null) {
                Hitbox target = newTarget.hb;
                Gdx.input.setCursorPosition((int)target.cX, Settings.HEIGHT - (int)target.cY);
                this.hoveredMonster = newTarget;
            }
            if (this.hoveredMonster.halfDead) {
                this.hoveredMonster = null;
            }
        }
    }

    private void updateControllerInput() {
        if (!Settings.isControllerMode) {
            return;
        }
        if (CInputActionSet.cancel.isJustPressed()) {
            CInputActionSet.cancel.unpress();
            this.close();
            return;
        }
        if (!this.hbTop.hovered && !this.hbBot.hovered) {
            if (this.potion.canUse()) {
                Gdx.input.setCursorPosition((int)this.hbTop.cX, Settings.HEIGHT - (int)this.hbTop.cY);
            } else {
                Gdx.input.setCursorPosition((int)this.hbBot.cX, Settings.HEIGHT - (int)this.hbBot.cY);
            }
        } else if (this.hbTop.hovered) {
            if (CInputActionSet.up.isJustPressed() || CInputActionSet.down.isJustPressed() || CInputActionSet.altUp.isJustPressed() || CInputActionSet.altDown.isJustPressed()) {
                Gdx.input.setCursorPosition((int)this.hbBot.cX, Settings.HEIGHT - (int)this.hbBot.cY);
            }
        } else if (this.hbBot.hovered && this.potion.canUse() && (CInputActionSet.up.isJustPressed() || CInputActionSet.down.isJustPressed() || CInputActionSet.altUp.isJustPressed() || CInputActionSet.altDown.isJustPressed())) {
            Gdx.input.setCursorPosition((int)this.hbTop.cX, Settings.HEIGHT - (int)this.hbTop.cY);
        }
    }

    private void updateTargetMode() {
        if (InputHelper.justClickedRight || AbstractDungeon.isScreenUp || (float)InputHelper.mY > (float)Settings.HEIGHT - 80.0f * Settings.scale || AbstractDungeon.player.hoveredCard != null || (float)InputHelper.mY < 140.0f * Settings.scale || CInputActionSet.cancel.isJustPressed()) {
            CInputActionSet.cancel.unpress();
            this.targetMode = false;
            GameCursor.hidden = false;
        }
        this.hoveredMonster = null;
        for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
            if (!m.hb.hovered || m.isDying) continue;
            this.hoveredMonster = m;
            break;
        }
        if (InputHelper.justClickedLeft || CInputActionSet.select.isJustPressed()) {
            InputHelper.justClickedLeft = false;
            CInputActionSet.select.unpress();
            if (this.hoveredMonster != null) {
                if (AbstractDungeon.player.hasPower("Surrounded")) {
                    AbstractDungeon.player.flipHorizontal = this.hoveredMonster.drawX < AbstractDungeon.player.drawX;
                }
                CardCrawlGame.metricData.potions_floor_usage.add(AbstractDungeon.floorNum);
                this.potion.use(this.hoveredMonster);
                if (AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
                    AbstractDungeon.actionManager.addToBottom(new HandCheckAction());
                }
                for (AbstractRelic r : AbstractDungeon.player.relics) {
                    r.onUsePotion();
                }
                AbstractDungeon.topPanel.destroyPotion(this.slot);
                this.targetMode = false;
                GameCursor.hidden = false;
            }
        }
    }

    private void updateInput() {
        if (InputHelper.justClickedLeft) {
            InputHelper.justClickedLeft = false;
            if (this.hbTop.hovered) {
                this.hbTop.clickStarted = true;
            } else if (this.hbBot.hovered) {
                this.hbBot.clickStarted = true;
            } else {
                this.close();
            }
        }
        if ((this.hbTop.clicked || this.hbTop.hovered && CInputActionSet.select.isJustPressed()) && (!AbstractDungeon.isScreenUp || this.potion.canUse())) {
            CInputActionSet.select.unpress();
            this.hbTop.clicked = false;
            if (this.potion.canUse()) {
                if (!this.potion.targetRequired) {
                    CardCrawlGame.metricData.potions_floor_usage.add(AbstractDungeon.floorNum);
                    this.potion.use(null);
                    for (AbstractRelic r : AbstractDungeon.player.relics) {
                        r.onUsePotion();
                    }
                    CardCrawlGame.sound.play("POTION_1");
                    AbstractDungeon.topPanel.destroyPotion(this.slot);
                } else {
                    this.targetMode = true;
                    GameCursor.hidden = true;
                    this.autoTargetFirst = true;
                }
                this.close();
            } else if (this.potion.ID == "SmokeBomb" && AbstractDungeon.getCurrRoom().monsters != null) {
                for (AbstractMonster m : AbstractDungeon.getCurrRoom().monsters.monsters) {
                    if (!m.hasPower("BackAttack")) continue;
                    AbstractDungeon.effectList.add(new ThoughtBubble(AbstractDungeon.player.dialogX, AbstractDungeon.player.dialogY, 3.0f, SmokeBomb.potionStrings.DESCRIPTIONS[1], true));
                }
            }
        } else if ((this.hbBot.clicked || this.hbBot.hovered && CInputActionSet.select.isJustPressed()) && this.potion.canDiscard()) {
            CInputActionSet.select.unpress();
            this.hbBot.clicked = false;
            CardCrawlGame.sound.play("POTION_DROP_2");
            AbstractDungeon.topPanel.destroyPotion(this.slot);
            this.slot = -1;
            this.potion = null;
            this.close();
        }
    }

    public void render(SpriteBatch sb) {
        if (!this.isHidden) {
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.POTION_UI_BG, this.x - 200.0f, this.y - 169.0f, 200.0f, 169.0f, 400.0f, 338.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 400, 338, false, false);
            this.topHoverColor.a = this.hbTop.hovered ? 0.5f : MathHelper.fadeLerpSnap(this.topHoverColor.a, 0.0f);
            this.botHoverColor.a = this.hbBot.hovered ? 0.5f : MathHelper.fadeLerpSnap(this.botHoverColor.a, 0.0f);
            sb.setBlendFunction(770, 1);
            sb.setColor(this.topHoverColor);
            sb.draw(ImageMaster.POTION_UI_TOP, this.x - 200.0f, this.y - 169.0f, 200.0f, 169.0f, 400.0f, 338.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 400, 338, false, false);
            sb.setColor(this.botHoverColor);
            sb.draw(ImageMaster.POTION_UI_BOT, this.x - 200.0f, this.y - 169.0f, 200.0f, 169.0f, 400.0f, 338.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 400, 338, false, false);
            sb.setBlendFunction(770, 771);
            Color c = Settings.CREAM_COLOR;
            if (!this.potion.canUse() || AbstractDungeon.isScreenUp) {
                c = Color.GRAY;
            }
            if (this.potion.canUse()) {
                if (AbstractDungeon.getCurrRoom().event != null) {
                    if (!(AbstractDungeon.getCurrRoom().event instanceof WeMeetAgain)) {
                        c = Settings.CREAM_COLOR;
                    }
                } else {
                    c = Settings.CREAM_COLOR;
                }
            }
            String label = TEXT[1];
            if (this.potion.isThrown) {
                label = TEXT[0];
            }
            FontHelper.renderFontCenteredWidth(sb, FontHelper.buttonLabelFont, label, this.x, this.hbTop.cY + 4.0f * Settings.scale, c);
            FontHelper.renderFontCenteredWidth(sb, FontHelper.buttonLabelFont, TEXT[2], this.x, this.hbBot.cY + 12.0f * Settings.scale, Settings.RED_TEXT_COLOR);
            this.hbTop.render(sb);
            this.hbBot.render(sb);
            if (this.hbTop.hovered) {
                if (this.potion.isThrown) {
                    TipHelper.renderGenericTip(this.x + 174.0f * Settings.scale, this.y + 20.0f * Settings.scale, LABEL[0], MSG[0]);
                } else {
                    TipHelper.renderGenericTip(this.x + 174.0f * Settings.scale, this.y + 20.0f * Settings.scale, LABEL[1], MSG[1]);
                }
            } else if (this.hbBot.hovered) {
                TipHelper.renderGenericTip(this.x + 174.0f * Settings.scale, this.y + 20.0f * Settings.scale, LABEL[2], MSG[2]);
            }
        }
        if (this.targetMode) {
            if (this.hoveredMonster != null) {
                this.hoveredMonster.renderReticle(sb);
            }
            this.renderTargetingUi(sb);
        }
    }

    private void renderTargetingUi(SpriteBatch sb) {
        float x = InputHelper.mX;
        float y = InputHelper.mY;
        this.controlPoint = new Vector2(this.potion.posX - (x - this.potion.posX) / 4.0f, y + (y - this.potion.posY - 40.0f * Settings.scale) / 2.0f);
        if (this.hoveredMonster == null) {
            this.arrowScale = Settings.scale;
            this.arrowScaleTimer = 0.0f;
            sb.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
        } else {
            this.arrowScaleTimer += Gdx.graphics.getDeltaTime();
            if (this.arrowScaleTimer > 1.0f) {
                this.arrowScaleTimer = 1.0f;
            }
            this.arrowScale = Interpolation.elasticOut.apply(Settings.scale, Settings.scale * 1.2f, this.arrowScaleTimer);
            sb.setColor(new Color(1.0f, 0.2f, 0.3f, 1.0f));
        }
        Vector2 tmp = new Vector2(this.controlPoint.x - x, this.controlPoint.y - y);
        tmp.nor();
        this.drawCurvedLine(sb, new Vector2(this.potion.posX, this.potion.posY - 40.0f * Settings.scale), new Vector2(x, y), this.controlPoint);
        sb.draw(ImageMaster.TARGET_UI_ARROW, x - 128.0f, y - 128.0f, 128.0f, 128.0f, 256.0f, 256.0f, this.arrowScale, this.arrowScale, tmp.angle() + 90.0f, 0, 0, 256, 256, false, false);
    }

    private void drawCurvedLine(SpriteBatch sb, Vector2 start, Vector2 end, Vector2 control) {
        float radius = 7.0f * Settings.scale;
        for (int i = 0; i < this.points.length - 1; ++i) {
            float angle;
            Vector2 tmp;
            this.points[i] = Bezier.quadratic(this.points[i], (float)i / 20.0f, start, control, end, new Vector2());
            radius += 0.4f * Settings.scale;
            if (i != 0) {
                tmp = new Vector2(this.points[i - 1].x - this.points[i].x, this.points[i - 1].y - this.points[i].y);
                angle = tmp.nor().angle() + 90.0f;
            } else {
                tmp = new Vector2(this.controlPoint.x - this.points[i].x, this.controlPoint.y - this.points[i].y);
                angle = tmp.nor().angle() + 270.0f;
            }
            sb.draw(ImageMaster.TARGET_UI_CIRCLE, this.points[i].x - 64.0f, this.points[i].y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, radius / 18.0f, radius / 18.0f, angle, 0, 0, 128, 128, false, false);
        }
    }
}


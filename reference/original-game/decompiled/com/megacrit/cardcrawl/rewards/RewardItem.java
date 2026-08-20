/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rewards;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.TipTracker;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.localization.UIStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.MonsterRoomBoss;
import com.megacrit.cardcrawl.rooms.TreasureRoom;
import com.megacrit.cardcrawl.ui.FtueTip;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.ObtainKeyEffect;
import com.megacrit.cardcrawl.vfx.RewardGlowEffect;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RewardItem {
    private static final Logger logger = LogManager.getLogger(RewardItem.class.getName());
    private static final UIStrings uiStrings = CardCrawlGame.languagePack.getUIString("RewardItem");
    public static final String[] TEXT = RewardItem.uiStrings.TEXT;
    private static final TutorialStrings tutorialStrings = CardCrawlGame.languagePack.getTutorialString("Potion Tip");
    public static final String[] MSG = RewardItem.tutorialStrings.TEXT;
    public static final String[] LABEL = RewardItem.tutorialStrings.LABEL;
    public RewardType type;
    public Texture outlineImg = null;
    public Texture img = null;
    public int goldAmt = 0;
    public int bonusGold = 0;
    public String text;
    public RewardItem relicLink;
    public AbstractRelic relic;
    public AbstractPotion potion;
    public ArrayList<AbstractCard> cards;
    private ArrayList<AbstractGameEffect> effects = new ArrayList();
    private boolean isBoss;
    public Hitbox hb = new Hitbox(460.0f * Settings.xScale, 90.0f * Settings.yScale);
    public float y;
    public float flashTimer = 0.0f;
    public boolean isDone = false;
    public boolean ignoreReward = false;
    public boolean redText = false;
    private static final float FLASH_DUR = 0.5f;
    private static final int ITEM_W = 464;
    private static final int ITEM_H = 98;
    public static final float REWARD_ITEM_X = (float)Settings.WIDTH * 0.41f;
    private static final float REWARD_TEXT_X = (float)Settings.WIDTH * 0.434f;
    private Color reticleColor = new Color(1.0f, 1.0f, 1.0f, 0.0f);

    public RewardItem(RewardItem setRelicLink, RewardType type) {
        this.type = type;
        if (type == RewardType.SAPPHIRE_KEY) {
            this.text = TEXT[6];
            this.img = ImageMaster.loadImage("images/relics/sapphire_key.png");
            this.outlineImg = ImageMaster.loadImage("images/relics/outline/sapphire_key.png");
            this.relicLink = setRelicLink;
            setRelicLink.relicLink = this;
        } else if (type == RewardType.EMERALD_KEY) {
            this.text = TEXT[5];
            this.img = ImageMaster.loadImage("images/relics/emerald_key.png");
            this.outlineImg = ImageMaster.loadImage("images/relics/outline/emerald_key.png");
        }
    }

    public RewardItem(int gold) {
        this.type = RewardType.GOLD;
        this.goldAmt = gold;
        this.applyGoldBonus(false);
    }

    public RewardItem(int gold, boolean theft) {
        this.type = RewardType.STOLEN_GOLD;
        this.goldAmt = gold;
        this.applyGoldBonus(theft);
    }

    private void applyGoldBonus(boolean theft) {
        int tmp = this.goldAmt;
        this.bonusGold = 0;
        if (theft) {
            this.text = this.goldAmt + TEXT[0];
        } else {
            if (!(AbstractDungeon.getCurrRoom() instanceof TreasureRoom)) {
                if (AbstractDungeon.player.hasRelic("Golden Idol")) {
                    this.bonusGold += MathUtils.round((float)tmp * 0.25f);
                }
                if (ModHelper.isModEnabled("Midas")) {
                    this.bonusGold += MathUtils.round((float)tmp * 2.0f);
                }
                if (ModHelper.isModEnabled("MonsterHunter")) {
                    this.bonusGold += MathUtils.round((float)tmp * 1.5f);
                }
            }
            this.text = this.bonusGold == 0 ? this.goldAmt + TEXT[1] : this.goldAmt + TEXT[1] + " (" + this.bonusGold + ")";
        }
    }

    public RewardItem(AbstractRelic relic) {
        this.type = RewardType.RELIC;
        this.relic = relic;
        relic.hb = new Hitbox(80.0f * Settings.scale, 80.0f * Settings.scale);
        relic.hb.move(-1000.0f, -1000.0f);
        this.text = relic.name;
    }

    public RewardItem(AbstractPotion potion) {
        this.type = RewardType.POTION;
        this.potion = potion;
        this.text = potion.name;
    }

    public RewardItem() {
        this.type = RewardType.CARD;
        this.isBoss = AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss;
        this.cards = AbstractDungeon.getRewardCards();
        this.text = TEXT[2];
    }

    public RewardItem(AbstractCard.CardColor colorType) {
        this.type = RewardType.CARD;
        this.isBoss = AbstractDungeon.getCurrRoom() instanceof MonsterRoomBoss;
        this.cards = colorType == AbstractCard.CardColor.COLORLESS ? AbstractDungeon.getColorlessRewardCards() : AbstractDungeon.getRewardCards();
        this.text = TEXT[2];
        for (AbstractCard c : this.cards) {
            for (AbstractRelic r : AbstractDungeon.player.relics) {
                r.onPreviewObtainCard(c);
            }
        }
    }

    public void incrementGold(int gold) {
        if (this.type == RewardType.GOLD) {
            this.goldAmt += gold;
            this.applyGoldBonus(false);
        } else if (this.type == RewardType.STOLEN_GOLD) {
            this.goldAmt += gold;
            this.applyGoldBonus(true);
        } else {
            logger.info("ERROR: Using increment() wrong for RewardItem");
        }
    }

    public void move(float y) {
        this.y = y;
        this.hb.move((float)Settings.WIDTH / 2.0f, y);
        if (this.type == RewardType.POTION) {
            this.potion.move(REWARD_ITEM_X, y);
        } else if (this.type == RewardType.RELIC) {
            this.relic.currentX = REWARD_ITEM_X;
            this.relic.currentY = y;
            this.relic.targetX = REWARD_ITEM_X;
            this.relic.targetY = y;
        }
    }

    public void flash() {
        this.flashTimer = 0.5f;
    }

    public void update() {
        if (this.flashTimer > 0.0f) {
            this.flashTimer -= Gdx.graphics.getDeltaTime();
            if (this.flashTimer < 0.0f) {
                this.flashTimer = 0.0f;
            }
        }
        this.hb.update();
        this.updateReticle();
        if (this.effects.size() == 0) {
            this.effects.add(new RewardGlowEffect(this.hb.cX, this.hb.cY));
        }
        Iterator<AbstractGameEffect> i = this.effects.iterator();
        while (i.hasNext()) {
            AbstractGameEffect e = i.next();
            e.update();
            if (!e.isDone) continue;
            i.remove();
        }
        if (this.hb.hovered) {
            switch (this.type) {
                case POTION: {
                    if (AbstractDungeon.topPanel.potionCombine) break;
                    TipHelper.queuePowerTips(360.0f * Settings.scale, InputHelper.mY, this.potion.tips);
                    break;
                }
            }
        }
        if (this.relicLink != null) {
            this.relicLink.redText = this.hb.hovered;
        }
        if (this.hb.justHovered) {
            CardCrawlGame.sound.play("UI_HOVER");
        }
        if (this.hb.hovered && InputHelper.justClickedLeft && !this.isDone) {
            CardCrawlGame.sound.playA("UI_CLICK_1", 0.1f);
            this.hb.clickStarted = true;
        }
        if (this.hb.hovered && CInputActionSet.select.isJustPressed() && !this.isDone) {
            this.hb.clicked = true;
            CardCrawlGame.sound.playA("UI_CLICK_1", 0.1f);
        }
        if (this.hb.clicked) {
            this.hb.clicked = false;
            this.isDone = true;
        }
    }

    private void updateReticle() {
        if (!Settings.isControllerMode) {
            return;
        }
        if (this.hb.hovered) {
            this.reticleColor.a += Gdx.graphics.getDeltaTime() * 3.0f;
            if (this.reticleColor.a > 1.0f) {
                this.reticleColor.a = 1.0f;
            }
        } else {
            this.reticleColor.a = 0.0f;
        }
    }

    public boolean claimReward() {
        switch (this.type) {
            case GOLD: {
                CardCrawlGame.sound.play("GOLD_GAIN");
                if (this.bonusGold == 0) {
                    AbstractDungeon.player.gainGold(this.goldAmt);
                } else {
                    AbstractDungeon.player.gainGold(this.goldAmt + this.bonusGold);
                }
                return true;
            }
            case STOLEN_GOLD: {
                CardCrawlGame.sound.play("GOLD_GAIN");
                if (this.bonusGold == 0) {
                    AbstractDungeon.player.gainGold(this.goldAmt);
                } else {
                    AbstractDungeon.player.gainGold(this.goldAmt + this.bonusGold);
                }
                return true;
            }
            case POTION: {
                if (AbstractDungeon.player.hasRelic("Sozu")) {
                    AbstractDungeon.player.getRelic("Sozu").flash();
                    return true;
                }
                if (AbstractDungeon.player.obtainPotion(this.potion)) {
                    if (!TipTracker.tips.get("POTION_TIP").booleanValue()) {
                        AbstractDungeon.ftue = new FtueTip(LABEL[0], MSG[0], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f, this.potion);
                        TipTracker.neverShowAgain("POTION_TIP");
                    }
                    CardCrawlGame.metricData.addPotionObtainData(this.potion);
                    return true;
                }
                return false;
            }
            case RELIC: {
                if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.GRID) {
                    return false;
                }
                if (!this.ignoreReward) {
                    this.relic.instantObtain();
                    CardCrawlGame.metricData.addRelicObtainData(this.relic);
                }
                if (this.relicLink != null) {
                    this.relicLink.isDone = true;
                    this.relicLink.ignoreReward = true;
                }
                return true;
            }
            case CARD: {
                if (AbstractDungeon.player.hasRelic("Question Card")) {
                    AbstractDungeon.player.getRelic("Question Card").flash();
                }
                if (AbstractDungeon.player.hasRelic("Busted Crown")) {
                    AbstractDungeon.player.getRelic("Busted Crown").flash();
                }
                if (AbstractDungeon.screen == AbstractDungeon.CurrentScreen.COMBAT_REWARD) {
                    AbstractDungeon.cardRewardScreen.open(this.cards, this, TEXT[4]);
                    AbstractDungeon.previousScreen = AbstractDungeon.CurrentScreen.COMBAT_REWARD;
                }
                return false;
            }
            case SAPPHIRE_KEY: {
                if (!this.ignoreReward) {
                    AbstractDungeon.topLevelEffects.add(new ObtainKeyEffect(ObtainKeyEffect.KeyColor.BLUE));
                }
                this.relicLink.isDone = true;
                this.relicLink.ignoreReward = true;
                this.img.dispose();
                this.outlineImg.dispose();
                return true;
            }
            case EMERALD_KEY: {
                AbstractDungeon.topLevelEffects.add(new ObtainKeyEffect(ObtainKeyEffect.KeyColor.GREEN));
                this.img.dispose();
                this.outlineImg.dispose();
                return true;
            }
        }
        logger.info("ERROR: Claim Reward() failed");
        return false;
    }

    public void render(SpriteBatch sb) {
        if (this.hb.hovered) {
            sb.setColor(new Color(0.4f, 0.6f, 0.6f, 1.0f));
        } else {
            sb.setColor(new Color(0.5f, 0.6f, 0.6f, 0.8f));
        }
        if (this.hb.clickStarted) {
            sb.draw(ImageMaster.REWARD_SCREEN_ITEM, (float)Settings.WIDTH / 2.0f - 232.0f, this.y - 49.0f, 232.0f, 49.0f, 464.0f, 98.0f, Settings.xScale * 0.98f, Settings.scale * 0.98f, 0.0f, 0, 0, 464, 98, false, false);
        } else {
            sb.draw(ImageMaster.REWARD_SCREEN_ITEM, (float)Settings.WIDTH / 2.0f - 232.0f, this.y - 49.0f, 232.0f, 49.0f, 464.0f, 98.0f, Settings.xScale, Settings.scale, 0.0f, 0, 0, 464, 98, false, false);
        }
        if (this.flashTimer != 0.0f) {
            sb.setColor(0.6f, 1.0f, 1.0f, this.flashTimer * 1.5f);
            sb.setBlendFunction(770, 1);
            sb.draw(ImageMaster.REWARD_SCREEN_ITEM, (float)Settings.WIDTH / 2.0f - 232.0f, this.y - 49.0f, 232.0f, 49.0f, 464.0f, 98.0f, Settings.xScale * 1.03f, Settings.scale * 1.15f, 0.0f, 0, 0, 464, 98, false, false);
            sb.setBlendFunction(770, 771);
        }
        sb.setColor(Color.WHITE);
        switch (this.type) {
            case CARD: {
                if (this.isBoss) {
                    sb.draw(ImageMaster.REWARD_CARD_BOSS, REWARD_ITEM_X - 32.0f, this.y - 32.0f - 2.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
                    break;
                }
                sb.draw(ImageMaster.REWARD_CARD_NORMAL, REWARD_ITEM_X - 32.0f, this.y - 32.0f - 2.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
                break;
            }
            case GOLD: {
                sb.draw(ImageMaster.UI_GOLD, REWARD_ITEM_X - 32.0f, this.y - 32.0f - 2.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
                break;
            }
            case STOLEN_GOLD: {
                sb.draw(ImageMaster.UI_GOLD, REWARD_ITEM_X - 32.0f, this.y - 32.0f - 2.0f * Settings.scale, 32.0f, 32.0f, 64.0f, 64.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 64, 64, false, false);
                break;
            }
            case RELIC: {
                this.relic.renderWithoutAmount(sb, new Color(0.0f, 0.0f, 0.0f, 0.25f));
                if (!this.hb.hovered) break;
                if (this.relicLink != null) {
                    ArrayList<PowerTip> tips = new ArrayList<PowerTip>();
                    tips.add(new PowerTip(this.relic.name, this.relic.description));
                    if (this.relicLink.type == RewardType.SAPPHIRE_KEY) {
                        tips.add(new PowerTip(TEXT[7], TEXT[8] + FontHelper.colorString(TEXT[6] + TEXT[9], "y")));
                    }
                    TipHelper.queuePowerTips(360.0f * Settings.scale, (float)InputHelper.mY + 50.0f * Settings.scale, tips);
                    break;
                }
                this.relic.renderTip(sb);
                break;
            }
            case SAPPHIRE_KEY: {
                this.renderKey(sb);
                if (!this.hb.hovered || this.relicLink == null) break;
                TipHelper.renderGenericTip(360.0f * Settings.scale, (float)InputHelper.mY + 50.0f * Settings.scale, TEXT[7], TEXT[8] + FontHelper.colorString(this.relicLink.relic.name + TEXT[9], "y"));
                break;
            }
            case EMERALD_KEY: {
                this.renderKey(sb);
                break;
            }
            case POTION: {
                this.potion.renderLightOutline(sb);
                this.potion.render(sb);
                this.potion.generateSparkles(this.potion.posX, this.potion.posY, true);
                break;
            }
        }
        Color color = this.hb.hovered ? Settings.GOLD_COLOR : Settings.CREAM_COLOR;
        if (this.redText) {
            color = Settings.RED_TEXT_COLOR;
        }
        FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, this.text, REWARD_TEXT_X, this.y + 5.0f * Settings.scale, 1000.0f * Settings.scale, 0.0f, color);
        if (!this.hb.hovered) {
            for (AbstractGameEffect e : this.effects) {
                e.render(sb);
            }
        }
        if (Settings.isControllerMode) {
            this.renderReticle(sb, this.hb);
        }
        if (this.type == RewardType.SAPPHIRE_KEY) {
            this.renderRelicLink(sb);
        }
        this.hb.render(sb);
    }

    public void renderReticle(SpriteBatch sb, Hitbox hb) {
        float offset = Interpolation.fade.apply(18.0f * Settings.scale, 12.0f * Settings.scale, this.reticleColor.a);
        sb.setColor(this.reticleColor);
        this.renderReticleCorner(sb, -hb.width / 2.0f + offset, hb.height / 2.0f - offset, hb, false, false);
        this.renderReticleCorner(sb, hb.width / 2.0f - offset, hb.height / 2.0f - offset, hb, true, false);
        this.renderReticleCorner(sb, -hb.width / 2.0f + offset, -hb.height / 2.0f + offset, hb, false, true);
        this.renderReticleCorner(sb, hb.width / 2.0f - offset, -hb.height / 2.0f + offset, hb, true, true);
    }

    private void renderReticleCorner(SpriteBatch sb, float x, float y, Hitbox hb, boolean flipX, boolean flipY) {
        sb.draw(ImageMaster.RETICLE_CORNER, hb.cX + x - 18.0f, hb.cY + y - 18.0f, 18.0f, 18.0f, 36.0f, 36.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 36, 36, flipX, flipY);
    }

    private void renderRelicLink(SpriteBatch sb) {
        sb.setColor(Color.WHITE);
        sb.draw(ImageMaster.RELIC_LINKED, this.hb.cX - 64.0f, this.y - 64.0f + 52.0f * Settings.scale, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 128, 128, false, false);
    }

    private void renderKey(SpriteBatch sb) {
        if (this.img != null && this.outlineImg != null) {
            sb.setColor(AbstractRelic.PASSIVE_OUTLINE_COLOR);
            sb.draw(this.outlineImg, REWARD_ITEM_X - 64.0f, this.y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 128, 128, false, false);
            sb.setColor(Color.WHITE);
            sb.draw(this.img, REWARD_ITEM_X - 64.0f, this.y - 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 128, 128, false, false);
        }
    }

    public void recordCardSkipMetrics() {
        if (this.cards != null && !this.cards.isEmpty()) {
            HashMap<String, Object> choice = new HashMap<String, Object>();
            ArrayList<String> notpicked = new ArrayList<String>();
            for (AbstractCard card : this.cards) {
                notpicked.add(card.getMetricID());
            }
            choice.put("picked", "SKIP");
            choice.put("not_picked", notpicked);
            choice.put("floor", AbstractDungeon.floorNum);
            CardCrawlGame.metricData.card_choices.add(choice);
        }
    }

    public static enum RewardType {
        CARD,
        GOLD,
        RELIC,
        POTION,
        STOLEN_GOLD,
        EMERALD_KEY,
        SAPPHIRE_KEY;

    }
}


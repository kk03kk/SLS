/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.city;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.vfx.RainingGoldEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KnowingSkull
extends AbstractImageEvent {
    private static final Logger logger = LogManager.getLogger(KnowingSkull.class.getName());
    public static final String ID = "Knowing Skull";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Knowing Skull");
    public static final String NAME = KnowingSkull.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = KnowingSkull.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = KnowingSkull.eventStrings.OPTIONS;
    private static final String INTRO_MSG = DESCRIPTIONS[0];
    private static final String INTRO_2_MSG = DESCRIPTIONS[1];
    private static final String ASK_AGAIN_MSG = DESCRIPTIONS[2];
    private static final String POTION_MSG = DESCRIPTIONS[4];
    private static final String CARD_MSG = DESCRIPTIONS[5];
    private static final String GOLD_MSG = DESCRIPTIONS[6];
    private static final String LEAVE_MSG = DESCRIPTIONS[7];
    private int potionCost;
    private int cardCost;
    private int goldCost;
    private int leaveCost;
    private static final int GOLD_REWARD = 90;
    private CurScreen screen = CurScreen.INTRO_1;
    private String optionsChosen = "";
    private int damageTaken;
    private int goldEarned;
    private List<String> potions;
    private List<String> cards;
    private ArrayList<Reward> options = new ArrayList();

    public KnowingSkull() {
        super(NAME, INTRO_MSG, "images/events/knowingSkull.jpg");
        this.imageEventText.setDialogOption(OPTIONS[0]);
        this.options.add(Reward.CARD);
        this.options.add(Reward.GOLD);
        this.options.add(Reward.POTION);
        this.options.add(Reward.LEAVE);
        this.cardCost = this.leaveCost = 6;
        this.potionCost = this.leaveCost;
        this.goldCost = this.leaveCost;
        this.damageTaken = 0;
        this.goldEarned = 0;
        this.cards = new ArrayList<String>();
        this.potions = new ArrayList<String>();
    }

    @Override
    public void onEnterRoom() {
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.play("EVENT_SKULL");
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        block0 : switch (this.screen) {
            case INTRO_1: {
                this.imageEventText.updateBodyText(INTRO_2_MSG);
                this.imageEventText.clearAllDialogs();
                this.imageEventText.setDialogOption(OPTIONS[4] + this.potionCost + OPTIONS[1]);
                this.imageEventText.setDialogOption(OPTIONS[5] + 90 + OPTIONS[6] + this.goldCost + OPTIONS[1]);
                this.imageEventText.setDialogOption(OPTIONS[3] + this.cardCost + OPTIONS[1]);
                this.imageEventText.setDialogOption(OPTIONS[7] + this.leaveCost + OPTIONS[1]);
                this.screen = CurScreen.ASK;
                break;
            }
            case ASK: {
                CardCrawlGame.sound.play("DEBUFF_2");
                switch (buttonPressed) {
                    case 0: {
                        this.obtainReward(0);
                        break block0;
                    }
                    case 1: {
                        this.obtainReward(1);
                        break block0;
                    }
                    case 2: {
                        this.obtainReward(2);
                        break block0;
                    }
                }
                AbstractDungeon.player.damage(new DamageInfo(null, this.leaveCost, DamageInfo.DamageType.HP_LOSS));
                this.damageTaken += this.leaveCost;
                this.setLeave();
                break;
            }
            case COMPLETE: {
                KnowingSkull.logMetric(ID, this.optionsChosen, this.cards, null, null, null, null, this.potions, null, this.damageTaken, 0, 0, 0, this.goldEarned, 0);
                this.openMap();
            }
        }
    }

    private void obtainReward(int slot) {
        switch (slot) {
            case 0: {
                AbstractDungeon.player.damage(new DamageInfo(null, this.potionCost, DamageInfo.DamageType.HP_LOSS));
                this.damageTaken += this.potionCost;
                ++this.potionCost;
                this.optionsChosen = this.optionsChosen + "POTION ";
                this.imageEventText.updateBodyText(POTION_MSG + ASK_AGAIN_MSG);
                if (AbstractDungeon.player.hasRelic("Sozu")) {
                    AbstractDungeon.player.getRelic("Sozu").flash();
                    break;
                }
                AbstractPotion p = PotionHelper.getRandomPotion();
                this.potions.add(p.ID);
                AbstractDungeon.player.obtainPotion(p);
                break;
            }
            case 1: {
                AbstractDungeon.player.damage(new DamageInfo(null, this.goldCost, DamageInfo.DamageType.HP_LOSS));
                this.damageTaken += this.goldCost;
                ++this.goldCost;
                this.optionsChosen = this.optionsChosen + "GOLD ";
                this.imageEventText.updateBodyText(GOLD_MSG + ASK_AGAIN_MSG);
                AbstractDungeon.effectList.add(new RainingGoldEffect(90));
                AbstractDungeon.player.gainGold(90);
                this.goldEarned += 90;
                break;
            }
            case 2: {
                AbstractDungeon.player.damage(new DamageInfo(null, this.cardCost, DamageInfo.DamageType.HP_LOSS));
                this.damageTaken += this.cardCost;
                ++this.cardCost;
                this.optionsChosen = this.optionsChosen + "CARD ";
                this.imageEventText.updateBodyText(CARD_MSG + ASK_AGAIN_MSG);
                AbstractCard c = AbstractDungeon.returnColorlessCard(AbstractCard.CardRarity.UNCOMMON).makeCopy();
                this.cards.add(c.cardID);
                AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(c, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                break;
            }
            default: {
                logger.info("This should never happen.");
            }
        }
        this.imageEventText.clearAllDialogs();
        this.imageEventText.setDialogOption(OPTIONS[4] + this.potionCost + OPTIONS[1]);
        this.imageEventText.setDialogOption(OPTIONS[5] + 90 + OPTIONS[6] + this.goldCost + OPTIONS[1]);
        this.imageEventText.setDialogOption(OPTIONS[3] + this.cardCost + OPTIONS[1]);
        this.imageEventText.setDialogOption(OPTIONS[7] + this.leaveCost + OPTIONS[1]);
    }

    private void setLeave() {
        this.imageEventText.updateBodyText(LEAVE_MSG);
        this.imageEventText.clearAllDialogs();
        this.imageEventText.setDialogOption(OPTIONS[8]);
        this.screen = CurScreen.COMPLETE;
    }

    private static enum Reward {
        POTION,
        LEAVE,
        GOLD,
        CARD;

    }

    private static enum CurScreen {
        INTRO_1,
        ASK,
        COMPLETE;

    }
}


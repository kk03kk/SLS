/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.beyond;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.curses.Doubt;
import com.megacrit.cardcrawl.cards.curses.Normality;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.helpers.MonsterHelper;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.RainingGoldEffect;
import com.megacrit.cardcrawl.vfx.UpgradeShineEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardBrieflyEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class MindBloom
extends AbstractImageEvent {
    public static final String ID = "MindBloom";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("MindBloom");
    public static final String NAME = MindBloom.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = MindBloom.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = MindBloom.eventStrings.OPTIONS;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private static final String DIALOG_2 = DESCRIPTIONS[1];
    private static final String DIALOG_3 = DESCRIPTIONS[2];
    private CurScreen screen = CurScreen.INTRO;

    public MindBloom() {
        super(NAME, DIALOG_1, "images/events/mindBloom.jpg");
        this.imageEventText.setDialogOption(OPTIONS[0]);
        this.imageEventText.setDialogOption(OPTIONS[3]);
        if (AbstractDungeon.floorNum % 50 <= 40) {
            this.imageEventText.setDialogOption(OPTIONS[1], CardLibrary.getCopy("Normality"));
        } else {
            this.imageEventText.setDialogOption(OPTIONS[2], CardLibrary.getCopy("Doubt"));
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(DIALOG_2);
                        this.screen = CurScreen.FIGHT;
                        MindBloom.logMetric(ID, "Fight");
                        CardCrawlGame.music.playTempBgmInstantly("MINDBLOOM", true);
                        ArrayList<String> list = new ArrayList<String>();
                        list.add("The Guardian");
                        list.add("Hexaghost");
                        list.add("Slime Boss");
                        Collections.shuffle(list, new Random(AbstractDungeon.miscRng.randomLong()));
                        AbstractDungeon.getCurrRoom().monsters = MonsterHelper.getEncounter((String)list.get(0));
                        AbstractDungeon.getCurrRoom().rewards.clear();
                        if (AbstractDungeon.ascensionLevel >= 13) {
                            AbstractDungeon.getCurrRoom().addGoldToRewards(25);
                        } else {
                            AbstractDungeon.getCurrRoom().addGoldToRewards(50);
                        }
                        AbstractDungeon.getCurrRoom().addRelicToRewards(AbstractRelic.RelicTier.RARE);
                        this.enterCombatFromImage();
                        AbstractDungeon.lastCombatMetricKey = "Mind Bloom Boss Battle";
                        break;
                    }
                    case 1: {
                        this.imageEventText.updateBodyText(DIALOG_3);
                        this.screen = CurScreen.LEAVE;
                        int effectCount = 0;
                        ArrayList<String> upgradedCards = new ArrayList<String>();
                        ArrayList<String> obtainedRelic = new ArrayList<String>();
                        for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
                            if (!c.canUpgrade()) continue;
                            if (++effectCount <= 20) {
                                float x = MathUtils.random(0.1f, 0.9f) * (float)Settings.WIDTH;
                                float y = MathUtils.random(0.2f, 0.8f) * (float)Settings.HEIGHT;
                                AbstractDungeon.effectList.add(new ShowCardBrieflyEffect(c.makeStatEquivalentCopy(), x, y));
                                AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect(x, y));
                            }
                            upgradedCards.add(c.cardID);
                            c.upgrade();
                            AbstractDungeon.player.bottledCardUpgradeCheck(c);
                        }
                        AbstractDungeon.getCurrRoom().spawnRelicAndObtain((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f, RelicLibrary.getRelic("Mark of the Bloom").makeCopy());
                        obtainedRelic.add("Mark of the Bloom");
                        MindBloom.logMetric(ID, "Upgrade", null, null, null, upgradedCards, obtainedRelic, null, null, 0, 0, 0, 0, 0, 0);
                        this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                        break;
                    }
                    case 2: {
                        if (AbstractDungeon.floorNum % 50 <= 40) {
                            this.imageEventText.updateBodyText(DIALOG_2);
                            this.screen = CurScreen.LEAVE;
                            ArrayList<String> cardsAdded = new ArrayList<String>();
                            cardsAdded.add("Normality");
                            cardsAdded.add("Normality");
                            MindBloom.logMetric(ID, "Gold", cardsAdded, null, null, null, null, null, null, 0, 0, 0, 0, 999, 0);
                            AbstractDungeon.effectList.add(new RainingGoldEffect(999));
                            AbstractDungeon.player.gainGold(999);
                            AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(new Normality(), (float)Settings.WIDTH * 0.6f, (float)Settings.HEIGHT / 2.0f));
                            AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(new Normality(), (float)Settings.WIDTH * 0.3f, (float)Settings.HEIGHT / 2.0f));
                            this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                            break;
                        }
                        this.imageEventText.updateBodyText(DIALOG_2);
                        this.screen = CurScreen.LEAVE;
                        Doubt curse = new Doubt();
                        MindBloom.logMetricObtainCardAndHeal(ID, "Heal", curse, AbstractDungeon.player.maxHealth - AbstractDungeon.player.currentHealth);
                        AbstractDungeon.player.heal(AbstractDungeon.player.maxHealth);
                        AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(curse, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                        this.imageEventText.updateDialogOption(0, OPTIONS[4]);
                        break;
                    }
                }
                this.imageEventText.clearRemainingOptions();
                break;
            }
            case LEAVE: {
                this.openMap();
                break;
            }
            default: {
                this.openMap();
            }
        }
    }

    private static enum CurScreen {
        INTRO,
        FIGHT,
        LEAVE;

    }
}


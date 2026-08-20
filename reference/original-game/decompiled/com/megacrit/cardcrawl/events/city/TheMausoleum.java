/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.city;

import com.megacrit.cardcrawl.cards.curses.Writhe;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.helpers.CardLibrary;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

public class TheMausoleum
extends AbstractImageEvent {
    public static final String ID = "The Mausoleum";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("The Mausoleum");
    public static final String NAME = TheMausoleum.eventStrings.NAME;
    public static final String[] DESCRIPTIONS = TheMausoleum.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = TheMausoleum.eventStrings.OPTIONS;
    private static final String DIALOG_1 = DESCRIPTIONS[0];
    private static final String CURSED_RESULT = DESCRIPTIONS[1];
    private static final String NORMAL_RESULT = DESCRIPTIONS[2];
    private static final String NOPE_RESULT = DESCRIPTIONS[3];
    private CurScreen screen = CurScreen.INTRO;
    private static final int PERCENT = 50;
    private static final int A_2_PERCENT = 100;
    private int percent = AbstractDungeon.ascensionLevel >= 15 ? 100 : 50;

    public TheMausoleum() {
        super(NAME, DIALOG_1, "images/events/mausoleum.jpg");
        this.imageEventText.setDialogOption(OPTIONS[0] + this.percent + OPTIONS[1], CardLibrary.getCopy("Writhe"));
        this.imageEventText.setDialogOption(OPTIONS[2]);
    }

    @Override
    public void onEnterRoom() {
        if (Settings.AMBIANCE_ON) {
            CardCrawlGame.sound.play("EVENT_GHOSTS");
        }
    }

    @Override
    public void update() {
        super.update();
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.screen) {
            case INTRO: {
                switch (buttonPressed) {
                    case 0: {
                        boolean result = AbstractDungeon.miscRng.randomBoolean();
                        if (AbstractDungeon.ascensionLevel >= 15) {
                            result = true;
                        }
                        if (result) {
                            this.imageEventText.updateBodyText(CURSED_RESULT);
                            AbstractDungeon.effectList.add(new ShowCardAndObtainEffect(new Writhe(), Settings.WIDTH / 2, Settings.HEIGHT / 2));
                        } else {
                            this.imageEventText.updateBodyText(NORMAL_RESULT);
                        }
                        CardCrawlGame.sound.play("BLUNT_HEAVY");
                        CardCrawlGame.screenShake.rumble(2.0f);
                        AbstractRelic r = AbstractDungeon.returnRandomScreenlessRelic(AbstractDungeon.returnRandomRelicTier());
                        AbstractDungeon.getCurrRoom().spawnRelicAndObtain(Settings.WIDTH / 2, Settings.HEIGHT / 2, r);
                        if (result) {
                            TheMausoleum.logMetricObtainCardAndRelic(ID, "Opened", new Writhe(), r);
                            break;
                        }
                        TheMausoleum.logMetricObtainRelic(ID, "Opened", r);
                        break;
                    }
                    default: {
                        this.imageEventText.updateBodyText(NOPE_RESULT);
                        TheMausoleum.logMetricIgnored(ID);
                    }
                }
                this.imageEventText.clearAllDialogs();
                this.imageEventText.setDialogOption(OPTIONS[2]);
                this.screen = CurScreen.RESULT;
                break;
            }
            default: {
                this.openMap();
            }
        }
    }

    private static enum CurScreen {
        INTRO,
        RESULT;

    }
}


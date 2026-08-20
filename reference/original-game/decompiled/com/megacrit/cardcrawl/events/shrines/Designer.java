/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.events.shrines;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.AbstractImageEvent;
import com.megacrit.cardcrawl.localization.EventStrings;
import com.megacrit.cardcrawl.vfx.UpgradeShineEffect;
import com.megacrit.cardcrawl.vfx.cardManip.PurgeCardEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardBrieflyEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Designer
extends AbstractImageEvent {
    public static final String ID = "Designer";
    private static final EventStrings eventStrings = CardCrawlGame.languagePack.getEventString("Designer");
    public static final String NAME = Designer.eventStrings.NAME;
    public static final String[] DESC = Designer.eventStrings.DESCRIPTIONS;
    public static final String[] OPTIONS = Designer.eventStrings.OPTIONS;
    private CurrentScreen curScreen = CurrentScreen.INTRO;
    private OptionChosen option = null;
    public static final int GOLD_REQ = 75;
    public static final int UPG_AMT = 2;
    public static final int REMOVE_AMT = 2;
    private boolean adjustmentUpgradesOne;
    private boolean cleanUpRemovesCards;
    private int adjustCost;
    private int cleanUpCost;
    private int fullServiceCost;
    private int hpLoss;

    public Designer() {
        super(NAME, DESC[0], "images/events/designer2.jpg");
        this.imageEventText.setDialogOption(OPTIONS[0]);
        this.option = OptionChosen.NONE;
        this.adjustmentUpgradesOne = AbstractDungeon.miscRng.randomBoolean();
        this.cleanUpRemovesCards = AbstractDungeon.miscRng.randomBoolean();
        if (AbstractDungeon.ascensionLevel >= 15) {
            this.adjustCost = 50;
            this.cleanUpCost = 75;
            this.fullServiceCost = 110;
            this.hpLoss = 5;
        } else {
            this.adjustCost = 40;
            this.cleanUpCost = 60;
            this.fullServiceCost = 90;
            this.hpLoss = 3;
        }
    }

    @Override
    public void update() {
        super.update();
        if (this.option != OptionChosen.NONE) {
            switch (this.option) {
                case REMOVE: {
                    if (AbstractDungeon.isScreenUp || AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) break;
                    CardCrawlGame.sound.play("CARD_EXHAUST");
                    AbstractCard c = AbstractDungeon.gridSelectScreen.selectedCards.get(0);
                    Designer.logMetricCardRemovalAtCost(ID, "Single Remove", c, this.cleanUpCost);
                    AbstractDungeon.topLevelEffects.add(new PurgeCardEffect(c, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                    AbstractDungeon.player.masterDeck.removeCard(c);
                    AbstractDungeon.gridSelectScreen.selectedCards.clear();
                    this.option = OptionChosen.NONE;
                    break;
                }
                case REMOVE_AND_UPGRADE: {
                    if (AbstractDungeon.isScreenUp || AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) break;
                    AbstractCard removeCard = AbstractDungeon.gridSelectScreen.selectedCards.get(0);
                    CardCrawlGame.sound.play("CARD_EXHAUST");
                    AbstractDungeon.topLevelEffects.add(new PurgeCardEffect(removeCard, (float)Settings.WIDTH / 2.0f - AbstractCard.IMG_WIDTH - 20.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f));
                    AbstractDungeon.player.masterDeck.removeCard(removeCard);
                    AbstractDungeon.gridSelectScreen.selectedCards.clear();
                    ArrayList<AbstractCard> upgradableCards = new ArrayList<AbstractCard>();
                    for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
                        if (!c.canUpgrade()) continue;
                        upgradableCards.add(c);
                    }
                    Collections.shuffle(upgradableCards, new Random(AbstractDungeon.miscRng.randomLong()));
                    if (!upgradableCards.isEmpty()) {
                        AbstractCard upgradeCard = (AbstractCard)upgradableCards.get(0);
                        upgradeCard.upgrade();
                        AbstractDungeon.player.bottledCardUpgradeCheck(upgradeCard);
                        AbstractDungeon.topLevelEffects.add(new ShowCardBrieflyEffect(upgradeCard.makeStatEquivalentCopy()));
                        AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                        Designer.logMetricCardUpgradeAndRemovalAtCost(ID, "Upgrade and Remove", upgradeCard, removeCard, this.fullServiceCost);
                    } else {
                        Designer.logMetricCardRemovalAtCost(ID, "Removal", removeCard, this.fullServiceCost);
                    }
                    this.option = OptionChosen.NONE;
                    break;
                }
                case TRANSFORM: {
                    if (AbstractDungeon.isScreenUp || AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) break;
                    ArrayList<String> transCards = new ArrayList<String>();
                    ArrayList<String> obtainedCards = new ArrayList<String>();
                    if (AbstractDungeon.gridSelectScreen.selectedCards.size() == 2) {
                        AbstractCard c = AbstractDungeon.gridSelectScreen.selectedCards.get(0);
                        AbstractDungeon.player.masterDeck.removeCard(c);
                        transCards.add(c.cardID);
                        AbstractDungeon.transformCard(c, false, AbstractDungeon.miscRng);
                        AbstractCard newCard1 = AbstractDungeon.getTransformedCard();
                        obtainedCards.add(newCard1.cardID);
                        AbstractDungeon.effectsQueue.add(new ShowCardAndObtainEffect(newCard1, (float)Settings.WIDTH / 2.0f - AbstractCard.IMG_WIDTH / 2.0f - 20.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f));
                        c = AbstractDungeon.gridSelectScreen.selectedCards.get(1);
                        AbstractDungeon.player.masterDeck.removeCard(c);
                        transCards.add(c.cardID);
                        AbstractDungeon.transformCard(c, false, AbstractDungeon.miscRng);
                        AbstractCard newCard2 = AbstractDungeon.getTransformedCard();
                        obtainedCards.add(newCard2.cardID);
                        AbstractDungeon.effectsQueue.add(new ShowCardAndObtainEffect(newCard2, (float)Settings.WIDTH / 2.0f + AbstractCard.IMG_WIDTH / 2.0f + 20.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f));
                        AbstractDungeon.gridSelectScreen.selectedCards.clear();
                        Designer.logMetricTransformCardsAtCost(ID, "Transformed Cards", transCards, obtainedCards, this.cleanUpCost);
                    } else {
                        AbstractCard c = AbstractDungeon.gridSelectScreen.selectedCards.get(0);
                        AbstractDungeon.player.masterDeck.removeCard(c);
                        AbstractDungeon.transformCard(c, false, AbstractDungeon.miscRng);
                        AbstractCard transCard = AbstractDungeon.getTransformedCard();
                        AbstractDungeon.effectsQueue.add(new ShowCardAndObtainEffect(transCard, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                        AbstractDungeon.gridSelectScreen.selectedCards.clear();
                        Designer.logMetricTransformCardAtCost(ID, "Transform", transCard, c, this.cleanUpCost);
                    }
                    this.option = OptionChosen.NONE;
                    break;
                }
                case UPGRADE: {
                    if (AbstractDungeon.isScreenUp || AbstractDungeon.gridSelectScreen.selectedCards.isEmpty()) break;
                    Designer.logMetricCardUpgradeAtCost(ID, "Upgrade", AbstractDungeon.gridSelectScreen.selectedCards.get(0), this.adjustCost);
                    AbstractDungeon.gridSelectScreen.selectedCards.get(0).upgrade();
                    AbstractDungeon.player.bottledCardUpgradeCheck(AbstractDungeon.gridSelectScreen.selectedCards.get(0));
                    AbstractDungeon.effectsQueue.add(new ShowCardBrieflyEffect(AbstractDungeon.gridSelectScreen.selectedCards.get(0).makeStatEquivalentCopy()));
                    AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
                    AbstractDungeon.gridSelectScreen.selectedCards.clear();
                    this.option = OptionChosen.NONE;
                    break;
                }
            }
        }
    }

    @Override
    protected void buttonEffect(int buttonPressed) {
        switch (this.curScreen) {
            case INTRO: {
                this.imageEventText.updateBodyText(DESC[1]);
                this.imageEventText.removeDialogOption(0);
                if (this.adjustmentUpgradesOne) {
                    this.imageEventText.updateDialogOption(0, OPTIONS[1] + this.adjustCost + OPTIONS[6] + OPTIONS[9], AbstractDungeon.player.gold < this.adjustCost || AbstractDungeon.player.masterDeck.hasUpgradableCards() == false);
                } else {
                    this.imageEventText.updateDialogOption(0, OPTIONS[1] + this.adjustCost + OPTIONS[6] + OPTIONS[7] + 2 + OPTIONS[8], AbstractDungeon.player.gold < this.adjustCost || AbstractDungeon.player.masterDeck.hasUpgradableCards() == false);
                }
                if (this.cleanUpRemovesCards) {
                    this.imageEventText.setDialogOption(OPTIONS[2] + this.cleanUpCost + OPTIONS[6] + OPTIONS[10], AbstractDungeon.player.gold < this.cleanUpCost || CardGroup.getGroupWithoutBottledCards(AbstractDungeon.player.masterDeck).size() == 0);
                } else {
                    this.imageEventText.setDialogOption(OPTIONS[2] + this.cleanUpCost + OPTIONS[6] + OPTIONS[11] + 2 + OPTIONS[12], AbstractDungeon.player.gold < this.cleanUpCost || CardGroup.getGroupWithoutBottledCards(AbstractDungeon.player.masterDeck).size() < 2);
                }
                this.imageEventText.setDialogOption(OPTIONS[3] + this.fullServiceCost + OPTIONS[6] + OPTIONS[13], AbstractDungeon.player.gold < this.fullServiceCost || CardGroup.getGroupWithoutBottledCards(AbstractDungeon.player.masterDeck).size() == 0);
                this.imageEventText.setDialogOption(OPTIONS[4] + this.hpLoss + OPTIONS[5]);
                this.curScreen = CurrentScreen.MAIN;
                break;
            }
            case MAIN: {
                switch (buttonPressed) {
                    case 0: {
                        this.imageEventText.updateBodyText(DESC[2]);
                        AbstractDungeon.player.loseGold(this.adjustCost);
                        if (this.adjustmentUpgradesOne) {
                            this.option = OptionChosen.UPGRADE;
                            AbstractDungeon.gridSelectScreen.open(AbstractDungeon.player.masterDeck.getUpgradableCards(), 1, OPTIONS[15], true, false, false, false);
                            break;
                        }
                        this.upgradeTwoRandomCards();
                        break;
                    }
                    case 1: {
                        this.imageEventText.updateBodyText(DESC[2]);
                        AbstractDungeon.player.loseGold(this.cleanUpCost);
                        if (this.cleanUpRemovesCards) {
                            this.option = OptionChosen.REMOVE;
                            AbstractDungeon.gridSelectScreen.open(CardGroup.getGroupWithoutBottledCards(AbstractDungeon.player.masterDeck.getPurgeableCards()), 1, OPTIONS[17], false, false, false, true);
                            break;
                        }
                        this.option = OptionChosen.TRANSFORM;
                        AbstractDungeon.gridSelectScreen.open(CardGroup.getGroupWithoutBottledCards(AbstractDungeon.player.masterDeck.getPurgeableCards()), 2, OPTIONS[16], false, false, false, false);
                        break;
                    }
                    case 2: {
                        this.imageEventText.updateBodyText(DESC[2]);
                        AbstractDungeon.player.loseGold(this.fullServiceCost);
                        this.option = OptionChosen.REMOVE_AND_UPGRADE;
                        AbstractDungeon.gridSelectScreen.open(CardGroup.getGroupWithoutBottledCards(AbstractDungeon.player.masterDeck.getPurgeableCards()), 1, OPTIONS[17], false, false, false, true);
                        break;
                    }
                    case 3: {
                        this.imageEventText.loadImage("images/events/designerPunched2.jpg");
                        this.imageEventText.updateBodyText(DESC[3]);
                        Designer.logMetricTakeDamage(ID, "Punched", this.hpLoss);
                        CardCrawlGame.sound.play("BLUNT_FAST");
                        AbstractDungeon.player.damage(new DamageInfo(null, this.hpLoss, DamageInfo.DamageType.HP_LOSS));
                        break;
                    }
                }
                this.imageEventText.updateDialogOption(0, OPTIONS[14]);
                this.imageEventText.clearRemainingOptions();
                this.curScreen = CurrentScreen.DONE;
                break;
            }
            default: {
                this.openMap();
            }
        }
    }

    private void upgradeTwoRandomCards() {
        ArrayList<AbstractCard> upgradableCards = new ArrayList<AbstractCard>();
        for (AbstractCard c : AbstractDungeon.player.masterDeck.group) {
            if (!c.canUpgrade()) continue;
            upgradableCards.add(c);
        }
        Collections.shuffle(upgradableCards, new Random(AbstractDungeon.miscRng.randomLong()));
        if (upgradableCards.isEmpty()) {
            Designer.logMetricLoseGold(ID, "Tried to Upgrade", this.adjustCost);
        } else if (upgradableCards.size() == 1) {
            ((AbstractCard)upgradableCards.get(0)).upgrade();
            AbstractDungeon.player.bottledCardUpgradeCheck((AbstractCard)upgradableCards.get(0));
            AbstractDungeon.topLevelEffects.add(new ShowCardBrieflyEffect(((AbstractCard)upgradableCards.get(0)).makeStatEquivalentCopy()));
            AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
            Designer.logMetricCardUpgradeAtCost(ID, "Tried to Upgrade", (AbstractCard)upgradableCards.get(0), this.adjustCost);
        } else {
            ArrayList<String> cards = new ArrayList<String>();
            cards.add(((AbstractCard)upgradableCards.get((int)0)).cardID);
            cards.add(((AbstractCard)upgradableCards.get((int)1)).cardID);
            Designer.logMetricUpgradeCardsAtCost(ID, "Upgraded Two", cards, this.adjustCost);
            ((AbstractCard)upgradableCards.get(0)).upgrade();
            ((AbstractCard)upgradableCards.get(1)).upgrade();
            AbstractDungeon.player.bottledCardUpgradeCheck((AbstractCard)upgradableCards.get(0));
            AbstractDungeon.player.bottledCardUpgradeCheck((AbstractCard)upgradableCards.get(1));
            AbstractDungeon.topLevelEffects.add(new ShowCardBrieflyEffect(((AbstractCard)upgradableCards.get(0)).makeStatEquivalentCopy(), (float)Settings.WIDTH / 2.0f - AbstractCard.IMG_WIDTH / 2.0f - 20.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f));
            AbstractDungeon.topLevelEffects.add(new ShowCardBrieflyEffect(((AbstractCard)upgradableCards.get(1)).makeStatEquivalentCopy(), (float)Settings.WIDTH / 2.0f + AbstractCard.IMG_WIDTH / 2.0f + 20.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f));
            AbstractDungeon.topLevelEffects.add(new UpgradeShineEffect((float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f));
        }
    }

    private static enum OptionChosen {
        UPGRADE,
        REMOVE,
        REMOVE_AND_UPGRADE,
        TRANSFORM,
        NONE;

    }

    private static enum CurrentScreen {
        INTRO,
        MAIN,
        DONE;

    }
}


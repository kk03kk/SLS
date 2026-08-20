/*
 * Decompiled with CFR 0.152.
 */
package com.megacrit.cardcrawl.rooms;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.actions.unique.IncreaseMaxHpAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.MetallicizePower;
import com.megacrit.cardcrawl.powers.RegenerateMonsterPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rewards.RewardItem;
import com.megacrit.cardcrawl.rooms.MonsterRoom;

public class MonsterRoomElite
extends MonsterRoom {
    public MonsterRoomElite() {
        this.mapSymbol = "E";
        this.mapImg = ImageMaster.MAP_NODE_ELITE;
        this.mapImgOutline = ImageMaster.MAP_NODE_ELITE_OUTLINE;
        this.eliteTrigger = true;
        this.baseRareCardChance = 10;
        this.baseUncommonCardChance = 40;
    }

    @Override
    public void applyEmeraldEliteBuff() {
        if (Settings.isFinalActAvailable && AbstractDungeon.getCurrMapNode().hasEmeraldKey) {
            switch (AbstractDungeon.mapRng.random(0, 3)) {
                case 0: {
                    for (AbstractMonster m : this.monsters.monsters) {
                        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, m, new StrengthPower(m, AbstractDungeon.actNum + 1), AbstractDungeon.actNum + 1));
                    }
                    break;
                }
                case 1: {
                    for (AbstractMonster m : this.monsters.monsters) {
                        AbstractDungeon.actionManager.addToBottom(new IncreaseMaxHpAction(m, 0.25f, true));
                    }
                    break;
                }
                case 2: {
                    for (AbstractMonster m : this.monsters.monsters) {
                        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, m, new MetallicizePower(m, AbstractDungeon.actNum * 2 + 2), AbstractDungeon.actNum * 2 + 2));
                    }
                    break;
                }
                case 3: {
                    for (AbstractMonster m : this.monsters.monsters) {
                        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(m, m, new RegenerateMonsterPower(m, 1 + AbstractDungeon.actNum * 2), 1 + AbstractDungeon.actNum * 2));
                    }
                    break;
                }
            }
        }
    }

    @Override
    public void onPlayerEntry() {
        this.playBGM(null);
        if (this.monsters == null) {
            this.monsters = CardCrawlGame.dungeon.getEliteMonsterForRoomCreation();
            this.monsters.init();
        }
        waitTimer = 0.1f;
    }

    @Override
    public void dropReward() {
        AbstractRelic.RelicTier tier = this.returnRandomRelicTier();
        if (Settings.isEndless && AbstractDungeon.player.hasBlight("MimicInfestation")) {
            AbstractDungeon.player.getBlight("MimicInfestation").flash();
        } else {
            this.addRelicToRewards(tier);
            if (AbstractDungeon.player.hasRelic("Black Star")) {
                this.addNoncampRelicToRewards(this.returnRandomRelicTier());
            }
            this.addEmeraldKey();
        }
    }

    private void addEmeraldKey() {
        if (Settings.isFinalActAvailable && !Settings.hasEmeraldKey && !this.rewards.isEmpty() && AbstractDungeon.getCurrMapNode().hasEmeraldKey) {
            this.rewards.add(new RewardItem((RewardItem)this.rewards.get(this.rewards.size() - 1), RewardItem.RewardType.EMERALD_KEY));
        }
    }

    private AbstractRelic.RelicTier returnRandomRelicTier() {
        int roll = AbstractDungeon.relicRng.random(0, 99);
        if (ModHelper.isModEnabled("Elite Swarm")) {
            roll += 10;
        }
        if (roll < 50) {
            return AbstractRelic.RelicTier.COMMON;
        }
        if (roll > 82) {
            return AbstractRelic.RelicTier.RARE;
        }
        return AbstractRelic.RelicTier.UNCOMMON;
    }

    @Override
    public AbstractCard.CardRarity getCardRarity(int roll) {
        if (ModHelper.isModEnabled("Elite Swarm")) {
            return AbstractCard.CardRarity.RARE;
        }
        return super.getCardRarity(roll);
    }
}


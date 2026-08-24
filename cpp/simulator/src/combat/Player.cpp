//
// Created by gamerpuppy on 7/4/2021.
//

#include <cassert>
#include <algorithm>
#include <climits>

#include <constants/PlayerStatusEffects.h>
#include <combat/BattleContext.h>
#include <combat/Actions.h>
#include <game/Game.h>
#include "combat/Player.h"

using namespace sts;

int Player::getPowerPriority(PlayerStatus s) {
    switch (s) {
        case PS::CONFUSED: return 0;
        case PS::DOUBLE_DAMAGE:
        case PS::PEN_NIB: return 6;
        case PS::FRAIL: return 10;
        case PS::DRAW_CARD_NEXT_TURN: return 20;
        case PS::ESTABLISHMENT:
        case PS::TOOLS_OF_THE_TRADE: return 25;
        case PS::INTANGIBLE: return 75;
        case PS::WEAK: return 99;
        case PS::CONSTRICTED: return 105;
        default: return 5;
    }
}

void Player::recordPowerApplied(PlayerStatus s) {
    if (std::find(powerOrder.begin(), powerOrder.end(), s) != powerOrder.end()) return;
    const int priority = getPowerPriority(s);
    const auto position = std::find_if(powerOrder.begin(), powerOrder.end(), [priority](PlayerStatus existing) {
        return getPowerPriority(existing) > priority;
    });
    powerOrder.insert(position, s);
}

void Player::recordPowerRemoved(PlayerStatus s) {
    powerOrder.erase(std::remove(powerOrder.begin(), powerOrder.end(), s), powerOrder.end());
}

std::vector<std::pair<PlayerStatus, int>> Player::orderedPowers() const {
    std::vector<std::pair<PlayerStatus, int>> result;
    result.reserve(powerOrder.size());
    for (const auto status : powerOrder) {
        if (hasStatusRuntime(status)) result.emplace_back(status, getStatusRuntime(status));
    }
    return result;
}


bool Player::hasRelicRuntime(RelicId r) const {
    if ((int) r < 64) {
        return relicBits0 & (1ULL << (int)r);
    } else {
        return relicBits1 & (1ULL << ((int)r-64));
    }
}

bool Player::hasStatusRuntime(PlayerStatus s) const {
    switch (s) {
        case PS::ARTIFACT:
            return artifact;

        case PS::DEXTERITY:
            return dexterity;

        case PS::FOCUS:
            return focus;

        case PS::STRENGTH:
            return strength;

        default:
            break;
    }

    int idx = static_cast<int>(s);
    if (idx < 64) {
        return statusBits0 & (1ULL << idx);
    } else {
        return statusBits1 & (1ULL << (idx-64));
    }
}

int Player::getStatusRuntime(PlayerStatus s) const {
    switch (s) {
        case PS::ARTIFACT:
            return artifact;
        case PS::DEXTERITY:
            return dexterity;
        case PS::FOCUS:
            return focus;
        case PS::STRENGTH:
            return strength;
        default:
            if (!hasStatusRuntime(s)) return 0;
            const auto found = statusMap.find(s);
            return found == statusMap.end() ? 1 : found->second;
    }
}

void Player::gainBlock(BattleContext &bc, int amount) {
    if (amount <= 0) {
        return;
    }

    block += amount;

    if (hasStatus<PS::JUGGERNAUT>()) {
        bc.addToBot(Actions::DamageRandomEnemy(getStatus<PS::JUGGERNAUT>()));
    }

    // todo watcher weak power
}

void Player::gainGold(BattleContext &bc, int amount) {
#ifdef sts_asserts
    assert(amount > 0);
#endif

    if (hasRelic<R::ECTOPLASM>()) {
        return;
    }

    gold += amount;
    if (hasRelic<R::BLOODY_IDOL>()) {
        heal(5);
    }
}

void Player::useEnergy(int amount) {
#ifdef sts_asserts
    assert(energy >= amount);
#endif

    energy -= amount;
}

void Player::gainEnergy(int amount) {
    energy += amount;
}

void Player::increaseOrbSlots(int amount) {
    orbSlots = static_cast<int8_t>(std::max(
        0, std::min(MAX_ORB_SLOTS, static_cast<int>(orbSlots) + amount)));
}

void Player::channelOrb(BattleContext &bc, Orb orb) {
    if (orb == Orb::EMPTY || orbSlots <= 0) return;
    if (!hasEmptyOrb()) evokeOrb(bc);
    for (int i = 0; i < orbSlots; ++i) {
        if (orbs[i] == Orb::EMPTY) {
            orbs[i] = orb;
            orbEvokeAmounts[i] = orb == Orb::DARK ? std::max(0, 6 + focus) : 0;
            return;
        }
    }
}

void Player::evokeOrb(BattleContext &bc) {
    if (orbSlots <= 0 || orbs[0] == Orb::EMPTY) return;
    switch (orbs[0]) {
        case Orb::LIGHTNING:
            bc.addToBot(Actions::DamageRandomEnemy(std::max(0, 8 + focus)));
            break;
        case Orb::FROST:
            bc.addToBot(Actions::GainBlock(std::max(0, 5 + focus)));
            break;
        case Orb::DARK: {
            int target = -1;
            int lowestHp = INT_MAX;
            for (int i = 0; i < bc.monsters.monsterCount; ++i) {
                const auto &monster = bc.monsters.arr[i];
                if (!monster.isDeadOrEscaped() && monster.curHp < lowestHp) {
                    lowestHp = monster.curHp;
                    target = i;
                }
            }
            if (target >= 0) bc.addToBot(Actions::DamageEnemy(target, orbEvokeAmounts[0]));
            break;
        }
        case Orb::FUSION:
            bc.addToBot(Actions::GainEnergy(2));
            break;
        default:
            break;
    }
    for (int i = 1; i < orbSlots; ++i) {
        orbs[i - 1] = orbs[i];
        orbEvokeAmounts[i - 1] = orbEvokeAmounts[i];
    }
    orbs[orbSlots - 1] = Orb::EMPTY;
    orbEvokeAmounts[orbSlots - 1] = 0;
}

void Player::triggerStartOfTurnOrb(BattleContext &bc, int idx) {
    if (idx >= 0 && idx < orbSlots && orbs[idx] == Orb::FUSION) {
        bc.addToBot(Actions::GainEnergy(1));
    }
}

void Player::triggerEndOfTurnOrb(BattleContext &bc, int idx) {
    if (idx < 0 || idx >= orbSlots) return;
    switch (orbs[idx]) {
            case Orb::LIGHTNING:
                bc.addToBot(Actions::DamageRandomEnemy(std::max(0, 3 + focus)));
                break;
            case Orb::FROST:
                bc.addToBot(Actions::GainBlock(std::max(0, 2 + focus)));
                break;
            case Orb::DARK:
                orbEvokeAmounts[idx] += std::max(0, 6 + focus);
                break;
            case Orb::FUSION:
                break;
            default:
                break;
    }
}

void Player::triggerEndOfTurnOrbs(BattleContext &bc) {
    for (int i = 0; i < orbSlots; ++i) {
        triggerEndOfTurnOrb(bc, i);
    }
    if (orbSlots > 0 && orbs[0] != Orb::EMPTY &&
            hasRelic<R::GOLD_PLATED_CABLES>()) {
        triggerEndOfTurnOrb(bc, 0);
    }
}

void Player::adjustOrbFocus(int amount) {
    if (amount == 0) return;
    for (int i = 0; i < orbSlots; ++i) {
        if (orbs[i] == Orb::DARK) {
            orbEvokeAmounts[i] = std::max(0, static_cast<int>(orbEvokeAmounts[i]) + amount);
        }
    }
}

bool Player::hasEmptyOrb() const {
    for (int i = 0; i < orbSlots; ++i) {
        if (orbs[i] == Orb::EMPTY) return true;
    }
    return false;
}

void Player::removeDebuffs() {
    if (getStatus<PS::STRENGTH>() < 0) {
        setStatusValueNoChecks<PS::STRENGTH>(0);
    }
    if (getStatus<PS::DEXTERITY>() < 0) {
        setStatusValueNoChecks<PS::DEXTERITY>(0);
    }

    removeStatus<PS::BIAS>();
    removeStatus<PS::CONFUSED>();
    removeStatus<PS::CONSTRICTED>();

    if (hasStatus<PS::DRAW_REDUCTION>()) {
        ++cardDrawPerTurn;
        removeStatus<PS::DRAW_REDUCTION>();
    }

    removeStatus<PS::ENTANGLED>();
    removeStatus<PS::FASTING>();
    removeStatus<PS::FRAIL>();
    removeStatus<PS::HEX>();
    removeStatus<PS::LOSE_DEXTERITY>();
    removeStatus<PS::LOSE_STRENGTH>();
    removeStatus<PS::NO_BLOCK>();
    removeStatus<PS::NO_DRAW>();
    removeStatus<PS::VULNERABLE>();
    removeStatus<PS::WEAK>();
    removeStatus<PS::WRAITH_FORM>();
}

void Player::increaseMaxHp(int amount) {
    maxHp += amount;
    heal(amount);
}

void Player::heal(int amount) {
    if (hasRelic<RelicId::MARK_OF_THE_BLOOM>()) {
        return;
    }

    if (hasRelic<RelicId::MAGIC_FLOWER>()) {
        amount = amount * 3 / 2;
    }

    bool wasBloodied = curHp <= maxHp/2;

    curHp = std::min(static_cast<int>(maxHp), curHp + amount);

    if (wasBloodied && curHp > maxHp/2 && hasRelic<RelicId::RED_SKULL>()) {
        debuff<PS::STRENGTH>(3);
    }
}

void Player::damage(BattleContext &bc, const int calculatedDamage, const bool selfDamage) {
#ifdef sts_asserts
    assert(calculatedDamage >= 0);
#endif

    int damage = calculatedDamage;

    if (damage > 1 && hasStatus<PS::INTANGIBLE>()) {
        damage = 1;
    }

    const auto savedBlock = block;
    block = std::max(0, block-damage);
    damage -= savedBlock;



    if (damage > 0) {
        block = 0;
    }

    if (damage > 0 && hasStatus<PS::BUFFER>()) {
        decrementStatus<PS::BUFFER>();
        damage = 0;
    }

    if (damage > 0 && hasRelic<RelicId::TUNGSTEN_ROD>()) {
        damage -= 1;
    }

    if (damage > 0) {
        hpWasLost(bc, damage, selfDamage);
    }
}

void Player::attacked(BattleContext &bc, int enemyIdx, int calculatedDamage) {
    assert(calculatedDamage >= 0);

    // assume intangible is already handled

    int damage = calculatedDamage;
    const auto savedBlock = block;
    block = std::max(0, block-damage);
    damage -= savedBlock;

    // Buffer's onAttackedToChangeDamage hook runs before onAttacked relics.
    if (damage > 0 && hasStatus<PS::BUFFER>()) {
        decrementStatus<PS::BUFFER>();
        damage = 0;
    }

    if (hasStatus<PS::THORNS>()) {
        bc.addToTop( Actions::DamageEnemy(enemyIdx, getStatus<PS::THORNS>()) );
    }

    if (hasStatus<PS::FLAME_BARRIER>()) {
        bc.addToTop( Actions::DamageEnemy(enemyIdx, getStatus<PS::FLAME_BARRIER>()) );
    }

    if (damage > 1 && damage <= 5 && hasRelic<RelicId::TORII>()) {
        damage = 1;
    }

    if (damage > 0 && hasRelic<RelicId::TUNGSTEN_ROD>()) {
        damage -= 1;
    }

    if (damage > 0) {
        lastAttackUnblockedDamage = damage;

        if (hasStatus<PS::PLATED_ARMOR>()) {
            decrementStatus<PS::PLATED_ARMOR>();
        }

        if (bc.monsters.arr[enemyIdx].hasStatus<MS::PAINFUL_STABS>()) {
            bc.addToBot( Actions::MakeTempCardInDiscard({CardId::WOUND}) );
        }

        hpWasLost(bc, damage, false);

    } else {
        lastAttackUnblockedDamage = 0;
    }
}

void Player::loseHp(BattleContext &bc, int amount, bool selfDamage) {
    if (amount > 1 && hasStatus<PS::INTANGIBLE>()) {
        amount = 1;
    }

    // LoseHPAction calls AbstractCreature.damage with HP_LOSS in the original.
    // HP_LOSS bypasses block, but Buffer still receives the damage-change hook.
    if (amount > 0 && hasStatus<PS::BUFFER>()) {
        decrementStatus<PS::BUFFER>();
        return;
    }

    if (amount > 0 && hasRelic<RelicId::TUNGSTEN_ROD>()) {
        amount -= 1;
        if (amount == 0) {
            return;
        }
    }

    hpWasLost(bc, amount, selfDamage);
}

void Player::hpWasLost(BattleContext &bc, int amount, bool selfDamage) {
    assert(amount > 0);

    bool wasBloodied = curHp <= maxHp/2;

    curHp = std::max(0, curHp-amount);

    if (selfDamage && hasStatus<PS::RUPTURE>()) {
        // RupturePower.wasHPLost queues ApplyPowerAction on top.  This must
        // not be applied inline: the next queued action can observe the
        // difference, and terminal combat must not retain a phantom buff.
        bc.addToTop(Actions::BuffPlayer<PS::STRENGTH>(getStatus<PS::RUPTURE>()));
    }

    // todo - does order acquired matter with centennial/runic?
    // relics wasHpLost
    // -centennial-puzzle
    // -emotion chip
    // -runic cube
    // -self forming clay

    if (hasRelic<RelicId::CENTENNIAL_PUZZLE>()) {
        setHasRelic<RelicId::CENTENNIAL_PUZZLE>(false);
        bc.addToTop( Actions::DrawCards(3) );
    }

    if (hasRelic<RelicId::EMOTION_CHIP>()) {
        // todo
    }

    if (hasRelic<RelicId::SELF_FORMING_CLAY>()) {
        // SelfFormingClay queues ApplyPowerAction on top of the current
        // damage action.  Applying it synchronously makes the next-turn
        // block visible even if this hit has already ended the battle.
        bc.addToTop(Actions::BuffPlayer<PS::NEXT_TURN_BLOCK>(3));
    }

    if (hasRelic<RelicId::RUNIC_CUBE>()) {
        bc.addToTop( Actions::DrawCards(1) );
    }

    if (hasRelic<RelicId::RED_SKULL>() && !wasBloodied && curHp <= maxHp/2) {
        // RedSkull.onBloodied queues ApplyPowerAction.  In particular, a
        // terminal hit may prevent that action from applying; doing it inline
        // leaves an impossible +3 Strength in the retained terminal state.
        bc.addToTop(Actions::BuffPlayer<PS::STRENGTH>(3));
    }

    bc.cards.onTookDamage();
    ++timesDamagedThisCombat;

    if (curHp <= 0) {
        wouldDie(bc);
    }
}

void Player::wouldDie(BattleContext &bc) {
    // assume fairy and lizard tail heal for greater than zero - max hp is not less than ~8
    curHp = 0;
    if  (!hasRelic<RelicId::MARK_OF_THE_BLOOM>()) {
        for (int i = 0; i < bc.potionCapacity; i++) {
            if (bc.potions[i] == Potion::FAIRY_POTION) {
                bc.discardPotion(i);
                const int healAmount = std::max(
                        1,
                        static_cast<int>(static_cast<float>(maxHp) * (hasRelic<R::SACRED_BARK>() ? 0.6f : 0.3f))
                );
                heal(healAmount);
                return;
            }
        }

        if (hasRelic<RelicId::LIZARD_TAIL>()) {
            setHasRelic<RelicId::LIZARD_TAIL>(false);
            heal(maxHp/2);
            return;
        }
    }

    bc.outcome = Outcome::PLAYER_LOSS;
}

void Player::applyEndOfTurnPowers(BattleContext &bc) {
    if (bomb1) {
        bc.addToBot( Actions::DamageAllEnemy(bomb1) );
    }
    bomb1 = bomb2;
    bomb2 = bomb3;
    bomb3 = 0;

    for (auto pair : orderedPowers()) {
        if (!hasStatusRuntime(pair.first)) {
            continue;
        }

        switch (pair.first) {
            case PS::BURST:
                bc.addToBot(Actions::RemoveStatus<PS::BURST>());
                break;

            case PS::COMBUST:
                if (!bc.monsters.areMonstersBasicallyDead()) {
                    bc.addToBot(Actions::PlayerLoseHp(combustHpLoss, true));
                    bc.addToBot(Actions::DamageAllEnemy(pair.second));
                }
                break;

            case PS::CONSTRICTED:
                bc.addToBot(Actions::DamagePlayer(pair.second));
                break;

            case PS::DOUBLE_TAP:
                bc.addToBot(Actions::RemoveStatus<PS::DOUBLE_TAP>());
                break;

            case PS::ENTANGLED:
                bc.addToBot(Actions::RemoveStatus<PS::ENTANGLED>());
                break;

            case PS::EQUILIBRIUM:
                for (int i = 0; i < bc.cards.cardsInHand; ++i) {
                    if (!bc.cards.hand[i].isEthereal()) {
                        bc.cards.hand[i].retain = true;
                    }
                }
                break;

            case PS::ESTABLISHMENT:
                bc.addToBot({[amount = pair.second](BattleContext &bc) {
                    for (int i = 0; i < bc.cards.cardsInHand; ++i) {
                        auto &card = bc.cards.hand[i];
                        if (card.hasSelfRetain() || card.retain) {
                            card.updateCost(-amount);
                        }
                    }
                }});
                break;

            case PS::LOSE_DEXTERITY:
                bc.addToBot(Actions::DebuffPlayer<PS::DEXTERITY>(-pair.second));
                bc.addToBot(Actions::RemoveStatus<PS::LOSE_DEXTERITY>());
                break;

            case PS::LOSE_STRENGTH:
                bc.addToBot(Actions::DebuffPlayer<PS::STRENGTH>(-pair.second));
                bc.addToBot(Actions::RemoveStatus<PS::LOSE_STRENGTH>());
                break;

            case PS::NO_DRAW:
                bc.addToBot(Actions::RemoveStatus<PS::NO_DRAW>());
                break;

            case PS::OMEGA:
                bc.addToBot(Actions::DamageAllEnemy(pair.second));
                break;

            case PS::RAGE:
                removeStatus<PS::RAGE>();
                break;

            case PS::REBOUND:
                bc.addToBot(Actions::RemoveStatus<PS::REBOUND>());
                break;

            case PS::REGEN:
                bc.addToTop(Actions::HealPlayer(pair.second));
                bc.addToTop(Actions::DecrementStatus<PS::REGEN>());
                break;

                //case RetainCardPower -> if not has relic runic pyramid and not has power equilibrium, addToBot retain cards action

            case PS::RITUAL:
                bc.addToBot(Actions::BuffPlayer<PS::STRENGTH>(pair.second));
                break;

            case PS::RETAIN_CARDS:
                bc.addToBot(Actions::RetainCardsAction(pair.second));
                break;
                // case TheBomb

            case PS::WRAITH_FORM: // todo does this debuff or just decrement?
                bc.addToBot(Actions::DecrementStatus<PS::DEXTERITY>(pair.second));
                break;

            default:
                break;
        }
    }
}

void Player::applyAtEndOfRoundPowers(BattleContext &bc) {
    for (const auto [status, amount] : orderedPowers()) {
        if (!hasStatusRuntime(status)) continue;
        switch (status) {
            case PS::FRAIL: decrementIfNotJustApplied<PS::FRAIL>(); break;
            case PS::VULNERABLE: decrementIfNotJustApplied<PS::VULNERABLE>(); break;
            case PS::WEAK: decrementIfNotJustApplied<PS::WEAK>(); break;
            case PS::DOUBLE_DAMAGE: decrementIfNotJustApplied<PS::DOUBLE_DAMAGE>(); break;
            case PS::DRAW_REDUCTION:
                if (wasJustApplied<PS::DRAW_REDUCTION>()) {
                    setJustApplied<PS::DRAW_REDUCTION>(false);
                } else {
                    // The original queues ReducePowerAction. The next turn's
                    // DrawCardAction therefore captures the reduced hand size
                    // before onRemove restores gameHandSize. Do not apply the
                    // removal synchronously here or Snecko Eye draws one extra
                    // card (and consumes one extra cardRandomRng value).
                    bc.addToBot({[](BattleContext &bc) {
                        if (bc.player.hasStatus<PS::DRAW_REDUCTION>()) {
                            bc.player.removeStatus<PS::DRAW_REDUCTION>();
                            ++bc.player.cardDrawPerTurn;
                        }
                    }});
                }
                break;
            case PS::DUPLICATION: decrementStatus<PS::DUPLICATION>(); break;
            case PS::EQUILIBRIUM: decrementStatus<PS::EQUILIBRIUM>(); break;
            case PS::INTANGIBLE: decrementStatus<PS::INTANGIBLE>(); break;
            case PS::NO_BLOCK: decrementStatus<PS::NO_BLOCK>(); break;
            case PS::WAVE_OF_THE_HAND: decrementStatus<PS::WAVE_OF_THE_HAND>(); break;
            default:
                break;
        }
    }
}

void Player::applyStartOfTurnRelics(BattleContext &bc) {
    //****** Player relics atTurnStart ******
    if (hasRelic<R::ART_OF_WAR>()) {
        if (attacksPlayedThisTurn == 0) {
            bc.addToBot(Actions::GainEnergy(1));
        }
    }

    if (hasRelic<R::BRIMSTONE>()) {
        buff<PS::STRENGTH>(2);
        for (int i = 0; i < bc.monsters.monsterCount; i++) {
            if (bc.monsters.arr[i].isTargetable()) {
                bc.monsters.arr[i].buff<MS::STRENGTH>(1);
            }
        }
    }

    if (hasRelic<R::CAPTAINS_WHEEL>()) {
        if (bc.turn == 2) {
            bc.addToBot( Actions::GainBlock(18) );
        }
    }

    if (hasRelic<R::DAMARU>()) {
        bc.addToBot( Actions::BuffPlayer<PS::MANTRA>(1) );
        // todo handle mantra change stance
    }

    if (hasRelic<R::EMOTION_CHIP>()) {
        // todo if lost hp last turn addToBot(new ImpulseAction())
    }

    if (hasRelic<R::HAPPY_FLOWER>()) {
        if (++happyFlowerCounter == 3) {
            happyFlowerCounter = 0;
            bc.addToBot( Actions::GainEnergy(1) );
        }
    }

    if (hasRelic<R::HORN_CLEAT>()) {
        if (bc.turn == 1) {
            bc.addToBot( Actions::GainBlock(14) );
        }
    }

    if (hasRelic<R::INCENSE_BURNER>()) {
        if (++incenseBurnerCounter == 6) {
            incenseBurnerCounter = 0;
            bc.addToBot( Actions::BuffPlayer<PS::INTANGIBLE>(1) );
        }
    }

    if (hasRelic<R::INSERTER>()) {
        if (++inserterCounter == 2) {
            inserterCounter = 0; // todo
            bc.addToBot( {[=](BattleContext &bc) {
                bc.player.increaseOrbSlots(1);
            }});
        }
    }

    if (hasRelic<R::MERCURY_HOURGLASS>()) {
        bc.addToBot( Actions::DamageAllEnemy(3) );
    }

    if (hasRelic<R::NECRONOMICON>()) {
        haveUsedNecronomiconThisTurn = false;
    }

    if (hasRelic<R::ORANGE_PELLETS>()) {
        orangePelletsCardTypesPlayed.reset();
    }

}

void Player::applyStartOfTurnPowers(BattleContext &bc) {
    // ****** Player powers atStartOfTurn ******
    for (auto pair : orderedPowers()) {
        if (!hasStatusRuntime(pair.first)) {
            continue;
        }

        switch (pair.first) {
            case PS::BERSERK:
                bc.addToBot(Actions::GainEnergy(pair.second));
                break;

            case PS::BATTLE_HYMN:
                bc.addToBot(Actions::MakeTempCardInHand(CardId::SMITE, hasStatus<PS::MASTER_REALITY>(), pair.second) );
                break;

            case PS::BIAS:
                bc.addToBot( Actions::DecrementStatus<PS::FOCUS>(pair.second) );
                break;

            case PS::CREATIVE_AI:
//                bc.addToBot( Actions::SetState(InputState::CREATE_RANDOM_CARD_IN_HAND_POWER, pair.second) ); // todo
                break;

            case PS::ECHO_FORM:
                echoFormCardsDoubled = 0;
                break;

            case PS::BLASPHEMER:
                removeStatus<PS::BLASPHEMER>();
                bc.addToBot( Actions::PlayerLoseHp(9999) );
                break;

            case PS::FASTING:
                //  add energy dont need? just change energy per turn instead
                break;

            case PS::FORESIGHT:
                if (bc.cards.drawPile.empty()) {
                    bc.addToTop( Actions::SetState(InputState::SHUFFLE_DISCARD_TO_DRAW) );
                }
//                bc.addToBot( Actions::SetState(InputState::SCRY, pair.second) ); // tood
                break;

            case PS::FLAME_BARRIER:
                removeStatus<PS::FLAME_BARRIER>();
                break;

            case PS::HELLO_WORLD: // todo
                removeStatus<PS::HELLO_WORLD>();
                break;

            case PS::INFINITE_BLADES:
                bc.addToBot(Actions::MakeTempCardInHand(CardId::SHIV, hasStatus<PS::MASTER_REALITY>(), pair.second) );
                break;

            case PS::LOOP:
                if (orbSlots > 0 && orbs[0] != Orb::EMPTY) {
                    for (int i = 0; i < pair.second; ++i) {
                        triggerStartOfTurnOrb(bc, 0);
                        triggerEndOfTurnOrb(bc, 0);
                    }
                }
                break;

            case PS::MAGNETISM:
                // MagnetismPower creates each card when its start-of-turn
                // callback runs, using cardRandomRng, then queues the hand
                // insertion.  Deferring the random selection would shift
                // RNG relative to other start-of-turn powers.
                for (int i = 0; i < pair.second; ++i) {
                    const auto card = sts::getTrulyRandomColorlessCardInCombat(
                        bc.cardRandomRng
                    );
                    bc.addToBot(Actions::MakeTempCardInHand(card));
                }
                break;

            case PS::MAYHEM:
                for (int i = 0; i < pair.second; i++) {
                    bc.addToBot(Actions::PlayTopCardRandomTarget(false));
                }
                break;

            case PS::NEXT_TURN_BLOCK:
                bc.addToBot( Actions::GainBlock(pair.second) );
                removeStatus<PS::NEXT_TURN_BLOCK>();
                break;

                // todo  case NightMarePower: this.bc.addToBot(new MakeTempCardInHandAction(this.card, this.amount)); bc.addToBot remove power

            case PS::PANACHE:
                panacheCounter = 5;
                break;

            case PS::PHANTASMAL:
                decrementStatus<PS::PHANTASMAL>();
                bc.addToBot( Actions::BuffPlayer<PS::DOUBLE_DAMAGE>() );
                break;

                // time maze not used in standard modes

            case PS::WRATH_NEXT_TURN:
                removeStatus<PS::WRATH_NEXT_TURN>();
                bc.addToBot( Actions::ChangeStance(Stance::WRATH) );
                break;

            default:
                break;
        }

    }
}

void Player::applyStartOfTurnPostDrawRelics(BattleContext &bc) {
    // ****** Player Relics AtTurnStartPostDraw ******
    if (hasRelic<R::POCKETWATCH>()) {
        if (cardsPlayedThisTurn <= 3) {
            bc.addToBot(Actions::DrawCards(3));
        }
    }

    if (hasRelic<R::WARPED_TONGS>()) {
        bc.addToBot(Actions::UpgradeRandomCardAction());
    }
}

void Player::applyStartOfTurnPostDrawPowers(BattleContext &bc) {
    // ****** Player Powers AtStartOfTurnPostDraw ******
    for (auto pair : orderedPowers()) {
        if (!hasStatusRuntime(pair.first)) {
            continue;
        }

        switch (pair.first) {
            case PS::BRUTALITY:
                bc.addToBot( Actions::DrawCards(pair.second) );
                // Brutality's LoseHPAction has the player as both source and
                // target.  That ownership matters: Rupture reacts to this
                // HP loss exactly as it does to Hemokinesis/Bloodletting.
                bc.addToBot( Actions::PlayerLoseHp(pair.second, true) );
                break;

            case PS::DEMON_FORM:
                bc.addToBot( Actions::BuffPlayer<PS::STRENGTH>(pair.second) );
                break;

            case PS::DEVOTION: // the implementation of this is really weird in the game code
                bc.addToBot( Actions::BuffPlayer<PS::MANTRA>(pair.second) ); // todo make buffing mantra switch stance
                break;

            case PS::DRAW_CARD_NEXT_TURN:
                bc.addToBot( Actions::DrawCards(pair.second) );
                removeStatus<PS::DRAW_CARD_NEXT_TURN>();
                break;

            case PS::NOXIOUS_FUMES:
                bc.addToBot( Actions::DebuffAllEnemy<MS::POISON>(pair.second) );
                break;

            case PS::TOOLS_OF_THE_TRADE:
                bc.addToBot( Actions::DrawCards(pair.second) );
//                bc.addToBot( Actions::SetState(InputState::CHOOSE_DISCARD_CARDS, pair.second) );
                break;

            default:
                break;
        }
    }
}

void Player::rechargeEnergy(BattleContext &bc) {
    if (hasRelic<R::ICE_CREAM>()) {
        gainEnergy(energyPerTurn);
    } else {
        energy = energyPerTurn;
    }

    // ****** Player powers onEnergyRecharge ******
    if (hasStatus<PS::COLLECT>()) {
        decrementStatus<PS::COLLECT>();
        bc.addToBot(Actions::MakeTempCardInHand(CardId::MIRACLE) );
    }

    if (hasStatus<PS::DEVA>()) {
        gainEnergy(devaFormEnergyPerTurn);
        devaFormEnergyPerTurn += getStatus<PS::DEVA>();
    }

    if (hasStatus<PS::ENERGIZED>()) {
        gainEnergy(getStatus<PS::ENERGIZED>());
        removeStatus<PS::ENERGIZED>();
    }
}

std::ostream& printIfHaveStatus(const Player &p, std::ostream &os, PlayerStatus s) {
    if (!p.hasStatusRuntime(s)){
        return os;
    }

    std::string desc = playerStatusStrings[(int)s];
    return os << "(" << desc << "," << p.getStatusRuntime(s) << ")" << ", ";
}

namespace sts {

//    void printRelicCounters(std::ostream &os, const Player &p) {
//        const std::string s = " ";
//
//        if (p.hasRelic<R::INK_BOTTLE>()) {
//            os << s << "inkBottleCounter: " << p.inkBottleCounter;
//        }
//
//        if (p.hasRelic<R::INSERTER>()) {
//            os << "inserterCounter: " << p.inserterCounter << " ";
//        }
//
//        if (p.hasRelic<R::NUNCHAKU>()) {
//            os << "nunchakuCounter: " << p.nunchakuCounter << " ";
//        }
//
//        if (p.hasRelic<R::LETTER_OPENER>()) {
//            os << "skillsPlayedThisTurn: " << p.skillsPlayedThisTurn << " ";
//        }
//
//    }
//
//    void printStatusCounters(std::ostream &os, const Player &p) {
//
//        if (p.hasStatusInternal<PS::COMBUST>()) {
//            os << "combustHpLoss: " << p.combustHpLoss << " ";
//        }
//
//        if (p.hasStatusInternal<PS::DEVA>()) {
//            os << "devaFormEnergyPerTurn: " << p.devaFormEnergyPerTurn << " ";
//        }
//
//        if (p.hasStatusInternal<PS::ECHO_FORM>()) {
//            os << "panacheCounter: " << p.panacheCounter << " ";
//        }
//
//
//        if (p.hasStatusInternal<PS::PANACHE>()) {
//            os << "panacheCounter: " << p.panacheCounter << " ";
//        }
//    }

    void printRelics(std::ostream &os, const Player &p) {
        os << "\t" << "Relics: ";

        // this is slow and stupid
        int i = static_cast<int>(R::AKABEKO);
        while (i <= static_cast<int>(R::WRIST_BLADE)) {
            auto r = static_cast<RelicId>(i);

            if (p.hasRelicRuntime(r)) {
                os << getRelicName(r) << ", ";
            }

            ++i;
        }
        os << "\n";
    }

    void printAllInfos(std::ostream &os, const Player &p) {
        const std::string s = ", ";

        os << "\t" << "Relic Counters: "
            << "" << "happyFlowerCounter: " << static_cast<int>(p.happyFlowerCounter)
            << s << "incenseBurnerCounter: " << static_cast<int>(p.incenseBurnerCounter)
            << s << "inkBottleCounter: " << static_cast<int>(p.inkBottleCounter)
            << s << "inserterCounter: " << static_cast<int>(p.inserterCounter)
            << s << "nunchakuCounter: " << static_cast<int>(p.nunchakuCounter)
            << s << "penNibCounter: " << static_cast<int>(p.penNibCounter)
            << s << "sundialCounter: " << static_cast<int>(p.sundialCounter)

            << "\n\t" << "Status Counters: "
            << "" << "combustHpLoss: " << static_cast<int>(p.combustHpLoss)
            << s << "devaFormEnergyPerTurn: " << static_cast<int>(p.devaFormEnergyPerTurn)
            << s << "echoFormCardsDoubled: " << static_cast<int>(p.echoFormCardsDoubled)
            << s << "panacheCounter: " << static_cast<int>(p.panacheCounter)
            << s << "TheBomb: { 1=" << static_cast<int>(p.bomb1)
                << ", 2=" << static_cast<int>(p.bomb2)
                << ", 3=" << static_cast<int>(p.bomb3)
                << " }"

            << "\n\t" << "Misc: "
            << "" << "cardsPlayedThisTurn: " << static_cast<int>(p.cardsPlayedThisTurn)
            << s << "attacksPlayedThisTurn: " << static_cast<int>(p.attacksPlayedThisTurn)
            << s << "skillsPlayedThisTurn: " << static_cast<int>(p.skillsPlayedThisTurn);


            os << s << "orangePelletsCardTypesPlayed: {"
                << (p.orangePelletsCardTypesPlayed.test(0) ? "Attack," : "")
                << (p.orangePelletsCardTypesPlayed.test(1) ? "Skill," : "")
                << (p.orangePelletsCardTypesPlayed.test(2) ? "Power," : "")
                << "}";

            os << s << "cardsDiscardedThisTurn: " << static_cast<int>(p.cardsDiscardedThisTurn);
            os << s << "gold: " << static_cast<int>(p.gold) << '\n';
    }


    void printStatusEffects(std::ostream &os, const Player &p) {
        os << "\tStatusEffects: {";
        if (p.hasStatus<PS::CONFUSED>()) {
            os << "(Confused), ";
        }
        if (p.hasStatus<PS::CORRUPTION>()) {
            os << "(Corruption), ";
        }
        if (p.hasStatus<PS::BARRICADE>()) {
            os << "(Barricade), ";
        }

        printIfHaveStatus(p, os, PS::ARTIFACT);
        printIfHaveStatus(p, os, PS::DEXTERITY);
        printIfHaveStatus(p, os, PS::FOCUS);
        printIfHaveStatus(p, os, PS::STRENGTH);
        for (auto pair : p.statusMap) {
            printIfHaveStatus(p, os, pair.first);
        }
        os << "}\n";
    }

    std::ostream& operator<<(std::ostream &os, const Player &p) {
        os << "Player: {\n";

        os << "\t" << "hp:(" << p.curHp << "/" << p.maxHp << ")"
            << " energy:(" << p.energy << "/" << static_cast<int>(p.energyPerTurn)
        << ") block:(" << p.block << ")\n";

        printStatusEffects(os, p);
        printRelics(os, p);
        printAllInfos(os, p);

        os << "}\n";
        return os;
    }



}













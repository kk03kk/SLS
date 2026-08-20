//
// Created by gamerpuppy on 7/5/2021.
//

#include "combat/Monster.h"

#include <algorithm>

#include "game/Random.h"
#include "combat/BattleContext.h"
#include "game/GameContext.h"

using namespace sts;

int Monster::getPowerPriority(MonsterStatus s) {
    switch (s) {
        case MS::FLIGHT:
        case MS::REACTIVE: return 50;
        case MS::INTANGIBLE: return 75;
        case MS::INVINCIBLE:
        case MS::WEAK: return 99;
        default: return 5;
    }
}

void Monster::recordPowerApplied(MonsterStatus s) {
    if (std::find(powerOrder.begin(), powerOrder.end(), s) != powerOrder.end()) return;
    const int priority = getPowerPriority(s);
    const auto position = std::find_if(powerOrder.begin(), powerOrder.end(), [priority](MonsterStatus existing) {
        return getPowerPriority(existing) > priority;
    });
    powerOrder.insert(position, s);
}

void Monster::recordPowerRemoved(MonsterStatus s) {
    powerOrder.erase(std::remove(powerOrder.begin(), powerOrder.end(), s), powerOrder.end());
}

std::vector<std::pair<MonsterStatus, int>> Monster::orderedPowers() const {
    std::vector<std::pair<MonsterStatus, int>> result;
    result.reserve(powerOrder.size());
    for (const auto status : powerOrder) {
        if (hasStatusInternal(status)) result.emplace_back(status, getStatusInternal(status));
    }
    return result;
}

void Monster::setRandomHp(Random &hpRng, bool higherHp) {
    auto hpRange = monsterHpRange[static_cast<int>(id)][higherHp ? 1 : 0];
    curHp = hpRng.random(hpRange[0], hpRange[1]);
    maxHp = curHp;
}

void Monster::applyStartOfTurnPowers(BattleContext &bc) {
    if (!hasStatus<MS::BARRICADE>()) {
        block = 0;
    }

    for (const auto [status, amount] : orderedPowers()) {
        if (!hasStatusInternal(status)) continue;
        switch (status) {
            case MS::CHOKED:
                setHasStatus<MS::CHOKED>(false);
                break;
            case MS::FLIGHT:
                setStatus<MS::FLIGHT>(bc.ascension >= 17 ? 4 : 3);
                break;
            case MS::INVINCIBLE:
                setStatus<MS::INVINCIBLE>(bc.ascension >= 19 ? 200 : 300);
                break;
            case MS::POISON:
                if (!bc.monsters.areMonstersBasicallyDead()) {
                    bc.addToBot(Actions::PoisonLoseHpAction());
                }
                break;
            default:
                break;
        }
    }
}

void Monster::applyEndOfTurnTriggers(BattleContext &bc) {
    // Monster Powers atEndOfTurnPreEndTurnCards and atEndOfTurn
    for (const auto [status, amount] : orderedPowers()) {
        if (!hasStatusInternal(status)) continue;
        switch (status) {
            case MS::METALLICIZE: addBlock(amount); break;
            case MS::MALLEABLE: setStatus<MS::MALLEABLE>(3); break;
            case MS::PLATED_ARMOR: addBlock(amount); break;
            case MS::INTANGIBLE: decrementStatus<MS::INTANGIBLE>(); break;
            case MS::REGEN: heal(amount); break;
            case MS::SHACKLED:
                buff<MS::STRENGTH>(amount);
                removeStatus<MS::SHACKLED>();
                break;
            default:
                break;
        }
    }
}

void Monster::applyEndOfRoundPowers(BattleContext &bc) {
    // Monster Powers atEndOfRound
    for (const auto [status, amount] : orderedPowers()) {
        if (!hasStatusInternal(status)) continue;
        switch (status) {
            case MS::RITUAL:
                if (wasJustApplied<MS::RITUAL>()) setJustApplied<MS::RITUAL>(false);
                else buff<MS::STRENGTH>(amount);
                break;
            case MS::SLOW:
                setStatus<MS::SLOW>(0);
                // SlowPower remains installed with amount zero between turns.
                setHasStatus<MS::SLOW>(true);
                break;
            case MS::LOCK_ON: decrementStatus<MS::LOCK_ON>(); break;
            case MS::WEAK:
                if (wasJustApplied<MS::WEAK>()) setJustApplied<MS::WEAK>(false);
                else decrementStatus<MS::WEAK>();
                break;
            case MS::VULNERABLE:
                if (wasJustApplied<MS::VULNERABLE>()) setJustApplied<MS::VULNERABLE>(false);
                else decrementStatus<MS::VULNERABLE>();
                break;
            case MS::GENERIC_STRENGTH_UP: buff<MS::STRENGTH>(amount); break;
            default:
                break;
        }
    }
}

void Monster::construct(BattleContext &bc, MonsterId monsterId, int monsterIdx) {
    this->id = monsterId;
    this->idx = monsterIdx;
    initHp(bc.monsterHpRng, bc.ascension);

    switch (id) {
        case MonsterId::GREEN_LOUSE:
        case MonsterId::RED_LOUSE:
            if (bc.ascension >= 2) {
                miscInfo = bc.monsterHpRng.random(6, 8);
            } else {
                miscInfo = bc.monsterHpRng.random(5, 7);
            }
            break;

        case MonsterId::DARKLING:
            if (bc.ascension >= 2) {
                miscInfo = bc.monsterHpRng.random(9, 13);
            } else {
                miscInfo = bc.monsterHpRng.random(7, 11);
            }
            break;

        default:
            break;
    }
}

const char *Monster::getName() const {
    return monsterIdStrings[static_cast<int>(id)];
}

bool Monster::hasStatusInternal(MonsterStatus s) const {
    if (s == MS::STRENGTH) return strength != 0;
    return statusBits & (1ULL << (int)s);
}

int Monster::getStatusInternal(MonsterStatus s) const {
    if (s == MS::STRENGTH) {
        return strength;
    }

    if (!hasStatusInternal(s)) {
        return 0;
    }

    switch (s) {
        case MS::ARTIFACT:
            return artifact;

        case MS::BLOCK_RETURN:
            return blockReturn;

        case MS::CHOKED:
            return choked;

        case MS::CORPSE_EXPLOSION:
            return corpseExplosion;

        case MS::LOCK_ON:
            return lockOn;

        case MS::MARK:
            return mark;

        case MS::METALLICIZE:
            return metallicize;

        case MS::PLATED_ARMOR:
            return platedArmor;

        case MS::POISON:
            return poison;

        case MS::REGEN:
            return regen;

        case MS::SHACKLED:
            return shackled;

        case MS::VULNERABLE:
            return vulnerable;

        case MS::WEAK:
            return weak;

        case MS::ANGRY:
        case MS::BEAT_OF_DEATH:
        case MS::CURIOSITY:
        case MS::CURL_UP:
        case MS::ENRAGE:
        case MS::FADING:
        case MS::FLIGHT:
        case MS::GENERIC_STRENGTH_UP:
        case MS::INTANGIBLE:
        case MS::MALLEABLE:
        case MS::MODE_SHIFT:
        case MS::RITUAL:
        case MS::SLOW:
        case MS::SPORE_CLOUD:
        case MS::THIEVERY:
        case MS::THORNS:
        case MS::TIME_WARP:
            return uniquePower0;

        case MS::INVINCIBLE:
        case MS::REACTIVE:
        case MS::SHARP_HIDE:
            return uniquePower1;

        // boolean powers
        case MS::ASLEEP:
        case MS::BARRICADE:
        case MS::MINION:
        case MS::MINION_LEADER:
        case MS::PAINFUL_STABS:
        case MS::REGROW:
        case MS::SHIFTING:
        case MS::STASIS:
            return true; // already did status check above

        default:
#ifdef sts_asserts
            assert(false);
#endif
            return 0;
    }
}

bool Monster::isAlive() const {
    return curHp > 0;
}

bool Monster::isTargetable() const {
    return !isDeadOrEscaped();
}

bool Monster::isDying() const {
    return curHp <= 0;
}

bool Monster::isEscaping() const {
    return isEscapingB;
}

bool Monster::isDeadOrEscaped() const {
    return isDying() || isHalfDead() || isEscaping();
}

bool Monster::isHalfDead() const {
    return halfDead;
}

bool Monster::doesEscapeNext() const {
    return escapeNext;
}

bool Monster::isAttacking() const {
    return isMoveAttack(moveHistory[0]);
}

void Monster::heal(int amount) {
#ifdef sts_asserts
    if (amount < 0) {
        assert (false);
    }
#endif

    curHp = std::min(maxHp, curHp + amount);
}

void Monster::addBlock(int amount) {
    block += amount;
}

void Monster::die(BattleContext &bc) {
    // Mugger.die() selects one of two death voice lines through
    // AbstractDungeon.aiRng before performing its normal death handling.
    // The sound is cosmetic, but the RNG consumption is part of the run's
    // deterministic continuation.
    if (id == MonsterId::MUGGER) {
        bc.aiRng.random(2);
    }
    --bc.monsters.monstersAlive;

    // SpireShield/SpireSpear.die() turns the player toward the survivor and
    // queues removal of Surrounded.  The simulator does not model the visual
    // BackAttack power, but it must remove the player power before the next
    // public boundary and before the surviving monster attacks.
    if ((id == MonsterId::SPIRE_SHIELD || id == MonsterId::SPIRE_SPEAR)
            && bc.monsters.monstersAlive > 0
            && bc.player.hasStatus<PS::SURROUNDED>()) {
        const int survivorIdx = idx == 0 ? 1 : 0;
        bc.player.lastTargetedMonster = static_cast<std::int8_t>(survivorIdx);
        bc.player.removeStatus<PS::SURROUNDED>();
    }

    if (id == MonsterId::AWAKENED_ONE && !miscInfo) { // is awakened one stage 1 // todo change to status
        halfDead = true;
        removeDebuffs();
        removeStatus<MS::CURIOSITY>();
        setMove(MonsterMoveId::AWAKENED_ONE_REBIRTH);
        bc.cardQueue.clear();

    } else if (bc.monsters.monstersAlive == 0 || hasStatus<MS::MINION_LEADER>()) {
//            bc.cleanCardQueue(); // todo should this really return like this?
        bc.outcome = Outcome::PLAYER_VICTORY;
        return;
    }

    if (hasStatus<MS::SPORE_CLOUD>()) {
        // spore cloud always has a value of 2 in game
        bc.addToTop( Actions::DebuffPlayer<PS::VULNERABLE>(2, bc.turnHasEnded) );

    } else if (hasStatus<MS::REGROW>()) {
        resetAllStatusEffects();
        setMove(MMID::DARKLING_REGROW);
        halfDead = true;

    } else if (hasStatus<MS::STASIS>()) {
        returnStasisCard(bc);
    }

    if (hasStatus<MS::CORPSE_EXPLOSION>()) {
        int damage = maxHp * getStatus<MS::CORPSE_EXPLOSION>();
        bc.addToBot( Actions::DamageAllEnemy(damage) );
    }

    if (bc.player.hasRelic<RelicId::GREMLIN_HORN>()) {
        bc.addToBot( Actions::GainEnergy(1) );
        bc.addToBot( Actions::DrawCards(1) );
    }

    if (bc.player.hasRelic<RelicId::THE_SPECIMEN>()) {
        bc.addToBot( Actions::SetState(InputState::SELECT_ENEMY_THE_SPECIMEN_APPLY_POISON) );
    }
}

void Monster::suicideAction(BattleContext &bc) {
    if (!isAlive()) {
        return;
    }

    --bc.monsters.monstersAlive;
    curHp = 0;
    if (bc.monsters.monstersAlive == 0) {
        bc.outcome = Outcome::PLAYER_VICTORY;
    }
}

void Monster::attackedUnblockedHelper(BattleContext &bc, int damage) { // todo, is this supposed to be only damage > 0 ????
    if (bc.player.hasRelic<RelicId::THE_BOOT>() && damage > 0 && damage < 5) {
        damage = 5;
    }

    if (bc.player.hasStatus<PS::ENVENOM>()) {
        bc.addToTop( Actions::DebuffEnemy<MS::POISON>(this->idx, bc.player.getStatus<PS::ENVENOM>()) );
    }

    if (hasStatus<MS::INVINCIBLE>()) {
        damage = std::min(damage, getStatus<MS::INVINCIBLE>());
        uniquePower1 = static_cast<std::int16_t>(getStatus<MS::INVINCIBLE>() - damage);
        // InvinciblePower remains installed at amount zero and restores its
        // per-turn cap in atStartOfTurn().  Removing the zero-valued power
        // would make the cap disappear permanently after the first 300/200
        // damage turn.

    } else if (hasStatus<MS::PLATED_ARMOR>()) {
        decrementStatus<MS::PLATED_ARMOR>();
        if(!hasStatus<MS::PLATED_ARMOR>() && id == MonsterId::SHELLED_PARASITE) {
            setMove(MMID::SHELLED_PARASITE_STUNNED);
        }

    // Curl Up's original onAttacked hook only triggers when the incoming
    // unblocked damage is strictly smaller than current HP.  A lethal hit
    // removes the monster and its block immediately, so queuing Curl Up here
    // would leave an observable dead-monster block in the agent state.
    } else if (hasStatus<MS::CURL_UP>() && !curlUpTriggered && damage < curHp) {
        bc.addToBot(Actions::MonsterGainBlock(this->idx, getStatus<MS::CURL_UP>()) );
        bc.addToBot(Actions::MonsterRemoveCurlUp(this->idx));
        curlUpTriggered = true;

    } else if (hasStatus<MS::FLIGHT>() && damage > 0) {
        auto flight = getStatus<MS::FLIGHT>();
        if (flight == 1) {
            setMove(MMID::BYRD_STUNNED);
        }
        setStatus<MS::FLIGHT>(flight-1);

    } else if (hasStatus<MS::MALLEABLE>() || hasStatus<MS::REACTIVE>()) {
        if (hasStatus<MS::MALLEABLE>()) {
            const auto malleable = getStatus<MS::MALLEABLE>();
            bc.addToBot( Actions::MonsterGainBlock(this->idx, malleable) );
            setStatus<MS::MALLEABLE>(malleable+1);
        }
        if (hasStatus<MS::REACTIVE>()) {
            // ReactivePower queues one RollMoveAction for every non-lethal
            // NORMAL hit.  Collapsing a multi-hit card to one roll (and
            // rolling after a lethal hit) changes the AI RNG stream.
            if (damage < curHp) {
                bc.addToBot( Actions::ReactiveRollMove() );
            }
        }

    } else if (hasStatus<MS::THORNS>()) {
        bc.addToTop( Actions::DamagePlayer(getStatus<MS::THORNS>()) );

    } else if (hasStatus<MS::ASLEEP>()) {
        // lagavulin
        setHasStatus<MS::ASLEEP>(false);
        decrementStatus<MS::METALLICIZE>(8);

    } else if (hasStatus<MS::SHIFTING>()) {
        addDebuff<MS::STRENGTH>(-damage);
        buff<MS::SHACKLED>(damage);
    }

    curHp -= damage;
    if (curHp <= 0) {
        curHp = 0;
        die(bc);
    } else {
        onHpLost(bc, damage);
    }
}

void Monster::attacked(BattleContext &bc, int damage) {
    if (isDeadOrEscaped()) { // these probably shouldn't happen
        std::cerr << bc.seed << " was dead when attacked" << idx << "\n" << bc;
        assert(false);
        return;
    }

    if (damage < 0) {// these probably shouldn't happen
        damage = 0;
    }

    if (hasStatus<MS::INTANGIBLE>()) {
        if (damage > 0) {
            damage = 1;
        }
    }

    const bool hadBlock = block > 0;
    const int tempDamage = damage;
    damage -= block;
    block = std::max(0, block - tempDamage);

    // AngryPower.onAttacked receives damage *after* block is consumed and
    // only grants Strength when that damage is positive.  Applying it before
    // the block calculation made a fully blocked hit (and even a zero-damage
    // hit) permanently strengthen Gremlin Warriors.
    if (damage > 0 && hasStatus<MS::ANGRY>()) {
        // The Java power queues ApplyPowerAction on top, which means the
        // increase resolves before a following multi-hit but is discarded if
        // this hit killed the owner.
        bc.addToTop(Actions::BuffEnemy<MS::STRENGTH>(idx, getStatus<MS::ANGRY>()));
    }

    if (hadBlock && block == 0 && bc.player.hasRelic<RelicId::HAND_DRILL>()) {
        bc.addToBot(Actions::DebuffEnemy<MS::VULNERABLE>(idx, 2, false) );
    }

    if (damage > 0) { // todo can damage be zero???
        attackedUnblockedHelper(bc, damage);
    }
}

void Monster::damageUnblockedHelper(BattleContext &bc, int damage) {

    if (hasStatus<MS::INVINCIBLE>()) {
        damage = std::min(damage, getStatus<MS::INVINCIBLE>());
        uniquePower1 = static_cast<std::int16_t>(getStatus<MS::INVINCIBLE>() - damage);
    }
        if (hasStatus<MS::ASLEEP>()) {
        // lagavulin
        setHasStatus<MS::ASLEEP>(false);
        decrementStatus<MS::METALLICIZE>(8);
    }
    if (hasStatus<MS::SHIFTING>()) {
        addDebuff<MS::STRENGTH>(-damage);
        buff<MS::SHACKLED>(damage);
    }

    curHp = std::max(0, curHp-damage);
    if (curHp == 0) {
        die(bc);
    } else {
        onHpLost(bc, damage);
    }
}

void Monster::damage(BattleContext &bc, int damage) {
    if (isDeadOrEscaped()) { // these probably shouldn't happen
        std::cerr << bc.seed << " was dead when damaged" << idx << "\n" << bc;
        assert(false);
        return;
    }

    if (damage < 0) {
        damage = 0;
    }

    if (hasStatus<MS::INTANGIBLE>()) { // this is probably wrong with potions
        if (damage > 0) {
            damage = 1;
        }
    }

    const bool hadBlock = block > 0;
    const int tempDamage = damage;
    damage -= block;
    block = std::max(0, block-tempDamage);

    if (hadBlock && block == 0 && bc.player.hasRelic<RelicId::HAND_DRILL>()) {
        bc.addToBot(Actions::DebuffEnemy<MS::VULNERABLE>(idx, 2, false) );
    }

    if (damage > 0) {
        damageUnblockedHelper(bc, damage);
    } else {
        return;
    }
}

void Monster::onHpLost(BattleContext &bc, int amount) {
    const bool atOrBelowHalf = curHp <= maxHp/2;
    switch (id) {
        case MonsterId::ACID_SLIME_L:
            if (atOrBelowHalf) {
                moveHistory[0] = MMID::ACID_SLIME_L_SPLIT;
            }
            break;

        case MonsterId::SLIME_BOSS:
            if (atOrBelowHalf) {
                moveHistory[0] = MMID::SLIME_BOSS_SPLIT;
            }
            break;

        case MonsterId::SPIKE_SLIME_L:
            if (atOrBelowHalf) {
                moveHistory[0] = MMID::SPIKE_SLIME_L_SPLIT;
            }
            break;

        case MonsterId::THE_GUARDIAN:
            if (hasStatus<MS::MODE_SHIFT>()) {
                const int newModeShiftAmount = getStatus<MS::MODE_SHIFT>() - amount;
                if (newModeShiftAmount <= 0) {
                    removeStatus<MS::MODE_SHIFT>();
                    setMove(MMID::THE_GUARDIAN_DEFENSIVE_MODE);
                    bc.addToBot( Actions::MonsterGainBlock(idx, 20) );
                } else {
                    setStatus<MS::MODE_SHIFT>(newModeShiftAmount);
                }
            }
            break;

        default:
            break;
    }
}

void Monster::removeDebuffs() {
    if (getStatus<MS::STRENGTH>() < 0) {
        setStatus<MS::STRENGTH>(0);
    }

    removeStatus<MS::BLOCK_RETURN>();
    removeStatus<MS::CHOKED>();
    removeStatus<MS::CORPSE_EXPLOSION>();
    removeStatus<MS::LOCK_ON>();
    removeStatus<MS::MARK>();
    removeStatus<MS::POISON>();
    removeStatus<MS::SHACKLED>();
    removeStatus<MS::VULNERABLE>();
    removeStatus<MS::WEAK>();
}

void Monster::resetAllStatusEffects() {
    statusBits = 0;
    setStatus<MS::STRENGTH>(0);
    block = 0;
    curlUpTriggered = false;
}

// this is calculated at the player when the damage occurs in game, consider testing whether there are any scenarios when it can't be done before
int Monster::calculateDamageToPlayer(const BattleContext &bc, int baseDamage) const {
    const auto &p = bc.player;
    auto damage = static_cast<float>(baseDamage + getStatus<MS::STRENGTH>());

    if (p.hasStatus<PS::SURROUNDED>()) {
        const bool facingSelf = p.lastTargetedMonster == idx || bc.monsters.arr[p.lastTargetedMonster].isDeadOrEscaped();
        if (!facingSelf) {
            damage *= 1.5;
        }
    }

    if (hasStatus<MS::WEAK>()) {
        if (p.hasRelic<RelicId::PAPER_KRANE>()) {
            damage *= 0.6f;
        } else {
            damage *= 0.75f;
        }
    }

    if (p.hasStatus<PS::VULNERABLE>()) {
        if (p.hasRelic<RelicId::ODD_MUSHROOM>()) {
            damage *= 1.25f;
        } else {
            damage *= 1.5f;
        }
    }

    if (p.stance == Stance::WRATH) {
        damage *= 2;
    }

    // apply backIdx attack for spire guard elites

    if (p.hasStatus<PS::INTANGIBLE>()) {
        damage = std::min(damage, static_cast<float>(1));
    }

    return std::max(static_cast<int>(std::floor(damage)), 0);
}

void Monster::attackPlayerHelper(BattleContext &bc, int baseDamage, int times) {
    const int damage = calculateDamageToPlayer(bc, baseDamage);

    for (int i = 0; i < times; i++) {
        bc.addToBot(Actions::AttackPlayer(idx, damage));
    }
}

bool Monster::firstTurn() const {
    return moveHistory[0] == MMID::INVALID;
}

bool Monster::lastMove(MMID moveId) const {
    return moveHistory[0] == moveId;
}

bool Monster::lastMoveBefore(MMID moveId) const {
    return moveHistory[1] == moveId;
}

bool Monster::lastTwoMoves(MMID moveId) const {
    return moveHistory[0] == moveId && moveHistory[1] == moveId;
}

bool Monster::eitherLastTwo(MonsterMoveId moveId) const {
    return moveHistory[0] == moveId || moveHistory[1] == moveId;
}

void Monster::rollMove(BattleContext &bc) {
    auto miscInfoCopy = miscInfo;

    const auto move = getMoveForRoll(bc, miscInfoCopy, bc.aiRng.random(99));

    miscInfo = miscInfoCopy;
    setMove(move);
}

void Monster::setMove(MMID moveId) {
    moveHistory[1] = moveHistory[0];
    moveHistory[0] = moveId;
}

namespace sts {

    std::ostream& printIfHaveStatus(std::ostream &os, const Monster &m, MonsterStatus s, bool &havePrint) {
        if (!m.getStatusInternal(s)) {
            return os;
        }

        if (havePrint) {
            os << ", ";
        }
        havePrint = true;

        os << '(' << enemyStatusStrings[(int)s];
        if (!isBooleanPower(s)) {
            os << "," << m.getStatusInternal(s);
        }
        os << ")";
        return os;
    }



    std::ostream &operator<<(std::ostream &os, const Monster &m) {
        os << "{";
        os << m.idx << " " << sts::monsterIdStrings[(int) m.id]
           << " hp:(" << m.curHp << "/" << m.maxHp << ")"
           << " block:(" << m.block << ") statusEffects:{";

        bool havePrint = false;
        printIfHaveStatus(os, m, MS::STRENGTH, havePrint);
        for (int i = static_cast<int>(MS::ARTIFACT); i <= static_cast<int>(MS::STASIS); ++i) {
            auto s = static_cast<MS>(i);
            if (s != MonsterStatus::STRENGTH) {
                printIfHaveStatus(os, m, s, havePrint);
            }
        }
        os << "}";

        os << " halfDead: " << m.halfDead
           << ", moveHistory: { " << monsterMoveStrings[(int) m.moveHistory[0]] << ", " << monsterMoveStrings[(int) m.moveHistory[1]] << "}"
           << " nextActionDamage: " << " miscInfo: " << m.miscInfo << " uniquePower0: " << m.uniquePower0 << " uniquePower1: " << m.uniquePower1
            << "}";
        return os;
    }

}

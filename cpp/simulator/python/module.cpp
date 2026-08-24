#include <algorithm>
#include <cctype>
#include <cstdint>
#include <memory>
#include <stdexcept>
#include <string>
#include <vector>

#include <pybind11/pybind11.h>
#include <pybind11/stl.h>

#include "combat/BattleContext.h"
#include "constants/Cards.h"
#include "constants/MonsterEncounters.h"
#include "game/GameContext.h"
#include "sim/search/Action.h"
#include "sim/search/GameAction.h"
#include "sim/search/SimpleAgent.h"
#include "sim/search/BattleScumSearcher2.h"

namespace py = pybind11;
using namespace sts;

namespace {

std::string normalized(std::string value) {
    std::string result;
    for (const unsigned char c : value) {
        if (std::isalnum(c)) {
            result.push_back(static_cast<char>(std::toupper(c)));
        }
    }
    return result;
}

MonsterEncounter parse_encounter(const std::string &name) {
    const auto wanted = normalized(name);
    const auto count = sizeof(monsterEncounterStrings) / sizeof(monsterEncounterStrings[0]);
    for (std::size_t i = 1; i < count; ++i) {
        if (normalized(monsterEncounterStrings[i]) == wanted) {
            return static_cast<MonsterEncounter>(i);
        }
    }
    throw std::invalid_argument("Unknown encounter: " + name);
}

MonsterId parse_monster(const std::string &name) {
    const auto wanted = normalized(name);
    const auto count = sizeof(monsterIdStrings) / sizeof(monsterIdStrings[0]);
    for (std::size_t i = 1; i < count; ++i) {
        if (normalized(monsterIdStrings[i]) == wanted) return static_cast<MonsterId>(i);
    }
    throw std::invalid_argument("Unknown monster: " + name);
}

MMID parse_move(const std::string &name) {
    const auto wanted = normalized(name);
    const auto count = sizeof(monsterMoveStrings) / sizeof(monsterMoveStrings[0]);
    for (std::size_t i = 0; i < count; ++i) {
        if (normalized(monsterMoveStrings[i]) == wanted) return static_cast<MMID>(i);
    }
    throw std::invalid_argument("Unknown monster move: " + name);
}

Card parse_card(const std::string &name) {
    int upgrades = 0;
    auto raw_name = name;
    const auto plus = name.rfind('+');
    if (plus != std::string::npos) {
        raw_name = name.substr(0, plus);
        const auto suffix = name.substr(plus + 1);
        upgrades = suffix.empty() ? 1 : std::stoi(suffix);
        if (upgrades < 0) throw std::invalid_argument("Negative card upgrade count");
    }
    auto wanted = normalized(raw_name);
    if (wanted == "STRIKER") wanted = "STRIKERED";
    if (wanted == "DEFENDR") wanted = "DEFENDRED";
    const auto count = sizeof(cardEnumStrings) / sizeof(cardEnumStrings[0]);
    for (std::size_t i = 1; i < count; ++i) {
        if (normalized(cardEnumStrings[i]) == wanted || normalized(cardNames[i]) == wanted) {
            Card card(static_cast<CardId>(i));
            for (int upgrade = 0; upgrade < upgrades; ++upgrade) card.upgrade();
            return card;
        }
    }
    throw std::invalid_argument("Unknown card: " + name);
}

Potion parse_potion(const std::string &name) {
    const auto wanted = normalized(name);
    const auto count = sizeof(potionNames) / sizeof(potionNames[0]);
    for (std::size_t i = 1; i < count; ++i) {
        if (normalized(potionNames[i]) == wanted) return static_cast<Potion>(i);
    }
    throw std::invalid_argument("Unknown potion: " + name);
}

RelicId parse_relic(const std::string &name) {
    const auto wanted = normalized(name);
    const auto count = sizeof(relicEnumNames) / sizeof(relicEnumNames[0]);
    for (std::size_t i = 0; i + 1 < count; ++i) {
        if (normalized(relicEnumNames[i]) == wanted ||
            normalized(relicNames[i]) == wanted ||
            normalized(relicIds[i]) == wanted) {
            return static_cast<RelicId>(i);
        }
    }
    throw std::invalid_argument("Unknown relic: " + name);
}

py::dict power(const char *id, int amount, const char *name = nullptr) {
    py::dict value;
    value["id"] = id;
    value["name"] = name == nullptr ? id : name;
    value["amount"] = amount;
    return value;
}

py::list player_powers(const Player &p) {
    py::list result;
    for (const auto &[status, amount] : p.orderedPowers()) {
        // Panache stores damage in statusMap, while the stock AbstractPower
        // exposes its remaining five-card countdown as `amount`.
        const int publicAmount = status == PlayerStatus::PANACHE
            ? p.panacheCounter : amount;
        result.append(power(
            playerStatusEnumStrings[static_cast<int>(status)], publicAmount,
            playerStatusStrings[static_cast<int>(status)]));
    }
    // Stock represents each bomb as TheBombPower with `amount` equal to the
    // remaining turns.  Native stores the scheduled damage in countdown
    // buckets, so expose the corresponding public countdown rather than
    // hiding the mechanism or leaking the damage as `amount`.
    if (p.bomb1) result.append(power("THE_BOMB", 1, "The Bomb"));
    if (p.bomb2) result.append(power("THE_BOMB", 2, "The Bomb"));
    if (p.bomb3) result.append(power("THE_BOMB", 3, "The Bomb"));
    return result;
}

py::list monster_powers(const Monster &m) {
    py::list result;
    // Stock inserts SplitPower before debuffs subsequently applied to the
    // Slime Boss. Keep that public ordering even though the native simulator
    // models the threshold outside the status map.
    if ((m.id == MonsterId::SLIME_BOSS || m.id == MonsterId::ACID_SLIME_L
            || m.id == MonsterId::SPIKE_SLIME_L) && !m.isDeadOrEscaped()) {
        result.append(power("SPLIT", -1, "SPLIT"));
    }
    for (const auto &[status, amount] : m.orderedPowers()) {
        const int index = static_cast<int>(status);
        result.append(power(
            enemyStatusStrings[index], amount, enemyStatusStrings[index]));
    }
    return result;
}

py::dict checkpoint_monster_ghost(const Monster &monster, int slot) {
    py::dict ghost;
    ghost["slot"] = slot;
    ghost["id"] = static_cast<int>(monster.id);
    ghost["current_hp"] = monster.curHp;
    ghost["max_hp"] = monster.maxHp;
    ghost["block"] = monster.block;
    ghost["move_current"] = static_cast<int>(monster.moveHistory[0]);
    ghost["move_previous"] = static_cast<int>(monster.moveHistory[1]);
    ghost["status_bits"] = monster.statusBits;
    ghost["artifact"] = monster.artifact;
    ghost["block_return"] = monster.blockReturn;
    ghost["choked"] = monster.choked;
    ghost["corpse_explosion"] = monster.corpseExplosion;
    ghost["lock_on"] = monster.lockOn;
    ghost["mark"] = monster.mark;
    ghost["metallicize"] = monster.metallicize;
    ghost["plated_armor"] = monster.platedArmor;
    ghost["poison"] = monster.poison;
    ghost["regen"] = monster.regen;
    ghost["shackled"] = monster.shackled;
    ghost["strength"] = monster.strength;
    ghost["vulnerable"] = monster.vulnerable;
    ghost["weak"] = monster.weak;
    ghost["unique_power0"] = monster.uniquePower0;
    ghost["unique_power1"] = monster.uniquePower1;
    ghost["misc_info"] = monster.miscInfo;
    ghost["half_dead"] = monster.halfDead;
    ghost["is_escaping"] = monster.isEscapingB;
    ghost["escape_next"] = monster.escapeNext;
    py::list power_order;
    for (const auto power : monster.powerOrder) power_order.append(static_cast<int>(power));
    ghost["power_order"] = power_order;
    return ghost;
}

Monster restore_checkpoint_monster_ghost(const py::dict &ghost) {
    Monster monster;
    monster.idx = ghost["slot"].cast<int>();
    monster.id = static_cast<MonsterId>(ghost["id"].cast<int>());
    monster.curHp = ghost["current_hp"].cast<int>();
    monster.maxHp = ghost["max_hp"].cast<int>();
    monster.block = ghost["block"].cast<int>();
    monster.moveHistory[0] = static_cast<MonsterMoveId>(ghost["move_current"].cast<int>());
    monster.moveHistory[1] = static_cast<MonsterMoveId>(ghost["move_previous"].cast<int>());
    monster.statusBits = ghost["status_bits"].cast<std::uint64_t>();
    monster.artifact = ghost["artifact"].cast<int>();
    monster.blockReturn = ghost["block_return"].cast<int>();
    monster.choked = ghost["choked"].cast<int>();
    monster.corpseExplosion = ghost["corpse_explosion"].cast<int>();
    monster.lockOn = ghost["lock_on"].cast<int>();
    monster.mark = ghost["mark"].cast<int>();
    monster.metallicize = ghost["metallicize"].cast<int>();
    monster.platedArmor = ghost["plated_armor"].cast<int>();
    monster.poison = ghost["poison"].cast<int>();
    monster.regen = ghost["regen"].cast<int>();
    monster.shackled = ghost["shackled"].cast<int>();
    monster.strength = ghost["strength"].cast<int>();
    monster.vulnerable = ghost["vulnerable"].cast<int>();
    monster.weak = ghost["weak"].cast<int>();
    monster.uniquePower0 = ghost["unique_power0"].cast<int>();
    monster.uniquePower1 = ghost["unique_power1"].cast<int>();
    monster.miscInfo = ghost["misc_info"].cast<int>();
    monster.halfDead = ghost["half_dead"].cast<bool>();
    monster.isEscapingB = ghost["is_escaping"].cast<bool>();
    monster.escapeNext = ghost["escape_next"].cast<bool>();
    for (const auto power : ghost["power_order"].cast<py::list>()) {
        monster.powerOrder.push_back(static_cast<MonsterStatus>(power.cast<int>()));
    }
    return monster;
}

const char *intent_name(const Monster &monster, const BattleContext *context = nullptr) {
    // Three original moves reuse one byte/move id with a different public
    // intent.  Preserve the conditions from the Java getMove methods instead
    // of flattening them into a single label.
    if (monster.moveHistory[0] == MMID::DARKLING_HARDEN) {
        return context != nullptr && context->ascension >= 17 ? "DEFEND_BUFF" : "DEFEND";
    }
    if (monster.moveHistory[0] == MMID::DECA_SQUARE_OF_PROTECTION) {
        return context != nullptr && context->ascension >= 19 ? "DEFEND_BUFF" : "DEFEND";
    }
    if (monster.moveHistory[0] == MMID::GREMLIN_NOB_SKULL_BASH) {
        return context != nullptr && context->encounter == MonsterEncounter::COLOSSEUM_EVENT_NOBS
            ? "ATTACK" : "ATTACK_DEBUFF";
    }
    switch (monster.moveHistory[0]) {
        case MMID::GENERIC_ESCAPE_MOVE:
        case MMID::LOOTER_ESCAPE:
        case MMID::MUGGER_ESCAPE:
            return "ESCAPE";
        case MMID::BEAR_BEAR_HUG:
        case MMID::BRONZE_ORB_STASIS:
        case MMID::CHOSEN_HEX:
        case MMID::CORRUPT_HEART_DEBILITATE:
        case MMID::LAGAVULIN_SIPHON_SOUL:
        case MMID::RED_SLAVER_ENTANGLE:
        case MMID::SLIME_BOSS_GOOP_SPRAY:
        case MMID::SNAKE_PLANT_ENFEEBLING_SPORES:
        case MMID::SNECKO_PERPLEXING_GLARE:
        case MMID::SPIRE_GROWTH_CONSTRICT:
        case MMID::THE_COLLECTOR_MEGA_DEBUFF:
        case MMID::THE_GUARDIAN_VENT_STEAM:
        case MMID::THE_MAW_ROAR:
        case MMID::WRITHING_MASS_IMPLANT:
            return "STRONG_DEBUFF";
        case MMID::ACID_SLIME_L_LICK:
        case MMID::ACID_SLIME_M_LICK:
        case MMID::ACID_SLIME_S_LICK:
        case MMID::CHOSEN_DRAIN:
        case MMID::GIANT_HEAD_GLARE:
        case MMID::GREEN_LOUSE_SPIT_WEB:
        case MMID::NEMESIS_DEBUFF:
        case MMID::REPULSOR_REPULSE:
        case MMID::ROMEO_MOCK:
        case MMID::SENTRY_BOLT:
        case MMID::SPIKE_SLIME_L_LICK:
        case MMID::SPIKE_SLIME_M_LICK:
        case MMID::THE_CHAMP_TAUNT:
            return "DEBUFF";
        case MMID::ACID_SLIME_L_CORROSIVE_SPIT:
        case MMID::ACID_SLIME_M_CORROSIVE_SPIT:
        case MMID::AWAKENED_ONE_SLUDGE:
        case MMID::BLUE_SLAVER_RAKE:
        case MMID::CHOSEN_DEBILITATE:
        case MMID::DECA_BEAM:
        case MMID::FAT_GREMLIN_SMASH:
        case MMID::HEXAGHOST_INFERNO:
        case MMID::HEXAGHOST_SEAR:
        case MMID::MYSTIC_ATTACK_DEBUFF:
        case MMID::ORB_WALKER_LASER:
        case MMID::RED_SLAVER_SCRAPE:
        case MMID::REPTOMANCER_SNAKE_STRIKE:
        case MMID::SHELLED_PARASITE_FELL:
        case MMID::SNECKO_TAIL_WHIP:
        case MMID::SPHERIC_GUARDIAN_ATTACK_DEBUFF:
        case MMID::SPIKE_SLIME_L_FLAME_TACKLE:
        case MMID::SPIKE_SLIME_M_FLAME_TACKLE:
        case MMID::SPIRE_SHIELD_BASH:
        case MMID::SPIRE_SPEAR_BURN_STRIKE:
        case MMID::TASKMASTER_SCOURING_WHIP:
        case MMID::THE_CHAMP_FACE_SLAP:
        case MMID::TIME_EATER_HEAD_SLAM:
        case MMID::WRITHING_MASS_WITHER:
            return "ATTACK_DEBUFF";
        case MMID::SHELLED_PARASITE_SUCK:
        case MMID::THE_GUARDIAN_TWIN_SLAM:
            return "ATTACK_BUFF";
        case MMID::CULTIST_INCANTATION:
        case MMID::CORRUPT_HEART_BUFF:
        case MMID::DARKLING_REINCARNATE:
        case MMID::DONU_CIRCLE_OF_POWER:
        case MMID::BYRD_CAW:
        case MMID::FUNGI_BEAST_GROW:
        case MMID::GREMLIN_NOB_BELLOW:
        case MMID::MYSTIC_BUFF:
        case MMID::MYSTIC_HEAL:
        case MMID::RED_LOUSE_GROW:
        case MMID::SPIKER_SPIKE:
        case MMID::SPIRE_SPEAR_PIERCER:
        case MMID::THE_CHAMP_ANGER:
        case MMID::THE_CHAMP_GLOAT:
        case MMID::THE_MAW_DROOL:
        case MMID::TIME_EATER_HASTE:
        case MMID::THE_GUARDIAN_DEFENSIVE_MODE:
            return "BUFF";
        case MMID::BRONZE_AUTOMATON_BOOST:
        case MMID::GREMLIN_LEADER_ENCOURAGE:
        case MMID::HEXAGHOST_INFLAME:
        case MMID::THE_CHAMP_DEFENSIVE_STANCE:
        case MMID::THE_COLLECTOR_BUFF:
            return "DEFEND_BUFF";
        case MMID::JAW_WORM_BELLOW:
            return "DEFEND_BUFF";
        case MMID::JAW_WORM_THRASH:
        case MMID::SPHERIC_GUARDIAN_HARDEN:
        case MMID::SPIRE_SHIELD_SMASH:
        case MMID::WRITHING_MASS_FLAIL:
            return "ATTACK_DEFEND";
        case MMID::TIME_EATER_RIPPLE:
            return "DEFEND_DEBUFF";
        case MMID::BRONZE_ORB_SUPPORT_BEAM:
        case MMID::CENTURION_DEFEND:
        case MMID::LOOTER_SMOKE_BOMB:
        case MMID::MUGGER_SMOKE_BOMB:
        case MMID::SHIELD_GREMLIN_PROTECT:
        case MMID::SPHERIC_GUARDIAN_ACTIVATE:
        case MMID::SPIRE_SHIELD_FORTIFY:
        case MMID::THE_GUARDIAN_CHARGING_UP:
            return "DEFEND";
        case MMID::LAGAVULIN_SLEEP:
            // Original intent is SLEEP while AsleepPower is present, then STUN
            // after the first hit wakes Lagavulin but before its next turn.
            return monster.hasStatus<MonsterStatus::ASLEEP>() ? "SLEEP" : "STUN";
        case MMID::HEXAGHOST_ACTIVATE:
        case MMID::AWAKENED_ONE_REBIRTH:
        case MMID::BRONZE_AUTOMATON_SPAWN_ORBS:
        case MMID::BYRD_FLY:
        case MMID::DARKLING_REGROW:
        case MMID::EXPLODER_EXPLODE:
        case MMID::GREMLIN_LEADER_RALLY:
        case MMID::GREMLIN_WIZARD_CHARGING:
        case MMID::REPTOMANCER_SUMMON:
        case MMID::SLIME_BOSS_PREPARING:
        case MMID::THE_COLLECTOR_SPAWN:
        case MMID::ACID_SLIME_L_SPLIT:
        case MMID::SLIME_BOSS_SPLIT:
        case MMID::SPIKE_SLIME_L_SPLIT:
            return "UNKNOWN";
        case MMID::BRONZE_AUTOMATON_STUNNED:
        case MMID::BYRD_STUNNED:
        case MMID::SHELLED_PARASITE_STUNNED:
            return "STUN";
        default: return monster.isAttacking() ? "ATTACK" : "UNKNOWN";
    }
}

py::dict card_dict(const CardInstance &card, const BattleContext *bc = nullptr) {
    py::dict result;
    result["id"] = getCardEnumName(card.id);
    result["name"] = getCardName(card.id);
    result["uuid"] = std::to_string(card.uniqueId);
    result["cost"] = static_cast<int>(card.costForTurn);
    result["base_cost"] = static_cast<int>(card.cost);
    result["upgrades"] = card.getUpgradeCount();
    result["special_data"] = card.specialData;
    result["free_to_play_once"] = card.freeToPlayOnce;
    result["retain"] = card.retain;
    result["self_retain"] = card.hasSelfRetain();
    result["ethereal"] = card.isEthereal();
    result["has_target"] = card.requiresTarget();
    result["exhausts"] = card.doesExhaust();
    result["is_playable"] = bc != nullptr && card.canUseOnAnyTarget(*bc);

    if (bc != nullptr && card.requiresTarget()) {
        py::list targets;
        const int hand_idx = static_cast<int>(&card - bc->cards.hand.data());
        for (int target = 0; target < bc->monsters.monsterCount; ++target) {
            search::Action action(search::ActionType::CARD, hand_idx, target);
            if (action.isValidAction(*bc)) targets.append(target);
        }
        result["playable_targets"] = targets;
    }
    return result;
}

py::dict public_run_card(const Card &card, const std::string &instance_id) {
    const CardInstance instance(card);
    py::dict result;
    result["instance_id"] = instance_id;
    result["content_id"] = getCardEnumName(card.id);
    result["upgrades"] = card.getUpgraded();
    result["base_cost"] = static_cast<int>(instance.cost);
    result["current_cost"] = static_cast<int>(instance.costForTurn);
    return result;
}

py::dict public_combat_card(
        const CardInstance &card,
        const std::string &zone,
        const BattleContext *bc = nullptr) {
    auto result = card_dict(card, bc);
    result["instance_id"] = zone + ":" + std::to_string(card.uniqueId);
    result["content_id"] = getCardEnumName(card.id);
    result["zone"] = zone;
    return result;
}

py::list public_map_state(const GameContext &gc) {
    py::list result;
    for (int y = 0; y < 15; ++y) {
        for (int x = 0; x < 7; ++x) {
            const auto &node = gc.map->nodes[y][x];
            if (node.parentCount == 0 && node.edgeCount == 0) continue;
            py::dict value;
            value["node_id"] = "map:" + std::to_string(x) + ":" + std::to_string(y);
            value["x"] = x;
            value["y"] = y;
            value["room_type"] = roomStrings[static_cast<int>(node.room)];
            const bool reachable = gc.curMapNodeY == -1
                ? y == 0 && node.edgeCount > 0
                : gc.curMapNodeY == 14
                    ? false
                    : y == gc.curMapNodeY + 1 && [&]() {
                        const auto &current = gc.map->getNode(gc.curMapNodeX, gc.curMapNodeY);
                        for (int edge = 0; edge < current.edgeCount; ++edge) {
                            if (current.edges[edge] == x) return true;
                        }
                        return false;
                    }();
            value["reachable"] = reachable;
            value["burning"] = x == gc.map->burningEliteX && y == gc.map->burningEliteY;
            py::list outgoing;
            for (int edge = 0; edge < node.edgeCount; ++edge) {
                outgoing.append(
                    "map:" + std::to_string(node.edges[edge]) + ":" + std::to_string(y + 1));
            }
            value["outgoing_node_ids"] = outgoing;
            result.append(value);
        }
    }
    return result;
}

py::dict public_combat_choice_state(const BattleContext &bc) {
    py::dict result;
    const auto task = bc.cardSelectInfo.cardSelectTask;
    result["task"] = cardSelectTaskStrings[static_cast<int>(task)];
    py::list options;

    auto append_cards = [&options](const auto &begin, const auto &end, const char *zone) {
        for (auto it = begin; it != end; ++it) {
            options.append(public_combat_card(*it, zone, nullptr));
        }
    };
    switch (task) {
        case CardSelectTask::ARMAMENTS:
        case CardSelectTask::DUAL_WIELD:
        case CardSelectTask::EXHAUST_ONE:
        case CardSelectTask::EXHAUST_MANY:
        case CardSelectTask::FORETHOUGHT:
        case CardSelectTask::GAMBLE:
        case CardSelectTask::RETAIN_CARDS:
        case CardSelectTask::WARCRY:
            result["source"] = "HAND";
            append_cards(bc.cards.hand.begin(), bc.cards.hand.begin() + bc.cards.cardsInHand, "HAND");
            break;
        case CardSelectTask::EXHUME:
            result["source"] = "EXHAUST";
            append_cards(bc.cards.exhaustPile.begin(), bc.cards.exhaustPile.end(), "EXHAUST");
            break;
        case CardSelectTask::HEADBUTT:
        case CardSelectTask::HOLOGRAM:
        case CardSelectTask::LIQUID_MEMORIES_POTION:
            result["source"] = "DISCARD";
            append_cards(bc.cards.discardPile.begin(), bc.cards.discardPile.end(), "DISCARD");
            break;
        case CardSelectTask::SECRET_TECHNIQUE:
        case CardSelectTask::SECRET_WEAPON:
        case CardSelectTask::SEEK:
            result["source"] = "DRAW";
            append_cards(bc.cards.drawPile.begin(), bc.cards.drawPile.end(), "DRAW");
            break;
        case CardSelectTask::CODEX:
        case CardSelectTask::DISCOVERY:
            result["source"] = "GENERATED";
            for (int index = 0; index < 3; ++index) {
                auto value = public_combat_card(
                    CardInstance(bc.cardSelectInfo.cards[index]), "GENERATED", nullptr);
                value["instance_id"] = "combat-choice:" + std::to_string(index);
                options.append(value);
            }
            break;
        default:
            result["source"] = "GENERATED";
            break;
    }
    result["options"] = options;
    return result;
}

py::dict public_combat_state(
        const BattleContext &bc,
        const std::array<MMID, 7> *display_moves = nullptr) {
    py::dict result;
    result["turn"] = bc.turn + 1;
    py::dict player;
    player["current_hp"] = bc.player.curHp;
    player["max_hp"] = bc.player.maxHp;
    player["block"] = bc.player.block;
    player["energy"] = bc.player.energy;
    player["max_energy"] = bc.player.energyPerTurn;
    player["powers"] = player_powers(bc.player);
    result["player"] = player;

    auto cards = [](const auto &values, const std::string &zone, const BattleContext *context) {
        py::list output;
        for (const auto &card : values) output.append(public_combat_card(card, zone, context));
        return output;
    };
    py::list hand;
    for (int i = 0; i < bc.cards.cardsInHand; ++i) {
        hand.append(public_combat_card(bc.cards.hand[i], "HAND", &bc));
    }
    result["hand"] = hand;
    result["draw_pile"] = cards(bc.cards.drawPile, "DRAW", nullptr);
    result["discard_pile"] = cards(bc.cards.discardPile, "DISCARD", nullptr);
    result["exhaust_pile"] = cards(bc.cards.exhaustPile, "EXHAUST", nullptr);

    py::list monsters;
    auto append_monster = [&bc, &monsters, display_moves](
            int index, const std::string &instance_id) {
        const auto &monster = bc.monsters.arr[index];
        const bool hide_intents = bc.player.hasRelic<RelicId::RUNIC_DOME>();
        auto display_monster = monster;
        if (display_moves != nullptr && index < static_cast<int>(display_moves->size())) {
            display_monster.moveHistory[0] = (*display_moves)[index];
        }
        const auto damage = display_monster.getMoveBaseDamage(bc);
        const int adjusted_damage = damage.attackCount > 0
            ? display_monster.calculateDamageToPlayer(bc, damage.damage)
            : 0;
        py::dict value;
        value["instance_id"] = instance_id;
        value["content_id"] = monsterIdStrings[static_cast<int>(monster.id)];
        value["current_hp"] = monster.curHp;
        value["max_hp"] = monster.maxHp;
        value["block"] = monster.block;
        // CommunicationMod leaves a dead Byrd's last visible Caw intent on
        // screen even when its internal grounded move has become STUN.
        value["intent"] = hide_intents
            ? "NONE"
            : monster.id == MonsterId::BYRD &&
                monster.isDeadOrEscaped() &&
                display_monster.moveHistory[0] == MonsterMoveId::BYRD_STUNNED
            ? "BUFF"
            : intent_name(display_monster, &bc);
        value["intent_damage"] = hide_intents ? 0 : adjusted_damage;
        value["intent_hits"] = hide_intents ? 0 : damage.attackCount;
        value["powers"] = monster_powers(monster);
        value["is_gone"] = monster.isDeadOrEscaped();
        monsters.append(value);
    };
    auto append_summon_ghost = [&bc, &monsters](
            const Monster &monster, const std::string &instance_id) {
        const bool hide_intents = bc.player.hasRelic<RelicId::RUNIC_DOME>();
        const auto damage = monster.getMoveBaseDamage(bc);
        const int adjusted_damage = damage.attackCount > 0
            ? monster.calculateDamageToPlayer(bc, damage.damage)
            : 0;
        py::dict value;
        value["instance_id"] = instance_id;
        value["content_id"] = monsterIdStrings[static_cast<int>(monster.id)];
        value["current_hp"] = monster.curHp;
        value["max_hp"] = monster.maxHp;
        value["block"] = monster.block;
        value["intent"] = hide_intents ? "NONE" : intent_name(monster, &bc);
        value["intent_damage"] = hide_intents ? 0 : adjusted_damage;
        value["intent_hits"] = hide_intents ? 0 : damage.attackCount;
        value["powers"] = monster_powers(monster);
        value["is_gone"] = monster.isDeadOrEscaped();
        monsters.append(value);
    };
    if (bc.encounter == MonsterEncounter::SLIME_BOSS &&
            bc.monsters.arr[6].id == MonsterId::SLIME_BOSS) {
        const bool spike_split = bc.monsters.arr[4].id == MonsterId::SPIKE_SLIME_L;
        const bool acid_split = bc.monsters.arr[5].id == MonsterId::ACID_SLIME_L;
        if (spike_split) {
            append_monster(0, "monster:0");
            append_monster(4, "monster:ghost-spike-large");
            append_monster(1, "monster:1");
        } else {
            append_monster(0, "monster:0");
        }
        if (acid_split) {
            append_monster(2, "monster:2");
            append_monster(6, "monster:ghost-slime-boss");
            append_monster(5, "monster:ghost-acid-large");
            append_monster(3, "monster:3");
        } else {
            append_monster(6, "monster:ghost-slime-boss");
            append_monster(2, "monster:2");
        }
    } else if (bc.encounter == MonsterEncounter::LARGE_SLIME &&
            (bc.monsters.arr[4].id == MonsterId::ACID_SLIME_L ||
             bc.monsters.arr[4].id == MonsterId::SPIKE_SLIME_L)) {
        append_monster(0, "monster:0");
        append_monster(4, "monster:ghost-large-slime");
        append_monster(1, "monster:1");
    } else if (bc.encounter == MonsterEncounter::GREMLIN_LEADER) {
        // Java appends newly summoned Gremlins while retaining replaced dead
        // minions in MonsterGroup.  Lightspeed reuses slots, so reconstruct
        // that public history as current minion then newest-to-oldest ghosts.
        for (int index = 0; index < 3; ++index) {
            if (bc.monsters.arr[index].id != MonsterId::INVALID) {
                append_monster(index, "monster:" + std::to_string(index));
            }
            for (int ghost = bc.gremlinLeaderGhostCount - 1; ghost >= 0; --ghost) {
                if (bc.gremlinLeaderGhostSlots[ghost] == index) {
                    append_summon_ghost(
                        bc.gremlinLeaderGhosts[ghost],
                        "monster:ghost-gremlin-" + std::to_string(ghost));
                }
            }
        }
        append_monster(3, "monster:3");
    } else if (bc.encounter == MonsterEncounter::COLLECTOR ||
            bc.encounter == MonsterEncounter::REPTOMANCER) {
        // SpawnMonsterAction inserts a replacement while retaining its dead
        // predecessor.  The fixed native slots are ordered by the Java drawX
        // positions: Collector [slot 0, 1, boss], Reptomancer [0, 1, boss,
        // 3, 4].  Equal-position replacements are inserted before older dead
        // entities, hence newest-to-oldest ghost order within each slot.
        const int slot_count = bc.encounter == MonsterEncounter::COLLECTOR ? 3 : 5;
        for (int index = 0; index < slot_count; ++index) {
            if (bc.monsters.arr[index].id != MonsterId::INVALID) {
                append_monster(index, "monster:" + std::to_string(index));
            }
            for (int ghost = bc.reusedSummonGhostCount - 1; ghost >= 0; --ghost) {
                if (bc.reusedSummonGhostSlots[ghost] == index) {
                    append_summon_ghost(
                        bc.reusedSummonGhosts[ghost],
                        "monster:ghost-summon-" + std::to_string(ghost));
                }
            }
        }
    } else {
        for (int index = 0; index < bc.monsters.monsterCount; ++index) {
            const auto &monster = bc.monsters.arr[index];
            // Summoning encounters reserve positional slots with INVALID monsters.
            // They are an internal implementation detail and do not exist in the
            // Original MonsterGroup exposed by CommunicationMod.
            if (monster.id == MonsterId::INVALID) continue;
            append_monster(index, "monster:" + std::to_string(index));
        }
    }
    result["monsters"] = monsters;
    if (bc.inputState == InputState::CARD_SELECT) {
        result["choice"] = public_combat_choice_state(bc);
    }
    return result;
}

py::dict rng_state(const Random &rng) {
    py::dict result;
    result["counter"] = rng.counter;
    result["seed0"] = rng.seed0;
    result["seed1"] = rng.seed1;
    return result;
}

int relic_counter(const RelicInstance &relic, const BattleContext &battle) {
    const auto &player = battle.player;
    switch (relic.id) {
        case RelicId::BURNING_BLOOD: return -1;
        case RelicId::HAPPY_FLOWER: return player.happyFlowerCounter;
        case RelicId::INCENSE_BURNER: return player.incenseBurnerCounter;
        case RelicId::INK_BOTTLE: return player.inkBottleCounter;
        case RelicId::KUNAI:
        case RelicId::ORNAMENTAL_FAN:
        case RelicId::SHURIKEN:
            return player.attacksPlayedThisTurn % 3;
        case RelicId::LETTER_OPENER: return player.skillsPlayedThisTurn % 3;
        // Stock decrements Neow's Lament as the battle begins, before the
        // first policy-visible combat boundary. GameContext keeps the
        // pre-battle value until BattleContext::updateRelicsOnExit, so project
        // the active value here without changing checkpoint/gameplay state.
        case RelicId::NEOWS_LAMENT: return std::max(0, relic.data - 1);
        case RelicId::NUNCHAKU: return player.nunchakuCounter;
        // Native uses -1 internally while the tenth-attack power is armed;
        // stock keeps the relic's policy-visible counter at 9 until that
        // attack is played and then resets it to 0.
        case RelicId::PEN_NIB:
            return player.penNibCounter == -1 ? 9 : player.penNibCounter;
        // Stock resets at battle start and increments atTurnStart. Native
        // BattleContext::turn is zero-based while the visible counter is one-
        // based at policy boundaries.
        case RelicId::STONE_CALENDAR: return battle.turn + 1;
        case RelicId::SUNDIAL: return player.sundialCounter;
        default: return relic.data;
    }
}

void restore_rng(Random &rng, const py::dict &value) {
    rng.counter = value["counter"].cast<std::int32_t>();
    rng.seed0 = value["seed0"].cast<std::uint64_t>();
    rng.seed1 = value["seed1"].cast<std::uint64_t>();
}

py::dict full_run_rng_state(const GameContext &gc) {
    py::dict result;
    result["ai"] = rng_state(gc.aiRng);
    result["card_random"] = rng_state(gc.cardRandomRng);
    result["card"] = rng_state(gc.cardRng);
    result["event"] = rng_state(gc.eventRng);
    result["math_util"] = rng_state(gc.mathUtilRng);
    result["merchant"] = rng_state(gc.merchantRng);
    result["misc"] = rng_state(gc.miscRng);
    result["monster_hp"] = rng_state(gc.monsterHpRng);
    result["monster"] = rng_state(gc.monsterRng);
    result["neow"] = rng_state(gc.neowRng);
    result["potion"] = rng_state(gc.potionRng);
    result["relic"] = rng_state(gc.relicRng);
    result["shuffle"] = rng_state(gc.shuffleRng);
    result["treasure"] = rng_state(gc.treasureRng);
    return result;
}

void restore_full_run_rng(GameContext &gc, const py::dict &rng) {
    restore_rng(gc.aiRng, rng["ai"].cast<py::dict>());
    restore_rng(gc.cardRandomRng, rng["card_random"].cast<py::dict>());
    restore_rng(gc.cardRng, rng["card"].cast<py::dict>());
    restore_rng(gc.eventRng, rng["event"].cast<py::dict>());
    restore_rng(gc.mathUtilRng, rng["math_util"].cast<py::dict>());
    restore_rng(gc.merchantRng, rng["merchant"].cast<py::dict>());
    restore_rng(gc.miscRng, rng["misc"].cast<py::dict>());
    restore_rng(gc.monsterHpRng, rng["monster_hp"].cast<py::dict>());
    restore_rng(gc.monsterRng, rng["monster"].cast<py::dict>());
    restore_rng(gc.neowRng, rng["neow"].cast<py::dict>());
    restore_rng(gc.potionRng, rng["potion"].cast<py::dict>());
    restore_rng(gc.relicRng, rng["relic"].cast<py::dict>());
    restore_rng(gc.shuffleRng, rng["shuffle"].cast<py::dict>());
    restore_rng(gc.treasureRng, rng["treasure"].cast<py::dict>());
}

template <typename Container>
py::list enum_values(const Container &values) {
    py::list result;
    for (const auto value : values) result.append(static_cast<int>(value));
    return result;
}

template <typename Container, typename Enum>
void restore_enum_values(Container &target, const py::handle &value) {
    target.clear();
    for (const auto item : value.cast<py::list>()) {
        target.push_back(static_cast<Enum>(item.cast<int>()));
    }
}

py::dict ordered_pool_state(const GameContext &gc) {
    py::dict result;
    result["events"] = enum_values(gc.eventList);
    result["shrines"] = enum_values(gc.shrineList);
    result["special_one_time_events"] = enum_values(gc.specialOneTimeEventList);
    result["common_relics"] = enum_values(gc.commonRelicPool);
    result["uncommon_relics"] = enum_values(gc.uncommonRelicPool);
    result["rare_relics"] = enum_values(gc.rareRelicPool);
    result["shop_relics"] = enum_values(gc.shopRelicPool);
    result["boss_relics"] = enum_values(gc.bossRelicPool);
    result["colorless_cards"] = enum_values(gc.colorlessCardPool);
    result["normal_encounters"] = enum_values(gc.monsterList);
    result["elite_encounters"] = enum_values(gc.eliteMonsterList);
    return result;
}

void restore_ordered_pools(GameContext &gc, const py::dict &pools) {
    restore_enum_values<std::vector<Event>, Event>(gc.eventList, pools["events"]);
    restore_enum_values<std::vector<Event>, Event>(gc.shrineList, pools["shrines"]);
    restore_enum_values<std::vector<Event>, Event>(
        gc.specialOneTimeEventList, pools["special_one_time_events"]);
    restore_enum_values<std::vector<RelicId>, RelicId>(gc.commonRelicPool, pools["common_relics"]);
    restore_enum_values<std::vector<RelicId>, RelicId>(gc.uncommonRelicPool, pools["uncommon_relics"]);
    restore_enum_values<std::vector<RelicId>, RelicId>(gc.rareRelicPool, pools["rare_relics"]);
    restore_enum_values<std::vector<RelicId>, RelicId>(gc.shopRelicPool, pools["shop_relics"]);
    restore_enum_values<std::vector<RelicId>, RelicId>(gc.bossRelicPool, pools["boss_relics"]);

    const auto colorless = pools["colorless_cards"].cast<py::list>();
    if (colorless.size() != gc.colorlessCardPool.size()) {
        throw std::invalid_argument("Colorless card pool has an invalid size");
    }
    for (std::size_t i = 0; i < gc.colorlessCardPool.size(); ++i) {
        gc.colorlessCardPool[i] = static_cast<CardId>(colorless[i].cast<int>());
    }
    restore_enum_values<fixed_list<MonsterEncounter, 16>, MonsterEncounter>(
        gc.monsterList, pools["normal_encounters"]);
    restore_enum_values<fixed_list<MonsterEncounter, 10>, MonsterEncounter>(
        gc.eliteMonsterList, pools["elite_encounters"]);
}

py::dict run_player_state(const GameContext &gc) {
    py::dict result;
    result["current_hp"] = gc.curHp;
    result["max_hp"] = gc.maxHp;
    result["gold"] = gc.gold;
    result["potion_count"] = gc.potionCount;
    result["potion_capacity"] = gc.potionCapacity;
    py::list potions;
    for (int i = 0; i < gc.potionCapacity; ++i) {
        potions.append(static_cast<int>(gc.potions[i]));
    }
    result["potions"] = potions;

    py::list relics;
    for (const auto &relic : gc.relics.relics) {
        py::dict value;
        value["id"] = static_cast<int>(relic.id);
        value["data"] = relic.data;
        relics.append(value);
    }
    result["relics"] = relics;

    py::list deck;
    for (const auto &card : gc.deck.cards) {
        py::dict value;
        value["id"] = static_cast<int>(card.id);
        value["upgraded"] = card.upgraded;
        value["misc"] = card.misc;
        deck.append(value);
    }
    result["deck"] = deck;
    result["bottle_indices"] = py::make_tuple(
        gc.deck.bottleIdxs[0], gc.deck.bottleIdxs[1], gc.deck.bottleIdxs[2]);
    result["blue_key"] = gc.blueKey;
    result["green_key"] = gc.greenKey;
    result["red_key"] = gc.redKey;

    return result;
}

py::dict public_inventory_state(
    const GameContext &gc, const BattleContext *battle = nullptr) {
    py::dict result;
    py::list potions;
    const int potion_capacity = battle != nullptr ? battle->potionCapacity : gc.potionCapacity;
    for (int i = 0; i < potion_capacity; ++i) {
        const auto potion = battle != nullptr ? battle->potions[i] : gc.potions[i];
        py::dict value;
        value["instance_id"] = "potion:" + std::to_string(i);
        value["content_id"] = potionEnumNames[static_cast<int>(potion)];
        value["slot"] = i;
        potions.append(value);
    }
    result["potions"] = potions;
    py::list relics;
    for (std::size_t i = 0; i < gc.relics.relics.size(); ++i) {
        const auto &relic = gc.relics.relics[i];
        py::dict value;
        value["instance_id"] = "relic:" + std::to_string(i);
        value["content_id"] = relicEnumNames[static_cast<int>(relic.id)];
        // During combat mutable relic counters live in BattleContext::player;
        // GameContext is synchronized only when the battle exits.  Project the
        // active combat value so the public FullRun boundary matches Original.
        value["counter"] = battle != nullptr
            ? relic_counter(relic, *battle)
            : relic.data;
        relics.append(value);
    }
    result["relics"] = relics;
    py::list deck;
    for (std::size_t i = 0; i < gc.deck.cards.size(); ++i) {
        deck.append(public_run_card(gc.deck.cards[i], "deck:" + std::to_string(i)));
    }
    result["deck"] = deck;
    return result;
}

void restore_run_player_state(GameContext &gc, const py::dict &state) {
    gc.curHp = state["current_hp"].cast<int>();
    gc.maxHp = state["max_hp"].cast<int>();
    gc.gold = state["gold"].cast<int>();
    gc.potionCapacity = state["potion_capacity"].cast<int>();
    if (gc.potionCapacity < 0 || gc.potionCapacity > static_cast<int>(gc.potions.size())) {
        throw std::invalid_argument("Potion capacity is outside native limits");
    }
    const auto potions = state["potions"].cast<py::list>();
    if (potions.size() != static_cast<std::size_t>(gc.potionCapacity)) {
        throw std::invalid_argument("Potion slots do not match potion capacity");
    }
    gc.potions.fill(Potion::EMPTY_POTION_SLOT);
    for (int i = 0; i < gc.potionCapacity; ++i) {
        gc.potions[i] = static_cast<Potion>(potions[i].cast<int>());
    }
    gc.potionCount = state["potion_count"].cast<int>();

    gc.relics = RelicContainer();
    for (const auto item : state["relics"].cast<py::list>()) {
        const auto value = item.cast<py::dict>();
        gc.relics.add(RelicInstance{
            static_cast<RelicId>(value["id"].cast<int>()), value["data"].cast<int>()});
    }
    gc.deck = Deck();
    for (const auto item : state["deck"].cast<py::list>()) {
        const auto value = item.cast<py::dict>();
        Card card(static_cast<CardId>(value["id"].cast<int>()));
        card.upgraded = value["upgraded"].cast<bool>();
        card.misc = value["misc"].cast<std::int16_t>();
        gc.deck.obtainRaw(card);
    }
    const auto bottles = state["bottle_indices"].cast<py::tuple>();
    if (bottles.size() != gc.deck.bottleIdxs.size()) {
        throw std::invalid_argument("Bottle index state must contain three entries");
    }
    for (std::size_t i = 0; i < gc.deck.bottleIdxs.size(); ++i) {
        gc.deck.bottleIdxs[i] = bottles[i].cast<int>();
    }
    gc.blueKey = state["blue_key"].cast<bool>();
    gc.greenKey = state["green_key"].cast<bool>();
    gc.redKey = state["red_key"].cast<bool>();
}

py::dict run_progress_state(const GameContext &gc) {
    py::dict result;
    result["outcome"] = static_cast<int>(gc.outcome);
    result["screen_state"] = static_cast<int>(gc.screenState);
    result["last_room"] = static_cast<int>(gc.lastRoom);
    result["current_room"] = static_cast<int>(gc.curRoom);
    result["current_event"] = static_cast<int>(gc.curEvent);
    result["boss"] = static_cast<int>(gc.boss);
    result["current_map_x"] = gc.curMapNodeX;
    result["current_map_y"] = gc.curMapNodeY;
    result["monster_chance"] = gc.monsterChance;
    result["shop_chance"] = gc.shopChance;
    result["treasure_chance"] = gc.treasureChance;
    result["potion_chance"] = gc.potionChance;
    result["card_rarity_factor"] = gc.cardRarityFactor;
    result["shop_remove_count"] = gc.shopRemoveCount;
    result["speedrun_pace"] = gc.speedrunPace;
    return result;
}

void restore_run_progress_state(GameContext &gc, const py::dict &state) {
    const bool registeredLegacyEvent =
        state["screen_state"].cast<int>() == static_cast<int>(ScreenState::EVENT_SCREEN) &&
        (state["current_event"].cast<int>() == static_cast<int>(Event::GOLDEN_IDOL) ||
         state["current_event"].cast<int>() == static_cast<int>(Event::THE_CLERIC));
    if (state.contains("screen_continuation_serialized") &&
            !state["screen_continuation_serialized"].cast<bool>() &&
            !registeredLegacyEvent) {
        throw std::invalid_argument("Checkpoint contains an unsupported screen continuation");
    }
    gc.outcome = static_cast<GameOutcome>(state["outcome"].cast<int>());
    gc.screenState = static_cast<ScreenState>(state["screen_state"].cast<int>());
    gc.lastRoom = static_cast<Room>(state["last_room"].cast<int>());
    gc.curRoom = static_cast<Room>(state["current_room"].cast<int>());
    gc.curEvent = static_cast<Event>(state["current_event"].cast<int>());
    gc.boss = static_cast<MonsterEncounter>(state["boss"].cast<int>());
    gc.curMapNodeX = state["current_map_x"].cast<int>();
    gc.curMapNodeY = state["current_map_y"].cast<int>();
    gc.monsterChance = state["monster_chance"].cast<float>();
    gc.shopChance = state["shop_chance"].cast<float>();
    gc.treasureChance = state["treasure_chance"].cast<float>();
    gc.potionChance = state["potion_chance"].cast<int>();
    gc.cardRarityFactor = state["card_rarity_factor"].cast<int>();
    gc.shopRemoveCount = state["shop_remove_count"].cast<int>();
    gc.speedrunPace = state["speedrun_pace"].cast<bool>();
}

py::dict run_card_state(const Card &card) {
    py::dict result;
    result["id"] = static_cast<int>(card.id);
    result["upgraded"] = card.upgraded;
    result["misc"] = card.misc;
    return result;
}

Card restore_run_card(const py::dict &state) {
    Card card(static_cast<CardId>(state["id"].cast<int>()));
    card.upgraded = state["upgraded"].cast<bool>();
    card.misc = state["misc"].cast<std::int16_t>();
    return card;
}

py::dict rewards_state(const Rewards &rewards) {
    py::dict result;
    py::list gold;
    for (int i = 0; i < rewards.goldRewardCount; ++i) gold.append(rewards.gold[i]);
    result["gold"] = gold;
    py::list card_rewards;
    for (int i = 0; i < rewards.cardRewardCount; ++i) {
        py::list cards;
        for (const auto &card : rewards.cardRewards[i]) cards.append(run_card_state(card));
        card_rewards.append(cards);
    }
    result["card_rewards"] = card_rewards;
    py::list relics;
    for (int i = 0; i < rewards.relicCount; ++i) {
        relics.append(static_cast<int>(rewards.relics[i]));
    }
    result["relics"] = relics;
    py::list potions;
    for (int i = 0; i < rewards.potionCount; ++i) {
        potions.append(static_cast<int>(rewards.potions[i]));
    }
    result["potions"] = potions;
    result["emerald_key"] = rewards.emeraldKey;
    result["sapphire_key"] = rewards.sapphireKey;
    return result;
}

Rewards restore_rewards(const py::dict &state) {
    Rewards result;
    for (const auto value : state["gold"].cast<py::list>()) {
        result.addGold(value.cast<int>());
    }
    for (const auto reward_value : state["card_rewards"].cast<py::list>()) {
        CardReward reward;
        for (const auto card_value : reward_value.cast<py::list>()) {
            reward.push_back(restore_run_card(card_value.cast<py::dict>()));
        }
        result.addCardReward(reward);
    }
    for (const auto value : state["relics"].cast<py::list>()) {
        result.addRelic(static_cast<RelicId>(value.cast<int>()));
    }
    for (const auto value : state["potions"].cast<py::list>()) {
        result.addPotion(static_cast<Potion>(value.cast<int>()));
    }
    result.emeraldKey = state["emerald_key"].cast<bool>();
    result.sapphireKey = state["sapphire_key"].cast<bool>();
    return result;
}

py::dict shop_state(const Shop &shop) {
    py::dict result;
    py::list cards;
    for (const auto &card : shop.cards) cards.append(run_card_state(card));
    result["cards"] = cards;
    py::list potions;
    for (const auto potion : shop.potions) potions.append(static_cast<int>(potion));
    result["potions"] = potions;
    py::list relics;
    for (const auto relic : shop.relics) relics.append(static_cast<int>(relic));
    result["relics"] = relics;
    py::list prices;
    for (const auto price : shop.prices) prices.append(price);
    result["prices"] = prices;
    result["remove_cost"] = shop.removeCost;
    return result;
}

py::dict public_screen_state(const GameContext &gc) {
    py::dict result;
    if (gc.screenState == ScreenState::EVENT_SCREEN && gc.curEvent == Event::GOLDEN_IDOL) {
        result["phase"] = gc.hasRelic(RelicId::GOLDEN_IDOL) ? 1 : 0;
    } else if (gc.screenState == ScreenState::EVENT_SCREEN && gc.curEvent == Event::THE_CLERIC) {
        result["phase"] = 0;
    } else if (gc.screenState == ScreenState::EVENT_SCREEN && gc.curEvent == Event::MATCH_AND_KEEP) {
        py::list slots;
        for (int index = 0; index < gc.info.toSelectCards.size(); ++index) {
            const auto &slot = gc.info.toSelectCards[index];
            py::dict value;
            value["instance_id"] = "match-slot:" + std::to_string(index);
            value["content_id"] = slot.deckIdx == -1
                ? "HIDDEN_CARD" : getCardEnumName(slot.card.id);
            value["removed"] = slot.deckIdx == 0;
            value["known"] = slot.deckIdx != -1;
            slots.append(value);
        }
        result["match_slots"] = slots;
        result["attempts_remaining"] = gc.info.eventData;
    } else if (gc.screenState == ScreenState::CARD_SELECT) {
        const char *select_type = "INVALID";
        switch (gc.info.selectScreenType) {
            case CardSelectScreenType::TRANSFORM: select_type = "TRANSFORM"; break;
            case CardSelectScreenType::TRANSFORM_UPGRADE: select_type = "TRANSFORM_UPGRADE"; break;
            case CardSelectScreenType::UPGRADE: select_type = "UPGRADE"; break;
            case CardSelectScreenType::REMOVE: select_type = "REMOVE"; break;
            case CardSelectScreenType::DUPLICATE: select_type = "DUPLICATE"; break;
            case CardSelectScreenType::OBTAIN: select_type = "OBTAIN"; break;
            case CardSelectScreenType::BOTTLE: select_type = "BOTTLE"; break;
            case CardSelectScreenType::BONFIRE_SPIRITS: select_type = "BONFIRE_SPIRITS"; break;
            default: break;
        }
        result["select_type"] = select_type;
        result["select_count"] = gc.info.toSelectCount;
        result["from_rewards"] = gc.info.cardSelectFromRewards;
        py::list options;
        for (int index = 0; index < gc.info.toSelectCards.size(); ++index) {
            const auto &option = gc.info.toSelectCards[index];
            py::dict value;
            value["instance_id"] = "select-card:" + std::to_string(index);
            value["content_id"] = getCardEnumName(option.card.id);
            value["upgrades"] = option.card.getUpgraded();
            value["deck_index"] = option.deckIdx;
            options.append(value);
        }
        result["card_options"] = options;
    } else if (gc.screenState == ScreenState::REWARDS) {
        const auto &rewards = gc.info.rewardsContainer;
        py::list card_rewards;
        for (int group = 0; group < rewards.cardRewardCount; ++group) {
            py::list cards;
            for (const auto &card : rewards.cardRewards[group]) {
                py::dict value;
                value["content_id"] = getCardEnumName(card.id);
                value["upgrades"] = card.getUpgraded();
                cards.append(value);
            }
            card_rewards.append(cards);
        }
        result["card_rewards"] = card_rewards;
        py::list gold;
        for (int i = 0; i < rewards.goldRewardCount; ++i) gold.append(rewards.gold[i]);
        result["gold"] = gold;
        py::list relics;
        for (int i = 0; i < rewards.relicCount; ++i) {
            relics.append(relicEnumNames[static_cast<int>(rewards.relics[i])]);
        }
        result["relics"] = relics;
        py::list potions;
        for (int i = 0; i < rewards.potionCount; ++i) {
            potions.append(potionEnumNames[static_cast<int>(rewards.potions[i])]);
        }
        result["potions"] = potions;
        result["emerald_key"] = rewards.emeraldKey;
        result["sapphire_key"] = rewards.sapphireKey;
    } else if (gc.screenState == ScreenState::SHOP_ROOM) {
        const auto &shop = gc.info.shop;
        py::list cards;
        for (const auto &card : shop.cards) {
            py::dict value;
            value["content_id"] = getCardEnumName(card.id);
            value["upgrades"] = card.getUpgraded();
            cards.append(value);
        }
        result["cards"] = cards;
        py::list relics;
        for (const auto relic : shop.relics) {
            relics.append(relicEnumNames[static_cast<int>(relic)]);
        }
        result["relics"] = relics;
        py::list potions;
        for (const auto potion : shop.potions) {
            potions.append(potionEnumNames[static_cast<int>(potion)]);
        }
        result["potions"] = potions;
        py::list prices;
        for (const auto price : shop.prices) prices.append(price);
        result["prices"] = prices;
        result["remove_cost"] = shop.removeCost;
    } else if (gc.screenState == ScreenState::BOSS_RELIC_REWARDS) {
        py::list relics;
        for (const auto relic : gc.info.bossRelics) {
            relics.append(relicEnumNames[static_cast<int>(relic)]);
        }
        result["boss_relics"] = relics;
    }
    return result;
}

Shop restore_shop(const py::dict &state) {
    Shop result;
    const auto cards = state["cards"].cast<py::list>();
    const auto potions = state["potions"].cast<py::list>();
    const auto relics = state["relics"].cast<py::list>();
    const auto prices = state["prices"].cast<py::list>();
    if (cards.size() != 7 || potions.size() != 3 || relics.size() != 3 || prices.size() != 13) {
        throw std::invalid_argument("Shop checkpoint has invalid fixed-array sizes");
    }
    for (int i = 0; i < 7; ++i) result.cards[i] = restore_run_card(cards[i].cast<py::dict>());
    for (int i = 0; i < 3; ++i) {
        result.potions[i] = static_cast<Potion>(potions[i].cast<int>());
        result.relics[i] = static_cast<RelicId>(relics[i].cast<int>());
    }
    for (int i = 0; i < 13; ++i) result.prices[i] = prices[i].cast<int>();
    result.removeCost = state["remove_cost"].cast<int>();
    return result;
}

py::dict screen_info_state(const GameContext &gc) {
    py::dict result;
    result["screen_state"] = static_cast<int>(gc.screenState);
    result["complete"] = true;
    switch (gc.screenState) {
        case ScreenState::EVENT_SCREEN: {
            // ScreenStateInfo is reused across rooms.  Only expose eventData
            // for events whose state machine actually reads it; otherwise a
            // stale value from an earlier event makes equivalent checkpoints
            // compare unequal.
            const bool uses_event_data =
                gc.curEvent == Event::CURSED_TOME ||
                gc.curEvent == Event::COLOSSEUM ||
                gc.curEvent == Event::SCRAP_OOZE;
            result["event_data"] = uses_event_data ? gc.info.eventData : 0;
            if (gc.curEvent == Event::NEOW) {
                py::list options;
                for (const auto &option : gc.info.neowRewards) {
                    py::dict value;
                    value["bonus"] = static_cast<int>(option.r);
                    value["drawback"] = static_cast<int>(option.d);
                    options.append(value);
                }
                result["neow_options"] = options;
            } else if (gc.curEvent == Event::GOLDEN_IDOL) {
                // Registered exact continuation. Golden Idol's second screen
                // is represented by possession of the relic; the remaining
                // choices read only these two setup-time values. Keep this
                // deliberately event-specific instead of claiming arbitrary
                // event state is serializable.
                result["hp_amount_0"] = gc.info.hpAmount0;
                result["hp_amount_1"] = gc.info.hpAmount1;
                result["continuation"] = "map";
            } else if (gc.curEvent == Event::THE_CLERIC) {
                result["hp_amount_0"] = gc.info.hpAmount0;
                result["continuation"] = "map";
            } else {
                // Each event owns additional phase fields. Their exhaustive
                // schema belongs to the run-content step, so reject exact
                // continuation claims for them now.
                result["complete"] = false;
            }
            break;
        }
        case ScreenState::REWARDS:
            result["rewards"] = rewards_state(gc.info.rewardsContainer);
            result["stolen_gold"] = gc.info.stolenGold;
            result["continuation"] = (
                gc.curRoom == Room::BOSS && (gc.act == 1 || gc.act == 2)
            ) ? "boss_treasure" : "map";
            break;
        case ScreenState::BOSS_RELIC_REWARDS: {
            py::list relics;
            for (const auto relic : gc.info.bossRelics) relics.append(static_cast<int>(relic));
            result["boss_relics"] = relics;
            break;
        }
        case ScreenState::CARD_SELECT: {
            // The callback after selection depends on the event/reward/shop
            // that opened this screen. The combat-reward bottle path is
            // explicitly represented and can therefore be restored exactly.
            const bool restorableRewardBottle =
                gc.info.cardSelectFromRewards &&
                gc.info.selectScreenType == CardSelectScreenType::BOTTLE;
            result["complete"] = restorableRewardBottle;
            result["transform_rng"] = static_cast<int>(gc.info.transformRng);
            result["select_type"] = static_cast<int>(gc.info.selectScreenType);
            result["select_count"] = gc.info.toSelectCount;
            result["from_rewards"] = gc.info.cardSelectFromRewards;
            if (restorableRewardBottle) {
                result["rewards"] = rewards_state(gc.info.rewardsContainer);
            }
            py::list available;
            for (const auto &selected : gc.info.toSelectCards) {
                py::dict value;
                value["card"] = run_card_state(selected.card);
                value["deck_index"] = selected.deckIdx;
                available.append(value);
            }
            result["available"] = available;
            py::list selected;
            for (const auto &choice : gc.info.haveSelectedCards) {
                py::dict value;
                value["card"] = run_card_state(choice.card);
                value["deck_index"] = choice.deckIdx;
                selected.append(value);
            }
            result["selected"] = selected;
            break;
        }
        case ScreenState::TREASURE_ROOM:
            result["have_gold"] = gc.info.haveGold;
            result["chest_size"] = static_cast<int>(gc.info.chestSize);
            result["relic_tier"] = static_cast<int>(gc.info.tier);
            result["continuation"] = "map";
            break;
        case ScreenState::SHOP_ROOM:
            result["shop"] = shop_state(gc.info.shop);
            result["continuation"] = "map";
            break;
        case ScreenState::REST_ROOM:
            result["continuation"] = "map";
            break;
        case ScreenState::MAP_SCREEN:
        case ScreenState::INVALID:
            break;
        case ScreenState::BATTLE:
            result["encounter"] = static_cast<int>(gc.info.encounter);
            break;
    }
    return result;
}

void restore_screen_info(GameContext &gc, const py::dict &state) {
    if (state["screen_state"].cast<int>() != static_cast<int>(gc.screenState)) {
        throw std::invalid_argument("Screen info does not match progress screen state");
    }
    const bool registeredLegacyEvent =
        gc.screenState == ScreenState::EVENT_SCREEN &&
        (gc.curEvent == Event::GOLDEN_IDOL || gc.curEvent == Event::THE_CLERIC);
    if (!state["complete"].cast<bool>() && !registeredLegacyEvent) {
        throw std::invalid_argument("Checkpoint contains an unsupported event continuation");
    }
    gc.info = ScreenStateInfo();
    switch (gc.screenState) {
        case ScreenState::EVENT_SCREEN:
            gc.info.eventData = state["event_data"].cast<int>();
            if (gc.curEvent == Event::NEOW) {
                const auto options = state["neow_options"].cast<py::list>();
                if (options.size() != gc.info.neowRewards.size()) {
                    throw std::invalid_argument("Neow checkpoint must contain four options");
                }
                for (std::size_t i = 0; i < gc.info.neowRewards.size(); ++i) {
                    const auto value = options[i].cast<py::dict>();
                    gc.info.neowRewards[i] = Neow::Option{
                        static_cast<Neow::Bonus>(value["bonus"].cast<int>()),
                        static_cast<Neow::Drawback>(value["drawback"].cast<int>())};
                }
            } else if (gc.curEvent == Event::GOLDEN_IDOL) {
                const bool unfavorable = gc.ascension >= 15;
                gc.info.hpAmount0 = state.contains("hp_amount_0")
                    ? state["hp_amount_0"].cast<int>()
                    : gc.fractionMaxHp(unfavorable ? 0.35f : 0.25f);
                gc.info.hpAmount1 = state.contains("hp_amount_1")
                    ? state["hp_amount_1"].cast<int>()
                    : gc.fractionMaxHp(unfavorable ? 0.10f : 0.08f);
                gc.regainControlAction = [](GameContext &context) {
                    context.screenState = ScreenState::MAP_SCREEN;
                    context.regainControlAction = nullptr;
                };
            } else if (gc.curEvent == Event::THE_CLERIC) {
                gc.info.hpAmount0 = state.contains("hp_amount_0")
                    ? state["hp_amount_0"].cast<int>()
                    : gc.fractionMaxHp(0.25f);
                gc.regainControlAction = [](GameContext &context) {
                    context.screenState = ScreenState::MAP_SCREEN;
                    context.regainControlAction = nullptr;
                };
            } else {
                throw std::invalid_argument(
                    "Checkpoint event is not registered for exact continuation");
            }
            break;
        case ScreenState::REWARDS:
            gc.info.rewardsContainer = restore_rewards(state["rewards"].cast<py::dict>());
            gc.info.stolenGold = state["stolen_gold"].cast<int>();
            if (gc.curRoom == Room::BOSS && (gc.act == 1 || gc.act == 2)) {
                gc.regainControlAction = [](GameContext &context) {
                    context.enterBossTreasureRoom();
                };
            } else {
                gc.regainControlAction = [](GameContext &context) {
                    context.screenState = ScreenState::MAP_SCREEN;
                    context.regainControlAction = nullptr;
                };
            }
            break;
        case ScreenState::BOSS_RELIC_REWARDS: {
            const auto relics = state["boss_relics"].cast<py::list>();
            if (relics.size() != 3) throw std::invalid_argument("Boss relic checkpoint must contain three relics");
            for (int i = 0; i < 3; ++i) gc.info.bossRelics[i] = static_cast<RelicId>(relics[i].cast<int>());
            gc.regainControlAction = [](GameContext &context) {
                context.transitionToAct(context.act + 1);
            };
            break;
        }
        case ScreenState::CARD_SELECT: {
            gc.info.transformRng = static_cast<RngReference>(state["transform_rng"].cast<int>());
            gc.info.selectScreenType = static_cast<CardSelectScreenType>(state["select_type"].cast<int>());
            gc.info.toSelectCount = state["select_count"].cast<int>();
            gc.info.cardSelectFromRewards = state.contains("from_rewards")
                && state["from_rewards"].cast<bool>();
            if (gc.info.cardSelectFromRewards) {
                if (!state.contains("rewards")) {
                    throw std::invalid_argument(
                        "Reward-origin card select checkpoint is missing rewards");
                }
                gc.info.rewardsContainer = restore_rewards(state["rewards"].cast<py::dict>());
                gc.regainControlAction = [](GameContext &context) {
                    context.screenState = ScreenState::REWARDS;
                    context.regainControlAction = [](GameContext &next) {
                        next.screenState = ScreenState::MAP_SCREEN;
                    };
                };
            }
            for (const auto item : state["available"].cast<py::list>()) {
                const auto value = item.cast<py::dict>();
                gc.info.toSelectCards.push_back(SelectScreenCard(
                    restore_run_card(value["card"].cast<py::dict>()),
                    value["deck_index"].cast<int>()));
            }
            for (const auto item : state["selected"].cast<py::list>()) {
                const auto value = item.cast<py::dict>();
                gc.info.haveSelectedCards.push_back(SelectScreenCard(
                    restore_run_card(value["card"].cast<py::dict>()),
                    value["deck_index"].cast<int>()));
            }
            break;
        }
        case ScreenState::TREASURE_ROOM:
            gc.info.haveGold = state["have_gold"].cast<bool>();
            gc.info.chestSize = static_cast<ChestSize>(state["chest_size"].cast<int>());
            gc.info.tier = static_cast<RelicTier>(state["relic_tier"].cast<int>());
            break;
        case ScreenState::SHOP_ROOM:
            gc.info.shop = restore_shop(state["shop"].cast<py::dict>());
            break;
        case ScreenState::BATTLE:
            gc.info.encounter = static_cast<MonsterEncounter>(
                state["encounter"].cast<int>());
            gc.regainControlAction = [](GameContext &context) {
                context.afterBattle();
            };
            break;
        default:
            break;
    }
    if (state.contains("continuation")) {
        const auto continuation = state["continuation"].cast<std::string>();
        if (continuation == "boss_treasure" ||
                (continuation == "map" && gc.curRoom == Room::BOSS &&
                 (gc.act == 1 || gc.act == 2))) {
            // Older exact checkpoints used the generic "map" label here.
            // The public room/act fields disambiguate that legacy value.
            gc.regainControlAction = [](GameContext &context) {
                context.enterBossTreasureRoom();
            };
        } else if (continuation == "map") {
            gc.regainControlAction = [](GameContext &context) {
                context.screenState = ScreenState::MAP_SCREEN;
            };
        } else {
            throw std::invalid_argument("Unknown run-screen continuation: " + continuation);
        }
    }
}

py::list run_legal_actions(const GameContext &gc) {
    py::list result;
    for (const auto &action : search::GameAction::getAllActionsInState(gc)) {
        if (!action.isValidAction(gc)) continue;
        py::dict value;
        value["bits"] = action.bits;
        value["idx1"] = action.getIdx1();
        value["idx2"] = action.getIdx2();
        value["idx3"] = action.getIdx3();
        value["potion"] = action.isPotionAction();
        value["potion_discard"] = action.isPotionDiscard();
        value["reward_type"] = static_cast<int>(action.getRewardsActionType());
        result.append(value);
    }
    return result;
}

py::dict combat_action_state(
        const search::Action &action,
        const BattleContext *battle = nullptr) {
    py::dict value;
    value["bits"] = action.bits;
    value["action_type"] = static_cast<int>(action.getActionType());
    value["source_index"] = action.getSourceIdx();
    value["target_index"] = action.getTargetIdx();
    value["domain"] = "COMBAT";
    bool requires_target = false;
    if (battle != nullptr && action.getActionType() == search::ActionType::CARD &&
            action.getSourceIdx() >= 0 && action.getSourceIdx() < battle->cards.cardsInHand) {
        requires_target = battle->cards.hand[action.getSourceIdx()].requiresTarget();
    } else if (battle != nullptr && action.getActionType() == search::ActionType::POTION &&
            action.getSourceIdx() >= 0 && action.getSourceIdx() < battle->potionCapacity) {
        requires_target = action.getTargetIdx() != 6
            && potionRequiresTarget(battle->potions[action.getSourceIdx()]);
    }
    value["requires_target"] = requires_target;
    if (action.getActionType() == search::ActionType::MULTI_CARD_SELECT) {
        py::list selected;
        for (const auto index : action.getSelectedIdxs()) selected.append(index);
        value["selected_indices"] = selected;
    }
    return value;
}

py::list combat_legal_actions(const BattleContext &battle) {
    py::list result;
    if (battle.outcome != Outcome::UNDECIDED) return result;
    if (battle.inputState == InputState::CARD_SELECT) {
        for (const auto &action : search::Action::enumerateCardSelectActions(battle)) {
            if (action.isValidAction(battle)) result.append(combat_action_state(action, &battle));
        }
        for (int source = 0; source < battle.potionCapacity; ++source) {
            const auto potion = battle.potions[source];
            if (potion == Potion::INVALID || potion == Potion::EMPTY_POTION_SLOT) continue;
            if (potionRequiresTarget(potion)) {
                for (int target = 0; target < battle.monsters.monsterCount; ++target) {
                    const search::Action action(search::ActionType::POTION, source, target);
                    if (action.isValidAction(battle)) {
                        result.append(combat_action_state(action, &battle));
                    }
                }
            } else {
                const search::Action action(search::ActionType::POTION, source, 0);
                if (action.isValidAction(battle)) {
                    result.append(combat_action_state(action, &battle));
                }
            }
            const search::Action discard(search::ActionType::POTION, source, 6);
            if (discard.isValidAction(battle)) {
                result.append(combat_action_state(discard, &battle));
            }
        }
        return result;
    }
    if (battle.inputState != InputState::PLAYER_NORMAL) return result;

    for (int source = 0; source < battle.cards.cardsInHand; ++source) {
        const auto &card = battle.cards.hand[source];
        if (card.requiresTarget()) {
            for (int target = 0; target < battle.monsters.monsterCount; ++target) {
                const search::Action action(search::ActionType::CARD, source, target);
                if (action.isValidAction(battle)) result.append(combat_action_state(action, &battle));
            }
        } else {
            const search::Action action(search::ActionType::CARD, source, 0);
            if (action.isValidAction(battle)) result.append(combat_action_state(action, &battle));
        }
    }
    for (int source = 0; source < battle.potionCapacity; ++source) {
        const auto potion = battle.potions[source];
        if (potion == Potion::INVALID || potion == Potion::EMPTY_POTION_SLOT) continue;
        if (potionRequiresTarget(potion)) {
            for (int target = 0; target < battle.monsters.monsterCount; ++target) {
                const search::Action action(search::ActionType::POTION, source, target);
                if (action.isValidAction(battle)) result.append(combat_action_state(action, &battle));
            }
        } else {
            const search::Action action(search::ActionType::POTION, source, 0);
            if (action.isValidAction(battle)) result.append(combat_action_state(action, &battle));
        }
        const search::Action discard(search::ActionType::POTION, source, 6);
        if (discard.isValidAction(battle)) result.append(combat_action_state(discard, &battle));
    }
    result.append(combat_action_state(search::Action(search::ActionType::END_TURN), &battle));
    return result;
}

template <typename Container>
py::list card_list(const Container &cards) {
    py::list result;
    for (const auto &card : cards) result.append(card_dict(card));
    return result;
}

class LightspeedBattle {
public:
    void reset(
        std::uint64_t seed,
        const std::string &encounter,
        int ascension,
        const std::vector<std::string> &deck,
        const std::vector<std::string> &relics = {},
        bool replace_relics = false) {
        gc_ = std::make_unique<GameContext>(CharacterClass::IRONCLAD, seed, ascension);
        install_combat_reward_callback();
        if (replace_relics) {
            gc_->relics = RelicContainer();
            for (const auto &spec : relics) {
                const auto separator = spec.rfind('@');
                const auto name = separator == std::string::npos
                    ? spec : spec.substr(0, separator);
                gc_->obtainRelic(parse_relic(name));
                if (separator != std::string::npos) {
                    gc_->relics.getRelicValueRef(parse_relic(name)) =
                        std::stoi(spec.substr(separator + 1));
                }
            }
        }
        if (!deck.empty()) {
            gc_->deck = Deck();
            for (const auto &card : deck) gc_->deck.obtainRaw(parse_card(card));
        }
        gc_->floorNum = 1;
        gc_->curRoom = Room::MONSTER;
        // AbstractDungeon resets miscRng from seed + floor immediately before
        // constructing a room.  A directly-created GameContext still contains
        // the run-start (seed-only) stream, which changes variable encounter
        // composition such as Small Slimes and Two Louse.
        gc_->miscRng = Random(seed + static_cast<std::uint64_t>(gc_->floorNum));
        bc_ = std::make_unique<BattleContext>();
        bc_->init(*gc_, parse_encounter(encounter));
        finalized_ = false;
        escaped_ = false;
        multi_select_bits_ = 0;
        multi_select_indices_.clear();
        has_pre_step_moves_ = false;
    }

    void set_card_piles(
        const std::vector<std::string> &hand,
        const std::vector<std::string> &draw,
        const std::vector<std::string> &discard,
        const std::vector<std::string> &exhaust) {
        require_reset();
        if (hand.size() > CardManager::MAX_HAND_SIZE) {
            throw std::invalid_argument("Opening hand exceeds native hand limit");
        }
        if (hand.size() + draw.size() + discard.size() + exhaust.size()
                > CardManager::MAX_GROUP_SIZE) {
            throw std::invalid_argument("Combat card count exceeds native group limit");
        }

        bc_->cards = CardManager();
        auto instance = [](const std::string &spec) {
            return CardInstance(parse_card(spec));
        };
        for (const auto &spec : hand) {
            bc_->cards.createTempCardInHand(instance(spec));
        }
        for (const auto &spec : draw) {
            bc_->cards.createTempCardInDrawPile(
                static_cast<int>(bc_->cards.drawPile.size()), instance(spec));
        }
        for (const auto &spec : discard) {
            bc_->cards.createTempCardInDiscard(instance(spec));
        }
        for (const auto &spec : exhaust) {
            auto card = instance(spec);
            card.uniqueId = static_cast<std::int16_t>(bc_->cards.nextUniqueCardId++);
            bc_->cards.exhaustPile.push_back(card);
        }
    }

    void reset_card_probe(
        std::uint64_t seed,
        const std::string &card_id,
        bool upgraded) {
        auto probe = CardInstance(parse_card(card_id));
        std::vector<std::string> relics{"Burning Blood"};
        if (probe.getType() == CardType::STATUS) {
            relics.emplace_back("Medical Kit");
        } else if (probe.getType() == CardType::CURSE) {
            relics.emplace_back("Blue Candle");
        }
        reset(seed, "CULTIST", 0, {}, relics, true);
        const auto spec = card_id + (upgraded && probe.canUpgrade() ? "+" : "");
        set_card_piles(
            {spec, "Strike_R"}, {"Defend_R", "Strike_R"},
            {"Defend_R"}, {"Defend_R"});

        auto &player = bc_->player;
        player.curHp = 80;
        player.maxHp = 80;
        player.block = 0;
        player.energy = 4;
        player.justAppliedBits = 0;
        player.statusBits0 = 0;
        player.statusBits1 = 0;
        player.statusMap.clear();
        player.powerOrder.clear();
        gc_->curHp = 80;
        gc_->maxHp = 80;

        auto &monster = bc_->monsters.arr[0];
        monster.curHp = 999;
        monster.maxHp = 999;
        monster.block = 0;
        monster.resetAllStatusEffects();
        monster.halfDead = false;
        monster.isEscapingB = false;
        monster.escapeNext = false;
        monster.setMove(MonsterMoveId::CULTIST_DARK_STRIKE);
        bc_->actionQueue.clear();
        bc_->cardQueue.clear();
        bc_->inputState = InputState::PLAYER_NORMAL;
    }

    void apply_scenario(const std::string &scenario) {
        require_reset();
        auto &player = bc_->player;
        player.block = 0;
        player.energy = 3;
        player.justAppliedBits = 0;
        player.statusBits0 = 0;
        player.statusBits1 = 0;
        player.statusMap.clear();
        player.powerOrder.clear();

        if (scenario == "retain_ethereal") {
            set_card_piles(
                {"Strike_R", "Ghostly Armor", "Dazed", "Defend_R"},
                {}, {}, {});
            bc_->cards.hand[0].retain = true;
            player.buff<PS::ESTABLISHMENT>(1);
            player.buff<PS::EQUILIBRIUM>(1);
        } else if (scenario == "duration_weak") {
            set_card_piles({"Defend_R"}, {"Strike_R"}, {}, {});
            player.debuff<PS::WEAK>(2, true);
        } else if (scenario == "damage_buffer_intangible") {
            set_card_piles({"Defend_R"}, {}, {}, {});
            player.block = 3;
            player.buff<PS::INTANGIBLE>(1);
            player.buff<PS::BUFFER>(1);
            if (!gc_->relics.has(RelicId::TORII)) {
                gc_->relics.add({RelicId::TORII, -1});
            }
            if (!gc_->relics.has(RelicId::TUNGSTEN_ROD)) {
                gc_->relics.add({RelicId::TUNGSTEN_ROD, -1});
            }
            player.setHasRelic<RelicId::TORII>(true);
            player.setHasRelic<RelicId::TUNGSTEN_ROD>(true);
        } else {
            throw std::invalid_argument("Unknown oracle scenario: " + scenario);
        }
    }

    void set_player_health(int current_hp, int max_hp) {
        require_reset();
        if (max_hp <= 0 || current_hp < 0 || current_hp > max_hp) {
            throw std::invalid_argument("Invalid player health");
        }
        bc_->player.curHp = current_hp;
        bc_->player.maxHp = max_hp;
        gc_->curHp = current_hp;
        gc_->maxHp = max_hp;
    }

    void set_potions(const std::vector<std::string> &potions) {
        require_reset();
        if (potions.size() > bc_->potions.size()) {
            throw std::invalid_argument("Potion count exceeds native slot limit");
        }
        bc_->potionCapacity = std::max(3, static_cast<int>(potions.size()));
        bc_->potionCount = 0;
        for (auto &potion : bc_->potions) potion = Potion::EMPTY_POTION_SLOT;
        for (std::size_t index = 0; index < potions.size(); ++index) {
            if (normalized(potions[index]) != "POTIONSLOT" &&
                    normalized(potions[index]) != "EMPTYPOTIONSLOT") {
                bc_->potions[index] = parse_potion(potions[index]);
                ++bc_->potionCount;
            }
        }
    }

    void set_rng_state(const py::dict &rng) {
        require_reset();
        // Preserve every run-level stream at a combat checkpoint.  The six
        // streams copied into BattleContext must be restored there as well,
        // because combat advances those copies until exitBattle writes them
        // back into GameContext.
        restore_full_run_rng(*gc_, rng);
        restore_rng(bc_->aiRng, rng["ai"].cast<py::dict>());
        restore_rng(bc_->monsterHpRng, rng["monster_hp"].cast<py::dict>());
        restore_rng(bc_->shuffleRng, rng["shuffle"].cast<py::dict>());
        restore_rng(bc_->cardRandomRng, rng["card_random"].cast<py::dict>());
        restore_rng(bc_->miscRng, rng["misc"].cast<py::dict>());
        restore_rng(bc_->potionRng, rng["potion"].cast<py::dict>());
    }

    void set_discovery_retrieval_updates(int updates) {
        require_reset();
        if (updates < 1 || updates > 120 ||
                bc_->inputState != InputState::CARD_SELECT ||
                bc_->cardSelectInfo.cardSelectTask != CardSelectTask::DISCOVERY ||
                !bc_->cardSelectInfo.discoveryRerollOnRetrieve) {
            throw std::invalid_argument(
                "Discovery timing evidence is invalid at the card probe boundary");
        }
        bc_->cardSelectInfo.discoveryRetrievalUpdates = updates;
    }

    void load_checkpoint(const py::dict &checkpoint) {
        const auto game = checkpoint["game_state"].cast<py::dict>();
        const auto combat = game["combat_state"].cast<py::dict>();
        const auto input_state = game["input_state"].cast<std::string>();
        const bool terminal_loss = game["outcome"].cast<std::string>() == "PLAYER_LOSS";
        if (input_state != "PLAYER_NORMAL" && input_state != "CARD_SELECT" &&
                !(terminal_loss && input_state == "INTERNAL")) {
            throw std::invalid_argument(
                "Checkpoint restore requires an agent-facing combat input state");
        }

        reset(
            game["seed"].cast<std::uint64_t>(),
            game["encounter"].cast<std::string>(),
            game["ascension_level"].cast<int>(),
            {}, {}, false);
        gc_->act = game["act"].cast<int>();
        gc_->floorNum = game["floor"].cast<int>();
        bc_->floorNum = game["floor"].cast<int>();

        restore_relics(game);

        restore_player(combat["player"].cast<py::dict>());
        restore_cards(combat);
        restore_monsters(combat);

        bc_->turn = combat["turn"].cast<int>() - 1;
        const auto combat_internal = combat["_internal"].cast<py::dict>();
        bc_->cards.nextUniqueCardId = combat_internal["next_unique_card_id"].cast<int>();
        const auto stasis_cards = combat_internal["stasis_cards"].cast<py::list>();
        if (stasis_cards.size() != bc_->cards.stasisCards.size()) {
            throw std::invalid_argument("Combat checkpoint has invalid stasis card count");
        }
        for (int index = 0; index < static_cast<int>(stasis_cards.size()); ++index) {
            bc_->cards.stasisCards[index] = stasis_cards[index].is_none()
                ? CardInstance(CardId::INVALID)
                : restore_card(stasis_cards[index].cast<py::dict>());
        }
        if (combat_internal.contains("slime_split_ghosts")) {
            for (const auto item : combat_internal["slime_split_ghosts"].cast<py::list>()) {
                const auto ghost = item.cast<py::dict>();
                const int slot = ghost["slot"].cast<int>();
                if (slot < 4 || slot >= static_cast<int>(bc_->monsters.arr.size())) {
                    throw std::invalid_argument("Slime split ghost slot is out of range");
                }
                Monster monster;
                monster.idx = slot;
                monster.id = static_cast<MonsterId>(ghost["id"].cast<int>());
                monster.curHp = ghost["current_hp"].cast<int>();
                monster.maxHp = ghost["max_hp"].cast<int>();
                monster.block = ghost["block"].cast<int>();
                monster.moveHistory[0] = static_cast<MonsterMoveId>(ghost["move_current"].cast<int>());
                monster.moveHistory[1] = static_cast<MonsterMoveId>(ghost["move_previous"].cast<int>());
                monster.statusBits = ghost["status_bits"].cast<std::uint64_t>();
                monster.artifact = ghost["artifact"].cast<int>();
                monster.blockReturn = ghost["block_return"].cast<int>();
                monster.choked = ghost["choked"].cast<int>();
                monster.corpseExplosion = ghost["corpse_explosion"].cast<int>();
                monster.lockOn = ghost["lock_on"].cast<int>();
                monster.mark = ghost["mark"].cast<int>();
                monster.metallicize = ghost["metallicize"].cast<int>();
                monster.platedArmor = ghost["plated_armor"].cast<int>();
                monster.poison = ghost["poison"].cast<int>();
                monster.regen = ghost["regen"].cast<int>();
                monster.shackled = ghost["shackled"].cast<int>();
                monster.strength = ghost["strength"].cast<int>();
                monster.vulnerable = ghost["vulnerable"].cast<int>();
                monster.weak = ghost["weak"].cast<int>();
                monster.uniquePower0 = ghost["unique_power0"].cast<int>();
                monster.uniquePower1 = ghost["unique_power1"].cast<int>();
                monster.miscInfo = ghost["misc_info"].cast<int>();
                monster.halfDead = ghost["half_dead"].cast<bool>();
                monster.isEscapingB = ghost["is_escaping"].cast<bool>();
                monster.escapeNext = ghost["escape_next"].cast<bool>();
                for (const auto power : ghost["power_order"].cast<py::list>()) {
                    monster.powerOrder.push_back(
                        static_cast<MonsterStatus>(power.cast<int>()));
                }
                bc_->monsters.arr[slot] = monster;
            }
        }
        if (combat_internal.contains("gremlin_leader_ghosts")) {
            const auto ghosts = combat_internal["gremlin_leader_ghosts"].cast<py::list>();
            if (ghosts.size() > bc_->gremlinLeaderGhosts.size()) {
                throw std::invalid_argument("Too many Gremlin Leader ghost snapshots");
            }
            bc_->gremlinLeaderGhostCount = static_cast<int>(ghosts.size());
            for (int index = 0; index < bc_->gremlinLeaderGhostCount; ++index) {
                const auto ghost = ghosts[index].cast<py::dict>();
                const int slot = ghost["slot"].cast<int>();
                if (slot < 0 || slot > 2) {
                    throw std::invalid_argument("Gremlin Leader ghost slot is out of range");
                }
                bc_->gremlinLeaderGhosts[index] = restore_checkpoint_monster_ghost(ghost);
                bc_->gremlinLeaderGhostSlots[index] = slot;
            }
        }
        if (combat_internal.contains("reused_summon_ghosts")) {
            const auto ghosts = combat_internal["reused_summon_ghosts"].cast<py::list>();
            if (ghosts.size() > bc_->reusedSummonGhosts.size()) {
                throw std::invalid_argument("Too many reused summon ghost snapshots");
            }
            bc_->reusedSummonGhostCount = static_cast<int>(ghosts.size());
            for (int index = 0; index < bc_->reusedSummonGhostCount; ++index) {
                const auto ghost = ghosts[index].cast<py::dict>();
                const int slot = ghost["slot"].cast<int>();
                const bool valid_collector_slot = bc_->encounter == MonsterEncounter::COLLECTOR &&
                    (slot == 0 || slot == 1);
                const bool valid_reptomancer_slot = bc_->encounter == MonsterEncounter::REPTOMANCER &&
                    (slot == 1 || slot == 4);
                if (!valid_collector_slot && !valid_reptomancer_slot) {
                    throw std::invalid_argument("Reused summon ghost slot is invalid for encounter");
                }
                bc_->reusedSummonGhosts[index] = restore_checkpoint_monster_ghost(ghost);
                bc_->reusedSummonGhostSlots[index] = slot;
            }
        }
        bc_->monsterTurnIdx = combat_internal["monster_turn_idx"].cast<int>();
        bc_->turnHasEnded = combat_internal["turn_has_ended"].cast<bool>();
        bc_->skipMonsterTurn = combat_internal["skip_monster_turn"].cast<bool>();
        bc_->isBattleOver = combat_internal["is_battle_over"].cast<bool>();
        bc_->endTurnQueued = combat_internal["end_turn_queued"].cast<bool>();
        bc_->miscBits = combat_internal["misc_bits"].cast<std::uint32_t>();
        bc_->monsters.extraRollMoveOnTurn =
            combat_internal["monster_extra_roll_bits"].cast<std::uint32_t>();
        bc_->monsters.skipTurn =
            combat_internal["monster_skip_turn_bits"].cast<std::uint32_t>();
        bc_->potionCount = combat_internal["potion_count"].cast<int>();
        bc_->potionCapacity = combat_internal["potion_capacity"].cast<int>();
        const auto potion_ids = combat_internal["potion_ids"].cast<py::list>();
        for (int index = 0; index < 5; ++index) {
            bc_->potions[index] = static_cast<Potion>(potion_ids[index].cast<int>());
        }
        bc_->inputState = input_state == "CARD_SELECT"
            ? InputState::CARD_SELECT
            : input_state == "INTERNAL" ? InputState::EXECUTING_ACTIONS
            : InputState::PLAYER_NORMAL;
        if (bc_->inputState == InputState::CARD_SELECT) {
            if (!combat_internal.contains("choice")) {
                throw std::invalid_argument("CARD_SELECT checkpoint is missing choice state");
            }
            const auto choice = combat_internal["choice"].cast<py::dict>();
            bc_->cardSelectInfo.cardSelectTask =
                static_cast<CardSelectTask>(choice["task"].cast<int>());
            bc_->cardSelectInfo.canPickZero = choice["can_pick_zero"].cast<bool>();
            bc_->cardSelectInfo.canPickAnyNumber = choice["can_pick_any_number"].cast<bool>();
            bc_->cardSelectInfo.pickCount = choice["pick_count"].cast<int>();
            bc_->cardSelectInfo.data0 = choice["data0"].cast<int>();
            bc_->cardSelectInfo.discoveryCardType =
                static_cast<CardType>(choice["discovery_card_type"].cast<int>());
            bc_->cardSelectInfo.discoveryRerollOnRetrieve =
                choice["discovery_reroll_on_retrieve"].cast<bool>();
            bc_->cardSelectInfo.discoveryRetrievalUpdates =
                choice.contains("discovery_retrieval_updates")
                    ? choice["discovery_retrieval_updates"].cast<int>() : 14;
            const auto generated = choice["cards"].cast<py::list>();
            if (generated.size() != 3) {
                throw std::invalid_argument("Choice checkpoint requires three generated-card slots");
            }
            for (int index = 0; index < 3; ++index) {
                bc_->cardSelectInfo.cards[index] =
                    static_cast<CardId>(generated[index].cast<int>());
            }
            multi_select_bits_ = choice["selected_bits"].cast<std::uint32_t>();
            multi_select_indices_.clear();
            if (choice.contains("selected_indices")) {
                for (const auto value : choice["selected_indices"].cast<py::list>()) {
                    multi_select_indices_.push_back(value.cast<int>());
                }
            }
        } else {
            multi_select_bits_ = 0;
            multi_select_indices_.clear();
        }
        bc_->outcome = terminal_loss ? Outcome::PLAYER_LOSS : Outcome::UNDECIDED;
        bc_->actionQueue.clear();
        bc_->cardQueue.clear();

        const auto rng = checkpoint["rng"].cast<py::dict>();
        set_rng_state(rng);

        gc_->curHp = bc_->player.curHp;
        gc_->maxHp = bc_->player.maxHp;
        finalized_ = false;
    }

    void step(
        const std::string &kind,
        int card_index,
        int potion_index,
        int target_index,
        int choice_index) {
        require_reset();
        for (int index = 0; index < bc_->monsters.monsterCount; ++index) {
            pre_step_moves_[index] = bc_->monsters.arr[index].moveHistory[0];
        }
        has_pre_step_moves_ = true;
        // The base game removes powers from a dead/escaped monster on the
        // following action-manager update.  lightspeed skips dead monsters in
        // turn processing, so perform that deferred cleanup at the next agent
        // boundary (not immediately on death, where CommunicationMod still
        // exposes the powers for one state).
        if (kind == "end_turn") {
            for (int index = 0; index < bc_->monsters.monsterCount; ++index) {
                auto &monster = bc_->monsters.arr[index];
                if (monster.isDeadOrEscaped()) monster.resetAllStatusEffects();
            }
        }
        search::Action action;
        const auto task = bc_->cardSelectInfo.cardSelectTask;
        const bool multi_select = bc_->inputState == InputState::CARD_SELECT &&
            (task == CardSelectTask::EXHAUST_MANY || task == CardSelectTask::GAMBLE ||
             task == CardSelectTask::RETAIN_CARDS ||
             task == CardSelectTask::WARCRY ||
             (task == CardSelectTask::LIQUID_MEMORIES_POTION &&
              bc_->cardSelectInfo.pickCount > 1) ||
             (task == CardSelectTask::FORETHOUGHT && bc_->cardSelectInfo.canPickAnyNumber));
        if (kind == "choose" && multi_select) {
            const int option_count = task == CardSelectTask::LIQUID_MEMORIES_POTION
                ? static_cast<int>(bc_->cards.discardPile.size())
                : bc_->cards.cardsInHand;
            if (choice_index < 0 || choice_index >= option_count) {
                throw std::invalid_argument("Refusing invalid multi-select card index");
            }
            if (is_selected(choice_index)) {
                throw std::invalid_argument("CommunicationMod cannot unselect a chosen card");
            }
            if ((task == CardSelectTask::EXHAUST_MANY ||
                 task == CardSelectTask::RETAIN_CARDS ||
                 task == CardSelectTask::LIQUID_MEMORIES_POTION) &&
                selected_count() >= bc_->cardSelectInfo.pickCount) {
                throw std::invalid_argument("Multi-select card limit reached");
            }
            if (task == CardSelectTask::LIQUID_MEMORIES_POTION) {
                multi_select_indices_.push_back(choice_index);
            } else {
                multi_select_bits_ |= 1U << choice_index;
            }
            return;
        }
        if (kind == "potion" && potion_index >= 0 && potion_index < bc_->potionCapacity &&
            bc_->potions[potion_index] == Potion::SMOKE_BOMB) {
            if (!bc_->canUseSmokeBomb()) {
                throw std::invalid_argument("Smoke Bomb cannot be used in this combat");
            }
            bc_->discardPotion(potion_index);
            gc_->curHp = bc_->player.curHp;
            gc_->maxHp = bc_->player.maxHp;
            gc_->potionCount = bc_->potionCount;
            gc_->potions = bc_->potions;
            escaped_ = true;
            finalized_ = true;
            return;
        }
        if (kind == "play") {
            const int source = card_index - 1;
            const auto &card = bc_->cards.hand[source];
            action = search::Action(
                search::ActionType::CARD,
                source,
                card.requiresTarget() ? target_index : 0);
        } else if (kind == "choose") {
            action = search::Action(search::ActionType::SINGLE_CARD_SELECT, choice_index);
        } else if (kind == "potion") {
            action = search::Action(
                search::ActionType::POTION, potion_index,
                target_index < 0 ? 0 : target_index);
        } else if (kind == "discard_potion") {
            action = search::Action(search::ActionType::POTION, potion_index, 6);
        } else if (kind == "proceed" && multi_select &&
                task == CardSelectTask::WARCRY) {
            if (selected_count() != 1) {
                throw std::invalid_argument("Put On Deck requires one selected card");
            }
            int selected = 0;
            while (!is_selected(selected)) ++selected;
            action = search::Action(search::ActionType::SINGLE_CARD_SELECT, selected);
        } else if (kind == "proceed" && multi_select &&
                task == CardSelectTask::LIQUID_MEMORIES_POTION) {
            if (selected_count() != bc_->cardSelectInfo.pickCount) {
                throw std::invalid_argument("Liquid Memories requires the full card selection");
            }
            fixed_list<int, 10> selected;
            auto sorted = multi_select_indices_;
            std::sort(sorted.begin(), sorted.end());
            for (const int index : sorted) selected.push_back(index);
            bc_->chooseDiscardToHandCards(selected, true);
            bc_->inputState = InputState::EXECUTING_ACTIONS;
            bc_->executeActions();
            multi_select_indices_.clear();
            multi_select_bits_ = 0;
            if (bc_->outcome != Outcome::UNDECIDED && !finalized_) {
                bc_->exitBattle(*gc_);
                finalized_ = true;
            }
            return;
        } else if (kind == "proceed" && multi_select) {
            action = search::Action(
                search::ActionType::MULTI_CARD_SELECT,
                static_cast<int>(multi_select_bits_));
        } else if (kind == "end_turn") {
            action = search::Action(search::ActionType::END_TURN);
        } else {
            throw std::invalid_argument("Unsupported simulator action kind: " + kind);
        }
        if (!action.isValidAction(*bc_)) {
            throw std::invalid_argument("Refusing illegal simulator action");
        }
        action.execute(*bc_);
        if (multi_select) {
            multi_select_bits_ = 0;
            multi_select_indices_.clear();
        }
        if (bc_->outcome != Outcome::UNDECIDED && !finalized_) {
            bc_->exitBattle(*gc_);
            finalized_ = true;
        }
    }

    py::dict snapshot() const {
        require_reset();
        py::dict payload;
        payload["ready_for_command"] = true;
        payload["in_game"] = true;
        payload["error"] = py::none();

        py::dict game;
        game["current_hp"] = finalized_ ? gc_->curHp : bc_->player.curHp;
        game["max_hp"] = finalized_ ? gc_->maxHp : bc_->player.maxHp;
        game["act"] = gc_->act;
        game["floor"] = bc_->floorNum;
        game["seed"] = bc_->seed;
        game["encounter"] = monsterEncounterEnumNames[static_cast<int>(bc_->encounter)];
        game["class"] = "IRONCLAD";
        game["ascension_level"] = bc_->ascension;
        game["gold"] = finalized_ ? gc_->gold : bc_->player.gold;
        py::list potions;
        for (int index = 0; index < bc_->potionCapacity; ++index) {
            const auto potion = bc_->potions[index];
            py::dict value;
            const bool empty = potion == Potion::INVALID || potion == Potion::EMPTY_POTION_SLOT;
            value["id"] = empty ? "Potion Slot" : getPotionName(potion);
            value["name"] = empty ? "Potion Slot" : getPotionName(potion);
            value["can_use"] = !empty && potion != Potion::FAIRY_POTION;
            value["can_discard"] = !empty;
            value["requires_target"] = !empty && potionRequiresTarget(potion);
            value["slot"] = index;
            potions.append(value);
        }
        game["potions"] = potions;
        py::list relics;
        for (const auto &relic : gc_->relics.relics) {
            py::dict value;
            value["id"] = relicIds[static_cast<int>(relic.id)];
            value["name"] = getRelicName(relic.id);
            value["counter"] = relic_counter(relic, *bc_);
            relics.append(value);
        }
        game["relics"] = relics;
        py::list master_deck;
        for (const auto &card : gc_->deck.cards) {
            py::dict value;
            value["id"] = getCardEnumName(card.id);
            value["upgrades"] = card.getUpgraded();
            value["misc"] = card.misc;
            master_deck.append(value);
        }
        game["master_deck"] = master_deck;

        const bool terminal = escaped_ || bc_->outcome != Outcome::UNDECIDED;
        const char *outcome = (escaped_ || bc_->outcome == Outcome::PLAYER_ESCAPE)
            ? "ESCAPED" : bc_->outcome == Outcome::PLAYER_VICTORY
            ? "PLAYER_VICTORY"
            : bc_->outcome == Outcome::PLAYER_LOSS ? "PLAYER_LOSS" : "UNDECIDED";
        payload["outcome"] = outcome;
        game["outcome"] = outcome;
        game["room_phase"] = terminal ? "COMPLETE" : "COMBAT";
        game["input_state"] = bc_->inputState == InputState::PLAYER_NORMAL
            ? "PLAYER_NORMAL"
            : bc_->inputState == InputState::CARD_SELECT ? "CARD_SELECT" : "INTERNAL";
        // CommunicationMod preserves combat_state on GAME_OVER but removes it
        // on COMBAT_REWARD. Keep the same terminal boundary so loss traces can
        // compare final card zones and powers instead of collapsing to blanks.
        if (!terminal || bc_->outcome == Outcome::PLAYER_LOSS) {
            game["combat_state"] = combat_state();
        }
        payload["game_state"] = game;

        py::dict rng = full_run_rng_state(*gc_);
        if (!finalized_) {
            rng["ai"] = rng_state(bc_->aiRng);
            rng["monster_hp"] = rng_state(bc_->monsterHpRng);
            rng["shuffle"] = rng_state(bc_->shuffleRng);
            rng["card_random"] = rng_state(bc_->cardRandomRng);
            rng["misc"] = rng_state(bc_->miscRng);
            rng["potion"] = rng_state(bc_->potionRng);
        }
        payload["_rng"] = rng;

        py::list commands;
        py::list actions;
        if (!terminal) {
            if (bc_->inputState == InputState::PLAYER_NORMAL) {
                commands.append("play");
                commands.append("potion");
                commands.append("end");
                enumerate_normal_actions(actions);
            } else if (bc_->inputState == InputState::CARD_SELECT) {
                commands.append("choose");
                const auto task = bc_->cardSelectInfo.cardSelectTask;
                if (task == CardSelectTask::EXHAUST_MANY ||
                    task == CardSelectTask::GAMBLE ||
                    task == CardSelectTask::RETAIN_CARDS ||
                    task == CardSelectTask::WARCRY) {
                    commands.append("proceed");
                }
                enumerate_choice_actions(actions);
            }
        }
        payload["available_commands"] = commands;
        payload["_legal_actions"] = actions;
        return payload;
    }

    void adopt_active_run(
            std::unique_ptr<GameContext> &game,
            std::unique_ptr<BattleContext> &battle) {
        if (gc_ || bc_) throw std::logic_error("Combat serializer already owns a context");
        gc_ = std::move(game);
        bc_ = std::move(battle);
        finalized_ = false;
        escaped_ = false;
        multi_select_bits_ = 0;
        multi_select_indices_.clear();
    }

    void return_active_run(
            std::unique_ptr<GameContext> &game,
            std::unique_ptr<BattleContext> &battle) {
        game = std::move(gc_);
        battle = std::move(bc_);
    }

    std::unique_ptr<BattleContext> release_loaded_battle() {
        return std::move(bc_);
    }

private:
    std::unique_ptr<GameContext> gc_;
    std::unique_ptr<BattleContext> bc_;
    bool finalized_ = false;
    bool escaped_ = false;
    std::array<MMID, 5> pre_step_moves_{};
    bool has_pre_step_moves_ = false;
    std::uint32_t multi_select_bits_ = 0;
    std::vector<int> multi_select_indices_;

    void install_combat_reward_callback() {
        gc_->regainControlAction = [](GameContext &gc) {
            gc.openCombatRewardScreen(
                gc.curRoom == Room::ELITE
                    ? gc.createEliteCombatReward()
                    : gc.createCombatReward());
            gc.regainControlAction = [](GameContext &next) {
                next.screenState = ScreenState::MAP_SCREEN;
            };
        };
    }

    int selected_count() const {
        if (bc_ && bc_->cardSelectInfo.cardSelectTask == CardSelectTask::LIQUID_MEMORIES_POTION &&
                bc_->cardSelectInfo.pickCount > 1) {
            return static_cast<int>(multi_select_indices_.size());
        }
        auto bits = multi_select_bits_;
        int count = 0;
        while (bits != 0) {
            count += bits & 1U;
            bits >>= 1U;
        }
        return count;
    }

    bool is_selected(int index) const {
        if (bc_ && bc_->cardSelectInfo.cardSelectTask == CardSelectTask::LIQUID_MEMORIES_POTION &&
                bc_->cardSelectInfo.pickCount > 1) {
            return std::find(multi_select_indices_.begin(), multi_select_indices_.end(), index)
                != multi_select_indices_.end();
        }
        return index >= 0 && index < 32 && (multi_select_bits_ & (1U << index)) != 0;
    }

    static CardInstance restore_card(const py::dict &value) {
        const auto id = value["id"].cast<std::string>();
        const int upgrades = value["upgrades"].cast<int>();
        CardInstance card(parse_card(id + (upgrades > 0 ? "+" + std::to_string(upgrades) : "")));
        if (value.contains("uuid")) {
            card.uniqueId = static_cast<std::int16_t>(
                std::stoi(value["uuid"].cast<std::string>()));
        }
        card.cost = value["base_cost"].cast<int>();
        card.costForTurn = value["cost"].cast<int>();
        card.specialData = value["special_data"].cast<int>();
        card.freeToPlayOnce = value["free_to_play_once"].cast<bool>();
        card.retain = value["retain"].cast<bool>();
        return card;
    }

    void restore_relics(const py::dict &game) {
        if (!game.contains("relics")) return;
        gc_->relics = RelicContainer();
        for (const auto item : game["relics"].cast<py::list>()) {
            const auto value = item.cast<py::dict>();
            const auto id = parse_relic(value["id"].cast<std::string>());
            const int counter = value.contains("counter")
                ? value["counter"].cast<int>() : -1;
            gc_->relics.add(RelicInstance{id, counter});
        }
    }

    void restore_cards(const py::dict &combat) {
        bc_->cards = CardManager();
        auto add_card = [this](CardInstance card, const char *pile) {
            if (card.uniqueId < 0) {
                card.uniqueId = static_cast<std::int16_t>(bc_->cards.nextUniqueCardId++);
            } else {
                bc_->cards.nextUniqueCardId = std::max(
                    bc_->cards.nextUniqueCardId,
                    static_cast<int>(card.uniqueId) + 1);
            }
            if (std::string(pile) != "exhaust") bc_->cards.notifyAddCardToCombat(card);
            if (std::string(pile) == "hand") {
                bc_->cards.notifyAddToHand(card);
                bc_->cards.hand[bc_->cards.cardsInHand++] = card;
            } else if (std::string(pile) == "draw") {
                bc_->cards.notifyAddToDrawPile(card);
                bc_->cards.drawPile.push_back(card);
            } else if (std::string(pile) == "discard") {
                bc_->cards.notifyAddToDiscardPile(card);
                bc_->cards.discardPile.push_back(card);
            } else {
                bc_->cards.exhaustPile.push_back(card);
            }
        };
        for (const auto item : combat["hand"].cast<py::list>())
            add_card(restore_card(item.cast<py::dict>()), "hand");
        for (const auto item : combat["draw_pile"].cast<py::list>())
            add_card(restore_card(item.cast<py::dict>()), "draw");
        for (const auto item : combat["discard_pile"].cast<py::list>())
            add_card(restore_card(item.cast<py::dict>()), "discard");
        for (const auto item : combat["exhaust_pile"].cast<py::list>())
            add_card(restore_card(item.cast<py::dict>()), "exhaust");
    }

    void restore_player(const py::dict &value) {
        const auto relic_bits0 = bc_->player.relicBits0;
        const auto relic_bits1 = bc_->player.relicBits1;
        bc_->player = Player();
        auto &p = bc_->player;
        p.cc = CharacterClass::IRONCLAD;
        p.curHp = value["current_hp"].cast<int>();
        p.maxHp = value["max_hp"].cast<int>();
        p.block = value["block"].cast<int>();
        p.energy = value["energy"].cast<int>();
        p.energyPerTurn = value["energy_per_turn"].cast<int>();
        p.cardDrawPerTurn = value["card_draw_per_turn"].cast<int>();
        p.cardsPlayedThisTurn = value["cards_played_this_turn"].cast<int>();
        p.attacksPlayedThisTurn = value["attacks_played_this_turn"].cast<int>();
        p.skillsPlayedThisTurn = value["skills_played_this_turn"].cast<int>();
        const auto internal = value["_internal"].cast<py::dict>();
        p.gold = internal["gold"].cast<int>();
        p.stance = static_cast<Stance>(internal["stance"].cast<int>());
        p.orbSlots = internal["orb_slots"].cast<int>();
        if (internal.contains("orbs")) {
            const auto orbs = internal["orbs"].cast<py::list>();
            const auto evoke = internal["orb_evoke_amounts"].cast<py::list>();
            if (orbs.size() != Player::MAX_ORB_SLOTS ||
                    evoke.size() != Player::MAX_ORB_SLOTS) {
                throw std::invalid_argument("Orb checkpoint arrays have invalid length");
            }
            for (int i = 0; i < Player::MAX_ORB_SLOTS; ++i) {
                p.orbs[i] = static_cast<Orb>(orbs[i].cast<int>());
                p.orbEvokeAmounts[i] = evoke[i].cast<int>();
            }
        }
        p.lastTargetedMonster = internal["last_targeted_monster"].cast<int>();
        p.relicBits0 = internal.contains("relic_bits0")
            ? internal["relic_bits0"].cast<std::uint64_t>() : relic_bits0;
        p.relicBits1 = internal.contains("relic_bits1")
            ? internal["relic_bits1"].cast<std::uint64_t>() : relic_bits1;
        p.justAppliedBits = internal["just_applied_bits"].cast<std::uint32_t>();
        p.statusBits0 = internal["status_bits0"].cast<std::uint64_t>();
        p.statusBits1 = internal["status_bits1"].cast<std::uint32_t>();
        p.combustHpLoss = internal["combust_hp_loss"].cast<int>();
        p.haveUsedNecronomiconThisTurn = internal["have_used_necronomicon"].cast<bool>();
        p.happyFlowerCounter = internal["happy_flower_counter"].cast<int>();
        p.incenseBurnerCounter = internal["incense_burner_counter"].cast<int>();
        p.inkBottleCounter = internal["ink_bottle_counter"].cast<int>();
        p.inserterCounter = internal["inserter_counter"].cast<int>();
        p.nunchakuCounter = internal["nunchaku_counter"].cast<int>();
        p.penNibCounter = internal["pen_nib_counter"].cast<int>();
        p.sundialCounter = internal["sundial_counter"].cast<int>();
        p.devaFormEnergyPerTurn = internal["deva_form_energy_per_turn"].cast<int>();
        p.echoFormCardsDoubled = internal["echo_form_cards_doubled"].cast<int>();
        p.panacheCounter = internal["panache_counter"].cast<int>();
        p.orangePelletsCardTypesPlayed = internal["orange_pellets_card_types"].cast<std::uint32_t>();
        p.cardsDiscardedThisTurn = internal["cards_discarded_this_turn"].cast<int>();
        p.lastAttackUnblockedDamage = internal["last_attack_unblocked_damage"].cast<int>();
        p.timesDamagedThisCombat = internal["times_damaged_this_combat"].cast<int>();
        p.bomb1 = internal["bomb1"].cast<int>();
        p.bomb2 = internal["bomb2"].cast<int>();
        p.bomb3 = internal["bomb3"].cast<int>();
        for (const auto item : internal["status_map"].cast<py::list>()) {
            const auto entry = item.cast<py::dict>();
            p.statusMap[static_cast<PlayerStatus>(entry["status"].cast<int>())] =
                entry["amount"].cast<int>();
        }
        for (const auto item : value["powers"].cast<py::list>()) {
            const auto entry = item.cast<py::dict>();
            const auto id = normalized(entry["id"].cast<std::string>());
            const int amount = entry["amount"].cast<int>();
            if (id == "STRENGTH") p.strength = amount;
            else if (id == "DEXTERITY") p.dexterity = amount;
            else if (id == "FOCUS") p.focus = amount;
            else if (id == "ARTIFACT") p.artifact = amount;
        }
        if (internal.contains("power_order")) {
            for (const auto item : internal["power_order"].cast<py::list>()) {
                p.powerOrder.push_back(static_cast<PlayerStatus>(item.cast<int>()));
            }
        } else {
            for (int raw = static_cast<int>(PS::INVALID) + 1;
                    raw <= static_cast<int>(PS::BERSERK); ++raw) {
                const auto status = static_cast<PlayerStatus>(raw);
                if (p.hasStatusRuntime(status)) p.recordPowerApplied(status);
            }
        }
    }

    void restore_monsters(const py::dict &combat) {
        bc_->monsters = MonsterGroup();
        const auto values = combat["monsters"].cast<py::list>();
        bc_->monsters.monsterCount = static_cast<int>(values.size());
        for (int index = 0; index < bc_->monsters.monsterCount; ++index) {
            const auto value = values[index].cast<py::dict>();
            const auto internal = value["_internal"].cast<py::dict>();
            Monster monster;
            monster.idx = index;
            const auto monster_id = value["monster_id"].cast<std::string>();
            monster.id = monster_id.rfind("INVALID", 0) == 0
                ? MonsterId::INVALID : parse_monster(monster_id);
            monster.curHp = value["current_hp"].cast<int>();
            monster.maxHp = value["max_hp"].cast<int>();
            monster.block = value["block"].cast<int>();
            monster.halfDead = value["half_dead"].cast<bool>();
            monster.isEscapingB = internal["is_escaping"].cast<bool>();
            monster.escapeNext = internal["escape_next"].cast<bool>();
            monster.moveHistory[0] = parse_move(value["move_id"].cast<std::string>());
            monster.moveHistory[1] = parse_move(internal["move_previous"].cast<std::string>());
            monster.statusBits = internal["status_bits"].cast<std::uint64_t>();
            monster.artifact = internal["artifact"].cast<int>();
            monster.blockReturn = internal["block_return"].cast<int>();
            monster.choked = internal["choked"].cast<int>();
            monster.corpseExplosion = internal["corpse_explosion"].cast<int>();
            monster.lockOn = internal["lock_on"].cast<int>();
            monster.mark = internal["mark"].cast<int>();
            monster.metallicize = internal["metallicize"].cast<int>();
            monster.platedArmor = internal["plated_armor"].cast<int>();
            monster.poison = internal["poison"].cast<int>();
            monster.regen = internal["regen"].cast<int>();
            monster.shackled = internal["shackled"].cast<int>();
            monster.strength = internal["strength"].cast<int>();
            monster.vulnerable = internal["vulnerable"].cast<int>();
            monster.weak = internal["weak"].cast<int>();
            monster.uniquePower0 = internal["unique_power0"].cast<int>();
            monster.uniquePower1 = internal["unique_power1"].cast<int>();
            monster.miscInfo = internal["misc_info"].cast<int>();
            if (internal.contains("power_order")) {
                for (const auto item : internal["power_order"].cast<py::list>()) {
                    monster.powerOrder.push_back(static_cast<MonsterStatus>(item.cast<int>()));
                }
            } else {
                for (int raw = 0; raw < static_cast<int>(MS::INVALID); ++raw) {
                    const auto status = static_cast<MonsterStatus>(raw);
                    if (monster.hasStatusInternal(status)) monster.recordPowerApplied(status);
                }
            }
            bc_->monsters.arr[index] = monster;
            if (monster.curHp > 0 && !monster.halfDead && !monster.isEscapingB)
                ++bc_->monsters.monstersAlive;
        }
    }

    void require_reset() const {
        if (!bc_) throw std::logic_error("Call reset() before using LightspeedBattle");
    }

    py::dict combat_state() const {
        py::dict combat;
        // CommunicationMod exposes the first player turn as 1; lightspeed uses 0 internally.
        combat["turn"] = bc_->turn + 1;

        py::dict player;
        player["current_hp"] = bc_->player.curHp;
        player["max_hp"] = bc_->player.maxHp;
        player["block"] = bc_->player.block;
        player["energy"] = bc_->player.energy;
        player["energy_per_turn"] = bc_->player.energyPerTurn;
        player["stance"] = stanceStrings[static_cast<int>(bc_->player.stance)];
        player["card_draw_per_turn"] = bc_->player.cardDrawPerTurn;
        player["cards_played_this_turn"] = bc_->player.cardsPlayedThisTurn;
        player["attacks_played_this_turn"] = bc_->player.attacksPlayedThisTurn;
        player["skills_played_this_turn"] = bc_->player.skillsPlayedThisTurn;
        player["powers"] = player_powers(bc_->player);
        py::list orbs;
        for (int i = 0; i < bc_->player.orbSlots; ++i) {
            const auto orb = bc_->player.orbs[i];
            py::dict value;
            const char *name = orb == Orb::DARK ? "Dark"
                : orb == Orb::FROST ? "Frost"
                : orb == Orb::FUSION ? "Plasma"
                : orb == Orb::LIGHTNING ? "Lightning" : "Empty";
            value["id"] = name;
            value["name"] = name;
            value["passive_amount"] = orb == Orb::DARK ? std::max(0, 6 + bc_->player.focus)
                : orb == Orb::FROST ? std::max(0, 2 + bc_->player.focus)
                : orb == Orb::FUSION ? 1
                : orb == Orb::LIGHTNING ? std::max(0, 3 + bc_->player.focus) : 0;
            value["evoke_amount"] = orb == Orb::DARK ? bc_->player.orbEvokeAmounts[i]
                : orb == Orb::FROST ? std::max(0, 5 + bc_->player.focus)
                : orb == Orb::FUSION ? 2
                : orb == Orb::LIGHTNING ? std::max(0, 8 + bc_->player.focus) : 0;
            orbs.append(value);
        }
        player["orbs"] = orbs;
        player["max_orbs"] = bc_->player.orbSlots;
        py::dict player_internal;
        player_internal["just_applied_bits"] = bc_->player.justAppliedBits;
        player_internal["gold"] = bc_->player.gold;
        player_internal["stance"] = static_cast<int>(bc_->player.stance);
        player_internal["orb_slots"] = bc_->player.orbSlots;
        py::list orb_types;
        py::list orb_evoke_amounts;
        for (int i = 0; i < Player::MAX_ORB_SLOTS; ++i) {
            orb_types.append(static_cast<int>(bc_->player.orbs[i]));
            orb_evoke_amounts.append(bc_->player.orbEvokeAmounts[i]);
        }
        player_internal["orbs"] = orb_types;
        player_internal["orb_evoke_amounts"] = orb_evoke_amounts;
        player_internal["last_targeted_monster"] = bc_->player.lastTargetedMonster;
        player_internal["relic_bits0"] = bc_->player.relicBits0;
        player_internal["relic_bits1"] = bc_->player.relicBits1;
        player_internal["status_bits0"] = bc_->player.statusBits0;
        player_internal["status_bits1"] = bc_->player.statusBits1;
        player_internal["combust_hp_loss"] = bc_->player.combustHpLoss;
        player_internal["have_used_necronomicon"] = bc_->player.haveUsedNecronomiconThisTurn;
        player_internal["happy_flower_counter"] = bc_->player.happyFlowerCounter;
        player_internal["incense_burner_counter"] = bc_->player.incenseBurnerCounter;
        player_internal["ink_bottle_counter"] = bc_->player.inkBottleCounter;
        player_internal["inserter_counter"] = bc_->player.inserterCounter;
        player_internal["nunchaku_counter"] = bc_->player.nunchakuCounter;
        player_internal["pen_nib_counter"] = bc_->player.penNibCounter;
        player_internal["sundial_counter"] = bc_->player.sundialCounter;
        player_internal["deva_form_energy_per_turn"] = bc_->player.devaFormEnergyPerTurn;
        player_internal["echo_form_cards_doubled"] = bc_->player.echoFormCardsDoubled;
        player_internal["panache_counter"] = bc_->player.panacheCounter;
        player_internal["orange_pellets_card_types"] = bc_->player.orangePelletsCardTypesPlayed.to_ulong();
        player_internal["cards_discarded_this_turn"] = bc_->player.cardsDiscardedThisTurn;
        player_internal["last_attack_unblocked_damage"] = bc_->player.lastAttackUnblockedDamage;
        player_internal["times_damaged_this_combat"] = bc_->player.timesDamagedThisCombat;
        player_internal["bomb1"] = bc_->player.bomb1;
        player_internal["bomb2"] = bc_->player.bomb2;
        player_internal["bomb3"] = bc_->player.bomb3;
        py::list status_map;
        for (const auto &[status, amount] : bc_->player.statusMap) {
            py::dict item;
            item["status"] = static_cast<int>(status);
            item["amount"] = amount;
            status_map.append(item);
        }
        player_internal["status_map"] = status_map;
        py::list power_order;
        for (const auto status : bc_->player.powerOrder) {
            power_order.append(static_cast<int>(status));
        }
        player_internal["power_order"] = power_order;
        player["_internal"] = player_internal;
        combat["player"] = player;

        py::list hand;
        for (int i = 0; i < bc_->cards.cardsInHand; ++i) {
            hand.append(card_dict(bc_->cards.hand[i], bc_.get()));
        }
        combat["hand"] = hand;
        combat["draw_pile"] = card_list(bc_->cards.drawPile);
        combat["discard_pile"] = card_list(bc_->cards.discardPile);
        combat["exhaust_pile"] = card_list(bc_->cards.exhaustPile);

        py::list monsters;
        for (int i = 0; i < bc_->monsters.monsterCount; ++i) {
            const auto &monster = bc_->monsters.arr[i];
            const bool reserved_summon_slot = monster.id == MonsterId::INVALID && (
                (bc_->encounter == MonsterEncounter::AUTOMATON && (i == 0 || i == 2)) ||
                (bc_->encounter == MonsterEncounter::COLLECTOR && (i == 0 || i == 1)));
            if (reserved_summon_slot) continue;
            const auto damage = monster.getMoveBaseDamage(*bc_);
            auto &player_for_display = bc_->player;
            const auto saved_status_bits0 = player_for_display.statusBits0;
            const auto saved_status_bits1 = player_for_display.statusBits1;
            const auto saved_status_map = player_for_display.statusMap;
            const auto saved_just_applied = player_for_display.justAppliedBits;
            const auto saved_power_order = player_for_display.powerOrder;
            player_for_display.removeStatus<PS::INTANGIBLE>();
            const int displayed_damage = damage.damage > 0
                ? monster.calculateDamageToPlayer(*bc_, damage.damage) : 0;
            player_for_display.statusBits0 = saved_status_bits0;
            player_for_display.statusBits1 = saved_status_bits1;
            player_for_display.statusMap = saved_status_map;
            player_for_display.justAppliedBits = saved_just_applied;
            player_for_display.powerOrder = saved_power_order;
            py::dict value;
            value["id"] = monster.getName();
            value["name"] = monster.getName();
            value["monster_id"] = monsterIdStrings[static_cast<int>(monster.id)];
            value["current_hp"] = monster.curHp;
            value["max_hp"] = monster.maxHp;
            value["block"] = monster.block;
            auto intent_monster = monster;
            // On the original GAME_OVER frame, a monster may already have
            // selected its next move while createIntent has not run because
            // player death stopped the queue. CommunicationMod then exposes
            // the new move id/damage with the previous displayed intent.
            if (bc_->outcome == Outcome::PLAYER_LOSS && has_pre_step_moves_ &&
                    monster.moveHistory[0] != pre_step_moves_[i]) {
                intent_monster.moveHistory[0] = pre_step_moves_[i];
            }
            value["intent"] = intent_name(intent_monster, bc_.get());
            value["move_id"] = monsterMoveStrings[static_cast<int>(monster.moveHistory[0])];
            py::dict monster_internal;
            monster_internal["status_bits"] = monster.statusBits;
            monster_internal["artifact"] = monster.artifact;
            monster_internal["block_return"] = monster.blockReturn;
            monster_internal["choked"] = monster.choked;
            monster_internal["corpse_explosion"] = monster.corpseExplosion;
            monster_internal["lock_on"] = monster.lockOn;
            monster_internal["mark"] = monster.mark;
            monster_internal["metallicize"] = monster.metallicize;
            monster_internal["plated_armor"] = monster.platedArmor;
            monster_internal["poison"] = monster.poison;
            monster_internal["regen"] = monster.regen;
            monster_internal["shackled"] = monster.shackled;
            monster_internal["strength"] = monster.strength;
            monster_internal["vulnerable"] = monster.vulnerable;
            monster_internal["weak"] = monster.weak;
            monster_internal["unique_power0"] = monster.uniquePower0;
            monster_internal["unique_power1"] = monster.uniquePower1;
            monster_internal["misc_info"] = monster.miscInfo;
            py::list monster_power_order;
            for (const auto status : monster.powerOrder) {
                monster_power_order.append(static_cast<int>(status));
            }
            monster_internal["power_order"] = monster_power_order;
            monster_internal["move_previous"] = monsterMoveStrings[static_cast<int>(monster.moveHistory[1])];
            monster_internal["is_escaping"] = monster.isEscapingB;
            monster_internal["escape_next"] = monster.escapeNext;
            value["_internal"] = monster_internal;
            value["move_base_damage"] = damage.damage;
            value["move_adjusted_damage"] = displayed_damage;
            value["move_hits"] = damage.attackCount;
            value["half_dead"] = monster.halfDead;
            value["is_gone"] = monster.isDeadOrEscaped();
            value["powers"] = monster_powers(monster);
            monsters.append(value);
        }
        combat["monsters"] = monsters;
        py::dict combat_internal;
        combat_internal["monster_turn_idx"] = bc_->monsterTurnIdx;
        combat_internal["turn_has_ended"] = bc_->turnHasEnded;
        combat_internal["skip_monster_turn"] = bc_->skipMonsterTurn;
        combat_internal["is_battle_over"] = bc_->isBattleOver;
        combat_internal["end_turn_queued"] = bc_->endTurnQueued;
        combat_internal["misc_bits"] = bc_->miscBits.to_ulong();
        combat_internal["monster_extra_roll_bits"] = bc_->monsters.extraRollMoveOnTurn.to_ulong();
        combat_internal["monster_skip_turn_bits"] = bc_->monsters.skipTurn.to_ulong();
        combat_internal["potion_count"] = bc_->potionCount;
        combat_internal["potion_capacity"] = bc_->potionCapacity;
        combat_internal["next_unique_card_id"] = bc_->cards.nextUniqueCardId;
        py::list stasis_cards;
        for (const auto &card : bc_->cards.stasisCards) {
            if (card.getId() == CardId::INVALID) stasis_cards.append(py::none());
            else stasis_cards.append(card_dict(card));
        }
        combat_internal["stasis_cards"] = stasis_cards;
        py::list potion_ids;
        for (int index = 0; index < 5; ++index) {
            potion_ids.append(static_cast<int>(bc_->potions[index]));
        }
        combat_internal["potion_ids"] = potion_ids;
        if (bc_->encounter == MonsterEncounter::SLIME_BOSS ||
                bc_->encounter == MonsterEncounter::LARGE_SLIME) {
            py::list ghosts;
            for (const int slot : {4, 5, 6}) {
                const auto &monster = bc_->monsters.arr[slot];
                if (monster.id == MonsterId::INVALID) continue;
                py::dict ghost;
                ghost["slot"] = slot;
                ghost["id"] = static_cast<int>(monster.id);
                ghost["current_hp"] = monster.curHp;
                ghost["max_hp"] = monster.maxHp;
                ghost["block"] = monster.block;
                ghost["move_current"] = static_cast<int>(monster.moveHistory[0]);
                ghost["move_previous"] = static_cast<int>(monster.moveHistory[1]);
                ghost["status_bits"] = monster.statusBits;
                ghost["artifact"] = monster.artifact;
                ghost["block_return"] = monster.blockReturn;
                ghost["choked"] = monster.choked;
                ghost["corpse_explosion"] = monster.corpseExplosion;
                ghost["lock_on"] = monster.lockOn;
                ghost["mark"] = monster.mark;
                ghost["metallicize"] = monster.metallicize;
                ghost["plated_armor"] = monster.platedArmor;
                ghost["poison"] = monster.poison;
                ghost["regen"] = monster.regen;
                ghost["shackled"] = monster.shackled;
                ghost["strength"] = monster.strength;
                ghost["vulnerable"] = monster.vulnerable;
                ghost["weak"] = monster.weak;
                ghost["unique_power0"] = monster.uniquePower0;
                ghost["unique_power1"] = monster.uniquePower1;
                ghost["misc_info"] = monster.miscInfo;
                ghost["half_dead"] = monster.halfDead;
                ghost["is_escaping"] = monster.isEscapingB;
                ghost["escape_next"] = monster.escapeNext;
                py::list power_order;
                for (const auto power : monster.powerOrder) {
                    power_order.append(static_cast<int>(power));
                }
                ghost["power_order"] = power_order;
                ghosts.append(ghost);
            }
            combat_internal["slime_split_ghosts"] = ghosts;
        }
        if (bc_->encounter == MonsterEncounter::GREMLIN_LEADER) {
            py::list ghosts;
            for (int index = 0; index < bc_->gremlinLeaderGhostCount; ++index) {
                ghosts.append(checkpoint_monster_ghost(
                    bc_->gremlinLeaderGhosts[index],
                    bc_->gremlinLeaderGhostSlots[index]));
            }
            combat_internal["gremlin_leader_ghosts"] = ghosts;
        }
        if (bc_->encounter == MonsterEncounter::COLLECTOR ||
                bc_->encounter == MonsterEncounter::REPTOMANCER) {
            py::list ghosts;
            for (int index = 0; index < bc_->reusedSummonGhostCount; ++index) {
                ghosts.append(checkpoint_monster_ghost(
                    bc_->reusedSummonGhosts[index],
                    bc_->reusedSummonGhostSlots[index]));
            }
            combat_internal["reused_summon_ghosts"] = ghosts;
        }
        if (bc_->inputState == InputState::CARD_SELECT) {
            py::dict choice_internal;
            const auto task = bc_->cardSelectInfo.cardSelectTask;
            choice_internal["task"] = static_cast<int>(task);
            choice_internal["can_pick_zero"] = bc_->cardSelectInfo.canPickZero;
            choice_internal["can_pick_any_number"] = bc_->cardSelectInfo.canPickAnyNumber;
            choice_internal["pick_count"] = bc_->cardSelectInfo.pickCount;
            choice_internal["data0"] =
                task == CardSelectTask::DISCOVERY || task == CardSelectTask::DUAL_WIELD
                    ? bc_->cardSelectInfo.data0 : 0;
            choice_internal["discovery_card_type"] =
                static_cast<int>(task == CardSelectTask::DISCOVERY
                    ? bc_->cardSelectInfo.discoveryCardType : CardType::INVALID);
            choice_internal["discovery_reroll_on_retrieve"] =
                task == CardSelectTask::DISCOVERY &&
                bc_->cardSelectInfo.discoveryRerollOnRetrieve;
            choice_internal["discovery_retrieval_updates"] =
                task == CardSelectTask::DISCOVERY
                    ? bc_->cardSelectInfo.discoveryRetrievalUpdates : 0;
            py::list generated;
            for (const auto card : bc_->cardSelectInfo.cards) {
                generated.append(static_cast<int>(
                    task == CardSelectTask::DISCOVERY || task == CardSelectTask::CODEX
                        ? card : CardId::INVALID));
            }
            choice_internal["cards"] = generated;
            choice_internal["selected_bits"] = multi_select_bits_;
            py::list selected_indices;
            for (const int index : multi_select_indices_) selected_indices.append(index);
            choice_internal["selected_indices"] = selected_indices;
            combat_internal["choice"] = choice_internal;
        }
        combat["_internal"] = combat_internal;
        if (bc_->inputState == InputState::CARD_SELECT) {
            combat["choice"] = choice_state();
        }
        return combat;
    }

    py::dict choice_state() const {
        py::dict result;
        const auto task = bc_->cardSelectInfo.cardSelectTask;
        result["task"] = cardSelectTaskStrings[static_cast<int>(task)];
        result["pick_count"] = bc_->cardSelectInfo.pickCount;
        py::list options;

        auto append_cards = [this, &options](const auto &begin, const auto &end) {
            int index = 0;
            for (auto it = begin; it != end; ++it, ++index) {
                auto value = card_dict(*it);
                value["choice_index"] = index;
                value["selected"] = is_selected(index);
                options.append(value);
            }
        };

        switch (task) {
            case CardSelectTask::ARMAMENTS:
            case CardSelectTask::DUAL_WIELD:
            case CardSelectTask::EXHAUST_ONE:
            case CardSelectTask::EXHAUST_MANY:
            case CardSelectTask::FORETHOUGHT:
            case CardSelectTask::GAMBLE:
            case CardSelectTask::RETAIN_CARDS:
            case CardSelectTask::WARCRY:
                result["source"] = "HAND";
                append_cards(
                    bc_->cards.hand.begin(),
                    bc_->cards.hand.begin() + bc_->cards.cardsInHand);
                break;
            case CardSelectTask::EXHUME:
                result["source"] = "EXHAUST_PILE";
                append_cards(bc_->cards.exhaustPile.begin(), bc_->cards.exhaustPile.end());
                break;
            case CardSelectTask::HEADBUTT:
            case CardSelectTask::HOLOGRAM:
            case CardSelectTask::LIQUID_MEMORIES_POTION:
                result["source"] = "DISCARD_PILE";
                append_cards(bc_->cards.discardPile.begin(), bc_->cards.discardPile.end());
                break;
            case CardSelectTask::SECRET_TECHNIQUE:
            case CardSelectTask::SECRET_WEAPON:
            case CardSelectTask::SEEK:
                result["source"] = "DRAW_PILE";
                append_cards(bc_->cards.drawPile.begin(), bc_->cards.drawPile.end());
                break;
            case CardSelectTask::CODEX:
            case CardSelectTask::DISCOVERY:
                result["source"] = "GENERATED";
                for (int index = 0; index < 3; ++index) {
                    auto value = card_dict(CardInstance(bc_->cardSelectInfo.cards[index]));
                    value["choice_index"] = index;
                    options.append(value);
                }
                break;
            default:
                result["source"] = "GENERATED";
                break;
        }
        result["options"] = options;
        return result;
    }

    static py::dict action_dict(
        const char *kind,
        int card_index = -1,
        int potion_index = -1,
        int target_index = -1,
        int choice_index = -1) {
        py::dict result;
        result["kind"] = kind;
        result["command"] = py::none();
        if (card_index < 0) result["card_index"] = py::none();
        else result["card_index"] = card_index;
        if (potion_index < 0) result["potion_index"] = py::none();
        else result["potion_index"] = potion_index;
        if (target_index < 0) result["target_index"] = py::none();
        else result["target_index"] = target_index;
        if (choice_index < 0) result["choice_index"] = py::none();
        else result["choice_index"] = choice_index;
        return result;
    }

    void enumerate_normal_actions(py::list &result) const {
        for (int source = 0; source < bc_->cards.cardsInHand; ++source) {
            const auto &card = bc_->cards.hand[source];
            if (card.requiresTarget()) {
                for (int target = 0; target < bc_->monsters.monsterCount; ++target) {
                    search::Action action(search::ActionType::CARD, source, target);
                    if (action.isValidAction(*bc_)) {
                        result.append(action_dict("play", source + 1, -1, target));
                    }
                }
            } else {
                search::Action action(search::ActionType::CARD, source, 0);
                if (action.isValidAction(*bc_)) {
                    result.append(action_dict("play", source + 1));
                }
            }
        }
        for (int source = 0; source < bc_->potionCapacity; ++source) {
            const auto potion = bc_->potions[source];
            if (potion == Potion::INVALID || potion == Potion::EMPTY_POTION_SLOT) continue;
            if (potionRequiresTarget(potion)) {
                for (int target = 0; target < bc_->monsters.monsterCount; ++target) {
                    search::Action action(search::ActionType::POTION, source, target);
                    if (action.isValidAction(*bc_)) {
                        result.append(action_dict("potion", -1, source, target));
                    }
                }
            } else {
                search::Action action(search::ActionType::POTION, source, 0);
                if (action.isValidAction(*bc_) &&
                    !(potion == Potion::SMOKE_BOMB && !bc_->canUseSmokeBomb())) {
                    result.append(action_dict("potion", -1, source));
                }
            }
            search::Action discard(search::ActionType::POTION, source, 6);
            if (discard.isValidAction(*bc_)) {
                result.append(action_dict("discard_potion", -1, source));
            }
        }
        result.append(action_dict("end_turn"));
    }

    void enumerate_choice_actions(py::list &result) const {
        const auto task = bc_->cardSelectInfo.cardSelectTask;
        if (task == CardSelectTask::EXHAUST_MANY || task == CardSelectTask::GAMBLE ||
                task == CardSelectTask::RETAIN_CARDS ||
                task == CardSelectTask::WARCRY ||
                (task == CardSelectTask::LIQUID_MEMORIES_POTION &&
                 bc_->cardSelectInfo.pickCount > 1) ||
                (task == CardSelectTask::FORETHOUGHT &&
                 bc_->cardSelectInfo.canPickAnyNumber)) {
            const bool can_select_more = task == CardSelectTask::GAMBLE ||
                task == CardSelectTask::FORETHOUGHT ||
                selected_count() < bc_->cardSelectInfo.pickCount;
            if (can_select_more) {
                const int option_count = task == CardSelectTask::LIQUID_MEMORIES_POTION
                    ? static_cast<int>(bc_->cards.discardPile.size())
                    : bc_->cards.cardsInHand;
                for (int index = 0; index < option_count; ++index) {
                    if (!is_selected(index)) {
                        result.append(action_dict("choose", -1, -1, -1, index));
                    }
                }
            }
            if ((task != CardSelectTask::LIQUID_MEMORIES_POTION &&
                    task != CardSelectTask::WARCRY) ||
                    selected_count() == bc_->cardSelectInfo.pickCount) {
                result.append(action_dict("proceed"));
            }
            return;
        }
        for (const auto &action : search::Action::enumerateCardSelectActions(*bc_)) {
            if (action.isValidAction(*bc_)) {
                result.append(action_dict("choose", -1, -1, -1, action.getSelectIdx()));
            }
        }
    }
};

class LightspeedRunState {
public:
    void reset(
        std::uint64_t seed,
        int ascension = 0,
        const py::object &math_seed = py::none()) {
        if (ascension < 0 || ascension > 20) {
            throw std::invalid_argument("Ascension must be between 0 and 20");
        }
        gc_ = std::make_unique<GameContext>(CharacterClass::IRONCLAD, seed, ascension);
        battle_.reset();
        battle_action_count_ = 0;
        action_history_.clear();
        has_terminal_display_moves_ = false;
        math_seed_ = math_seed.is_none()
            ? seed - static_cast<std::uint64_t>(897897)
            : math_seed.cast<std::uint64_t>();
        gc_->mathUtilRng = Random(math_seed_);
        map_assign_burning_elite_ = true;
    }

    py::dict snapshot() {
        require_reset();
        py::dict result;
        py::dict run_state;
        run_state["seed"] = gc_->seed;
        run_state["math_seed"] = math_seed_;
        run_state["ascension"] = gc_->ascension;
        run_state["act"] = gc_->act;
        run_state["floor"] = gc_->floorNum;
        run_state["monster_list_offset"] = gc_->monsterListOffset;
        run_state["elite_monster_list_offset"] = gc_->eliteMonsterListOffset;
        run_state["second_boss"] = static_cast<int>(gc_->secondBoss);
        try {
            run_state["map"] = gc_->map->toString(true);
        } catch (const std::out_of_range &error) {
            throw std::out_of_range(std::string("snapshot.run_state.map: ") + error.what());
        }
        run_state["burning_elite_x"] = gc_->map->burningEliteX;
        run_state["burning_elite_y"] = gc_->map->burningEliteY;
        run_state["burning_elite_buff"] = gc_->map->burningEliteBuff;
        result["run_state"] = run_state;
        try {
            result["rng"] = full_run_rng_state(*gc_);
        } catch (const std::out_of_range &error) {
            throw std::out_of_range(std::string("snapshot.rng: ") + error.what());
        }
        const auto offset = gc_->act == 1 ? 1 : gc_->act * (100 * (gc_->act - 1));
        py::dict map_rng;
        map_rng["algorithm"] = "sts.RandomXS128/Map.fromSeed:v1";
        map_rng["base_seed"] = gc_->seed;
        map_rng["derived_seed"] = gc_->seed + static_cast<std::uint64_t>(offset);
        map_rng["act"] = gc_->act;
        map_rng["ascension"] = gc_->ascension;
        map_rng["assign_burning_elite"] = map_assign_burning_elite_;
        py::dict derived_rng;
        derived_rng["map"] = map_rng;
        result["derived_rng"] = derived_rng;
        try {
            result["ordered_pools"] = ordered_pool_state(*gc_);
            result["player_state"] = run_player_state(*gc_);
            result["public_inventory"] = public_inventory_state(*gc_, battle_.get());
        } catch (const std::out_of_range &error) {
            throw std::out_of_range(std::string("snapshot.inventory: ") + error.what());
        }
        py::dict progress;
        py::dict screen;
        try {
            progress = run_progress_state(*gc_);
            screen = screen_info_state(*gc_);
        } catch (const std::out_of_range &error) {
            throw std::out_of_range(std::string("snapshot.continuation: ") + error.what());
        }
        progress["screen_continuation_serialized"] = screen["complete"];
        result["progress_state"] = progress;
        result["screen_info"] = screen;
        try {
            result["public_screen"] = public_screen_state(*gc_);
        } catch (const std::out_of_range &error) {
            throw std::out_of_range(std::string("snapshot.public_screen: ") + error.what());
        }
        try {
            result["public_map"] = public_map_state(*gc_);
        } catch (const std::out_of_range &error) {
            throw std::out_of_range(std::string("snapshot.public_map: ") + error.what());
        }
        py::dict public_run;
        public_run["character_id"] = "IRONCLAD";
        public_run["ascension"] = gc_->ascension;
        public_run["act"] = gc_->act;
        public_run["floor"] = gc_->floorNum;
        public_run["gold"] = battle_ ? battle_->player.gold : gc_->gold;
        public_run["visible_boss_id"] = monsterEncounterEnumNames[static_cast<int>(gc_->boss)];
        public_run["outcome"] = static_cast<int>(gc_->outcome);
        public_run["screen_state"] = static_cast<int>(gc_->screenState);
        public_run["current_event_id"] = eventIdStrings[static_cast<int>(gc_->curEvent)];
        result["public_run"] = public_run;
        py::list replay_actions;
        for (const auto bits : action_history_) replay_actions.append(bits);
        result["replay_actions"] = replay_actions;
        // A combat card-selection boundary can retain the card currently
        // resolving plus queued cleanup/callback actions.  Those closures are
        // deliberately not serialized.  Exact FullRun checkpoints therefore
        // reconstruct such boundaries from the seed and canonical action
        // history, just as event combats already do for event continuations.
        result["replay_required"] = battle_ &&
            (gc_->curRoom == Room::EVENT || battle_->inputState == InputState::CARD_SELECT);
        if (has_terminal_display_moves_) {
            py::list moves;
            for (const auto move : terminal_display_moves_) {
                moves.append(static_cast<int>(move));
            }
            result["terminal_display_moves"] = moves;
        }
        if (battle_) {
            LightspeedBattle serializer;
            serializer.adopt_active_run(gc_, battle_);
            py::dict checkpoint;
            try {
                checkpoint = serializer.snapshot();
            } catch (...) {
                serializer.return_active_run(gc_, battle_);
                throw;
            }
            serializer.return_active_run(gc_, battle_);
            checkpoint["rng"] = checkpoint["_rng"];
            checkpoint.attr("pop")("_rng");
            result["combat_checkpoint"] = checkpoint;
            py::dict combat;
            combat["encounter"] = static_cast<int>(battle_->encounter);
            combat["turn"] = battle_->turn;
            combat["input_state"] = static_cast<int>(battle_->inputState);
            combat["outcome"] = static_cast<int>(battle_->outcome);
            combat["current_hp"] = battle_->player.curHp;
            combat["max_hp"] = battle_->player.maxHp;
            combat["block"] = battle_->player.block;
            combat["energy"] = battle_->player.energy;
            result["combat_state"] = combat;
            result["public_combat"] = public_combat_state(
                *battle_, has_terminal_display_moves_ ? &terminal_display_moves_ : nullptr);
            result["legal_actions"] = combat_legal_actions(*battle_);
        } else {
            try {
                result["legal_actions"] = run_legal_actions(*gc_);
            } catch (const std::out_of_range &error) {
                throw std::out_of_range(std::string("snapshot.legal_actions: ") + error.what());
            }
        }
        return result;
    }

    void load_state(const py::dict &state) {
        const auto run = state["run_state"].cast<py::dict>();
        const auto requested_history = state.contains("replay_actions")
            ? state["replay_actions"].cast<py::list>() : py::list();
        const bool replay_required = state.contains("replay_required") &&
            state["replay_required"].cast<bool>();
        const auto progress = state.contains("progress_state")
            ? state["progress_state"].cast<py::dict>() : py::dict();
        const auto screen = state.contains("screen_info")
            ? state["screen_info"].cast<py::dict>() : py::dict();
        const bool registeredLegacyEvent =
            !screen.empty() && !screen["complete"].cast<bool>() &&
            screen["screen_state"].cast<int>() == static_cast<int>(ScreenState::EVENT_SCREEN) &&
            !progress.empty() &&
            (progress["current_event"].cast<int>() == static_cast<int>(Event::GOLDEN_IDOL) ||
             progress["current_event"].cast<int>() == static_cast<int>(Event::THE_CLERIC));
        if ((state.contains("screen_info") &&
                !screen["complete"].cast<bool>() && !registeredLegacyEvent) ||
                replay_required) {
            if (!state.contains("replay_actions")) {
                throw std::invalid_argument(
                    "Incomplete screen continuation requires deterministic replay history");
            }
            reset(
                run["seed"].cast<std::uint64_t>(),
                run["ascension"].cast<int>(),
                py::int_(run["math_seed"].cast<std::uint64_t>()));
            for (const auto item : requested_history) step(item.cast<std::uint32_t>());
            const auto replayed = snapshot();
            // Checkpoints are persisted as JSON.  pybind snapshots may contain
            // tuples, which become lists after a JSON/gzip round trip even
            // though the checkpoint is otherwise byte-for-byte equivalent in
            // the durable representation.
            const auto json = py::module_::import("json");
            const auto normalized_replayed = json.attr("loads")(
                json.attr("dumps")(replayed, py::arg("sort_keys") = true));
            const auto normalized_requested = json.attr("loads")(
                json.attr("dumps")(state, py::arg("sort_keys") = true));
            // Legal actions are derived from the restored native state.  They
            // are deliberately not part of checkpoint identity: fixing action
            // enumeration must not make an otherwise exact historical state
            // unloadable.  The replay layer compares the freshly derived
            // canonical candidates at the restored boundary.
            normalized_replayed.attr("pop")("legal_actions", py::none());
            normalized_requested.attr("pop")("legal_actions", py::none());
            if (!normalized_replayed.equal(normalized_requested)) {
                throw std::invalid_argument(
                    "Deterministic replay does not reproduce the requested checkpoint");
            }
            return;
        }
        reset(
            run["seed"].cast<std::uint64_t>(),
            run["ascension"].cast<int>(),
            py::int_(run["math_seed"].cast<std::uint64_t>()));
        gc_->act = run["act"].cast<int>();
        gc_->floorNum = run["floor"].cast<int>();
        gc_->monsterListOffset = run["monster_list_offset"].cast<int>();
        gc_->eliteMonsterListOffset = run["elite_monster_list_offset"].cast<int>();
        gc_->secondBoss = static_cast<MonsterEncounter>(run["second_boss"].cast<int>());
        const auto map_rng = state["derived_rng"].cast<py::dict>()["map"].cast<py::dict>();
        if (map_rng["base_seed"].cast<std::uint64_t>() != gc_->seed ||
                map_rng["act"].cast<int>() != gc_->act ||
                map_rng["ascension"].cast<int>() != gc_->ascension) {
            throw std::invalid_argument("Map RNG derivation does not match run state");
        }
        map_assign_burning_elite_ = map_rng["assign_burning_elite"].cast<bool>();
        if (gc_->act == 4) {
            gc_->map = std::make_shared<Map>(Map::act4Map());
        } else {
            gc_->map = std::make_shared<Map>(Map::fromSeed(
                gc_->seed, gc_->ascension, gc_->act, map_assign_burning_elite_));
        }
        // The emerald key can be acquired before a later Act map is generated.
        // In that case the historical derivation flag may still be true while
        // the actual map legitimately has no burning elite.  The checkpoint's
        // concrete map state is authoritative for exact continuation.
        gc_->map->burningEliteX = run["burning_elite_x"].cast<int>();
        gc_->map->burningEliteY = run["burning_elite_y"].cast<int>();
        gc_->map->burningEliteBuff = run["burning_elite_buff"].cast<int>();
        restore_full_run_rng(*gc_, state["rng"].cast<py::dict>());
        restore_ordered_pools(*gc_, state["ordered_pools"].cast<py::dict>());
        if (state.contains("player_state")) {
            restore_run_player_state(*gc_, state["player_state"].cast<py::dict>());
        }
        if (state.contains("progress_state")) {
            restore_run_progress_state(*gc_, state["progress_state"].cast<py::dict>());
        }
        if (state.contains("screen_info")) {
            restore_screen_info(*gc_, state["screen_info"].cast<py::dict>());
        }
        if (gc_->screenState == ScreenState::BATTLE ||
                (state.contains("combat_checkpoint") &&
                 gc_->outcome == GameOutcome::PLAYER_LOSS)) {
            if (state.contains("combat_checkpoint")) {
                LightspeedBattle loader;
                loader.load_checkpoint(state["combat_checkpoint"].cast<py::dict>());
                battle_ = loader.release_loaded_battle();
                battle_action_count_ = 0;
            } else {
                start_battle();
            }
        }
        has_terminal_display_moves_ = false;
        if (state.contains("terminal_display_moves")) {
            const auto moves = state["terminal_display_moves"].cast<py::list>();
            if (moves.size() != terminal_display_moves_.size()) {
                throw std::invalid_argument("Terminal display move array has invalid length");
            }
            for (int index = 0; index < static_cast<int>(moves.size()); ++index) {
                terminal_display_moves_[index] =
                    static_cast<MMID>(moves[index].cast<int>());
            }
            has_terminal_display_moves_ = true;
        }
        action_history_.clear();
        for (const auto item : requested_history) {
            action_history_.push_back(item.cast<std::uint32_t>());
        }
    }

    py::list legal_actions() const {
        require_reset();
        return battle_ ? combat_legal_actions(*battle_) : run_legal_actions(*gc_);
    }

    void set_skip_battles_for_testing(bool enabled) {
        require_reset();
        if (battle_) {
            throw std::logic_error(
                "Battle skipping can only be changed outside combat");
        }
        gc_->skipBattles = enabled;
    }

    void set_discovery_retrieval_updates_for_validation(int updates) {
        require_reset();
        if (updates < 1 || updates > 120) {
            throw std::invalid_argument("Discovery retrieval updates are out of range");
        }
        if (!battle_ || battle_->inputState != InputState::CARD_SELECT ||
                battle_->cardSelectInfo.cardSelectTask != CardSelectTask::DISCOVERY ||
                !battle_->cardSelectInfo.discoveryRerollOnRetrieve) {
            throw std::logic_error(
                "Discovery timing evidence is invalid at the current boundary");
        }
        battle_->cardSelectInfo.discoveryRetrievalUpdates = updates;
    }

    void reset_last_hand_card_costs_for_validation(int count) {
        require_reset();
        if (!battle_ || count < 1 || count > battle_->cards.cardsInHand) {
            throw std::invalid_argument("Card Soul reset evidence is invalid");
        }
        const int begin = battle_->cards.cardsInHand - count;
        for (int index = begin; index < battle_->cards.cardsInHand; ++index) {
            battle_->cards.hand[index].costForTurn = battle_->cards.hand[index].cost;
        }
    }

    py::dict step(std::uint32_t bits) {
        require_reset();
        if (battle_) {
            const search::Action action(bits);
            if (!action.isValidAction(*battle_)) {
                throw std::invalid_argument("Combat action is not legal in the current state");
            }
            std::array<MMID, 7> pre_action_moves;
            for (int index = 0; index < static_cast<int>(pre_action_moves.size()); ++index) {
                pre_action_moves[index] = battle_->monsters.arr[index].moveHistory[0];
            }
            action.execute(*battle_);
            action_history_.push_back(bits);
            ++battle_action_count_;
            if (battle_->outcome != Outcome::UNDECIDED) {
                battle_->exitBattle(*gc_);
                battle_action_count_ = 0;
                if (battle_->outcome == Outcome::PLAYER_LOSS) {
                    terminal_display_moves_ = pre_action_moves;
                    has_terminal_display_moves_ = true;
                } else {
                    battle_.reset();
                    has_terminal_display_moves_ = false;
                }
            }
            return snapshot();
        }
        const search::GameAction action(bits);
        if (!action.isValidAction(*gc_)) {
            throw std::invalid_argument("Run action is not legal in the current state");
        }
        try {
            action.execute(*gc_);
        } catch (const std::out_of_range &error) {
            throw std::out_of_range(std::string("Run action execution: ") + error.what());
        }
        action_history_.push_back(bits);
        // afterBattle intentionally leaves the room's screen value untouched
        // at a terminal Act 3/4 victory.  A structural test may resolve combat
        // synchronously, so do not mistake that terminal screen value for a
        // request to initialize the boss battle again.
        if (gc_->screenState == ScreenState::BATTLE &&
                gc_->outcome == GameOutcome::UNDECIDED) {
            start_battle();
        }
        try {
            return snapshot();
        } catch (const std::out_of_range &error) {
            throw std::out_of_range(std::string("Run snapshot after action: ") + error.what());
        }
    }

    py::dict advance_all_rng() {
        require_reset();
        py::dict result;
        result["ai"] = gc_->aiRng.randomLong();
        result["card_random"] = gc_->cardRandomRng.randomLong();
        result["card"] = gc_->cardRng.randomLong();
        result["event"] = gc_->eventRng.randomLong();
        result["math_util"] = gc_->mathUtilRng.randomLong();
        result["merchant"] = gc_->merchantRng.randomLong();
        result["misc"] = gc_->miscRng.randomLong();
        result["monster_hp"] = gc_->monsterHpRng.randomLong();
        result["monster"] = gc_->monsterRng.randomLong();
        result["neow"] = gc_->neowRng.randomLong();
        result["potion"] = gc_->potionRng.randomLong();
        result["relic"] = gc_->relicRng.randomLong();
        result["shuffle"] = gc_->shuffleRng.randomLong();
        result["treasure"] = gc_->treasureRng.randomLong();
        return result;
    }

    py::dict courier_restock_probe(const std::string &purchased_card) {
        require_reset();
        const Card original = parse_card(purchased_card);
        const CardType original_type = original.getType();
        if (original_type != CardType::ATTACK &&
                original_type != CardType::SKILL &&
                original_type != CardType::POWER) {
            throw std::invalid_argument(
                "Courier colored restock probe requires an attack, skill, or power");
        }
        if (!gc_->hasRelic(RelicId::THE_COURIER)) {
            gc_->obtainRelic(RelicId::THE_COURIER);
        }
        gc_->gold = 999999;
        gc_->info.shop.cards[0] = original;
        gc_->info.shop.cardPrice(0) = 0;
        const auto rng_before = full_run_rng_state(*gc_);
        gc_->info.shop.buyCard(*gc_, 0);

        const Card &restocked = gc_->info.shop.cards[0];
        py::dict result;
        result["purchased_id"] = getCardEnumName(original.id);
        result["purchased_type"] = cardTypeStrings[static_cast<int>(original_type)];
        result["restocked_id"] = getCardEnumName(restocked.id);
        result["restocked_type"] = cardTypeStrings[static_cast<int>(restocked.getType())];
        result["restocked_rarity"] = cardRarityStrings[static_cast<int>(restocked.getRarity())];
        result["rng_before"] = rng_before;
        result["rng_after"] = full_run_rng_state(*gc_);
        return result;
    }

    py::dict scripted_playout() {
        require_reset();
        search::SimpleAgent agent;
        agent.playout(*gc_);
        action_history_.assign(agent.actionHistory.begin(), agent.actionHistory.end());
        auto result = snapshot();
        result["scripted_action_count"] = agent.actionHistory.size();
        py::list actions;
        for (const auto bits : agent.actionHistory) actions.append(bits);
        result["scripted_action_history"] = actions;
        return result;
    }

    py::dict resolve_battle_scripted() {
        require_reset();
        if (!battle_) {
            throw std::logic_error("Scripted battle resolution requires a battle screen");
        }
        search::SimpleAgent agent;
        agent.curGameContext = gc_.get();
        agent.playoutBattle(*battle_);
        action_history_.insert(
            action_history_.end(), agent.actionHistory.begin(), agent.actionHistory.end());
        battle_->exitBattle(*gc_);
        battle_action_count_ = 0;
        if (battle_->outcome != Outcome::PLAYER_LOSS) battle_.reset();
        auto result = snapshot();
        result["scripted_combat_action_count"] = agent.actionHistory.size();
        return result;
    }

    py::dict scripted_playout_act1() {
        require_reset();
        search::SimpleAgent agent;
        agent.curGameContext = gc_.get();
        const auto act_one_boss = gc_->boss;
        BattleContext battle;
        while (gc_->outcome == GameOutcome::UNDECIDED && gc_->act == 1) {
            if (gc_->screenState == ScreenState::BATTLE) {
                battle = BattleContext();
                battle.init(*gc_);
                agent.playoutBattle(battle);
                battle.exitBattle(*gc_);
            } else {
                agent.stepOutOfCombat(*gc_);
            }
        }
        action_history_.assign(agent.actionHistory.begin(), agent.actionHistory.end());
        auto result = snapshot();
        result["scripted_action_count"] = agent.actionHistory.size();
        result["act_one_success"] = gc_->act > 1;
        result["act_one_boss"] = monsterEncounterEnumNames[static_cast<int>(act_one_boss)];
        return result;
    }

    py::dict search_battle_suffix(std::int64_t simulations) const {
        require_reset();
        if (!battle_) {
            throw std::logic_error("Battle suffix search requires a battle screen");
        }
        if (simulations <= 0) {
            throw std::invalid_argument("Battle suffix search budget must be positive");
        }
        search::BattleScumSearcher2 searcher(*battle_);
        searcher.search(simulations);
        py::dict result;
        result["found"] = searcher.outcomePlayerHp > 0;
        result["outcome_player_hp"] = searcher.outcomePlayerHp;
        result["requested_simulations"] = simulations;
        result["completed_simulations"] = searcher.root.simulationCount;
        py::list actions;
        for (const auto &action : searcher.bestActionSequence) {
            actions.append(action.bits);
        }
        result["action_bits"] = actions;
        return result;
    }

private:
    std::unique_ptr<GameContext> gc_;
    std::unique_ptr<BattleContext> battle_;
    int battle_action_count_ = 0;
    std::vector<std::uint32_t> action_history_;
    std::array<MMID, 7> terminal_display_moves_ {};
    bool has_terminal_display_moves_ = false;
    std::uint64_t math_seed_ = 0;
    bool map_assign_burning_elite_ = true;

    void require_reset() const {
        if (!gc_) throw std::logic_error("Run state has not been reset");
    }

    void start_battle() {
        battle_ = std::make_unique<BattleContext>();
        battle_->init(*gc_, gc_->info.encounter);
        battle_action_count_ = 0;
        has_terminal_display_moves_ = false;
    }
};

py::dict rng_probe(std::uint64_t seed) {
    Random rng(seed);
    py::dict initial;
    initial["counter"] = rng.counter;
    initial["seed0"] = rng.seed0;
    initial["seed1"] = rng.seed1;

    py::dict values;
    values["range_999"] = rng.random(999);
    values["between_5_12"] = rng.random(5, 12);
    values["long_range"] = rng.random(static_cast<std::int64_t>(1000000000000LL));
    values["random_long"] = rng.randomLong();
    values["boolean"] = rng.randomBoolean();
    values["chance_0_375"] = rng.randomBoolean(0.375F);
    values["unit_float"] = rng.random();
    values["float_range"] = rng.random(5.0F);
    values["float_between"] = rng.random(-2.0F, 3.0F);

    py::dict final;
    final["counter"] = rng.counter;
    final["seed0"] = rng.seed0;
    final["seed1"] = rng.seed1;

    py::dict result;
    result["seed_bits"] = seed;
    result["initial"] = initial;
    result["values"] = values;
    result["final"] = final;
    return result;
}

py::list shuffle_probe(std::uint64_t seed) {
    std::array<int, 10> values {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    java::Collections::shuffle(values.begin(), values.end(), java::Random(seed));
    py::list result;
    for (const auto value : values) result.append(value);
    return result;
}

py::dict action_queue_probe() {
    BattleContext bc;
    std::vector<int> order;
    bc.addToBot({[&order](BattleContext &) { order.push_back(1); }});
    bc.addToBot({[&order](BattleContext &) { order.push_back(2); }});
    bc.addToTop({[&order](BattleContext &) { order.push_back(3); }});
    bc.addToTop({[&order](BattleContext &) { order.push_back(4); }});
    while (!bc.actionQueue.isEmpty()) {
        auto action = bc.actionQueue.popFront();
        action(bc);
    }

    std::vector<int> post_victory;
    bc.addToBot({[&post_victory](BattleContext &) { post_victory.push_back(1); }, false});
    bc.addToBot({[&post_victory](BattleContext &) { post_victory.push_back(2); }, true});
    bc.addToBot({[&post_victory](BattleContext &) { post_victory.push_back(3); }, false});
    bc.outcome = Outcome::PLAYER_VICTORY;
    bc.clearPostCombatActions();
    while (!bc.actionQueue.isEmpty()) {
        auto action = bc.actionQueue.popFront();
        action(bc);
    }

    py::dict result;
    result["mixed_top_bottom"] = order;
    result["post_victory_retained"] = post_victory;
    return result;
}

py::dict card_color_probe() {
    const std::array<CardId, 8> ids {
        CardId::BRUTALITY,
        CardId::BUFFER,
        CardId::BULLET_TIME,
        CardId::BRILLIANCE,
        CardId::COMBUST,
        CardId::COMPILE_DRIVER,
        CardId::CONCENTRATE,
        CardId::COLLECT,
    };
    py::dict result;
    for (const auto id : ids) {
        result[getCardEnumName(id)] = cardColorStrings[static_cast<int>(getCardColor(id))];
    }
    return result;
}

py::list card_metadata_probe() {
    py::list result;
    for (int ordinal = static_cast<int>(CardId::INVALID) + 1;
         ordinal <= static_cast<int>(CardId::ZAP);
         ++ordinal) {
        const auto id = static_cast<CardId>(ordinal);
        py::dict card;
        card["enum_id"] = getCardEnumName(id);
        card["string_id"] = getCardStringId(id);
        card["color"] = cardColorStrings[static_cast<int>(getCardColor(id))];
        card["type"] = cardTypeStrings[static_cast<int>(getCardType(id))];
        card["rarity"] = cardRarityStrings[static_cast<int>(getCardRarity(id))];
        card["cost"] = getEnergyCost(id, false);
        card["upgraded_cost"] = getEnergyCost(id, true);
        card["base_damage"] = getBaseDamage(id, false);
        card["upgraded_base_damage"] = getBaseDamage(id, true);
        card["targets_enemy"] = cardTargetsEnemy(id, false);
        card["upgraded_targets_enemy"] = cardTargetsEnemy(id, true);
        card["ethereal"] = isCardEthereal(id, false);
        card["upgraded_ethereal"] = isCardEthereal(id, true);
        card["innate"] = isCardInnate(id, false);
        card["upgraded_innate"] = isCardInnate(id, true);
        card["exhaust"] = doesCardExhaust(id, false);
        card["upgraded_exhaust"] = doesCardExhaust(id, true);
        card["self_retain"] = doesCardSelfRetain(id, false);
        card["upgraded_self_retain"] = doesCardSelfRetain(id, true);
        card["x_cost"] = isXCost(id);
        result.append(card);
    }
    return result;
}

py::list potion_metadata_probe() {
    py::list result;
    for (int ordinal = static_cast<int>(Potion::EMPTY_POTION_SLOT) + 1;
         ordinal <= static_cast<int>(Potion::WEAK_POTION);
         ++ordinal) {
        const auto id = static_cast<Potion>(ordinal);
        py::dict potion;
        potion["enum_id"] = potionEnumNames[ordinal];
        potion["string_id"] = potionIds[ordinal];
        potion["rarity"] = static_cast<int>(getPotionRarity(id));
        potion["requires_target"] = potionRequiresTarget(id);
        result.append(potion);
    }
    return result;
}

py::list relic_metadata_probe() {
    py::list result;
    for (int ordinal = static_cast<int>(RelicId::AKABEKO);
         ordinal < static_cast<int>(RelicId::INVALID);
         ++ordinal) {
        const auto id = static_cast<RelicId>(ordinal);
        py::dict relic;
        relic["enum_id"] = relicEnumNames[ordinal];
        relic["string_id"] = relicIds[ordinal];
        relic["tier"] = relicTierStrings[static_cast<int>(getRelicTier(id))];
        result.append(relic);
    }
    return result;
}

py::dict run_fairy_potion_probe() {
    auto revive_hp = [](bool sacred_bark) {
        GameContext gc(CharacterClass::IRONCLAD, 23, 0);
        gc.maxHp = 100;
        gc.curHp = 0;
        gc.potions[0] = Potion::FAIRY_POTION;
        gc.potionCount = 1;
        if (sacred_bark) {
            gc.relics.add({RelicId::SACRED_BARK, 0});
        }
        gc.playerOnDie();
        return gc.curHp;
    };
    py::dict result;
    result["normal"] = revive_hp(false);
    result["sacred_bark"] = revive_hp(true);
    return result;
}

py::dict smoke_bomb_core_probe() {
    GameContext gc(CharacterClass::IRONCLAD, 19, 0);
    gc.floorNum = 1;
    gc.curRoom = Room::MONSTER;
    gc.miscRng = Random(gc.seed + gc.floorNum);
    BattleContext battle;
    battle.init(gc, MonsterEncounter::TWO_LOUSE);
    bool reward_callback_called = false;
    gc.regainControlAction = [&reward_callback_called](GameContext &) {
        reward_callback_called = true;
    };
    battle.potions[0] = Potion::SMOKE_BOMB;
    battle.potionCount = 1;
    search::Action smoke(search::ActionType::POTION, 0, 0);
    const bool normal_legal = smoke.isValidAction(battle);
    smoke.execute(battle);
    const bool escaped = battle.outcome == Outcome::PLAYER_ESCAPE;
    battle.exitBattle(gc);

    py::dict blocked;
    const std::array<MonsterEncounter, 10> bosses {
        MonsterEncounter::SLIME_BOSS,
        MonsterEncounter::THE_GUARDIAN,
        MonsterEncounter::HEXAGHOST,
        MonsterEncounter::AUTOMATON,
        MonsterEncounter::COLLECTOR,
        MonsterEncounter::CHAMP,
        MonsterEncounter::AWAKENED_ONE,
        MonsterEncounter::TIME_EATER,
        MonsterEncounter::DONU_AND_DECA,
        MonsterEncounter::THE_HEART,
    };
    for (const auto encounter : bosses) {
        GameContext boss_gc(CharacterClass::IRONCLAD, 20, 0);
        boss_gc.floorNum = 16;
        boss_gc.curRoom = Room::BOSS;
        boss_gc.miscRng = Random(boss_gc.seed + boss_gc.floorNum);
        BattleContext boss;
        boss.init(boss_gc, encounter);
        boss.potions[0] = Potion::SMOKE_BOMB;
        boss.potionCount = 1;
        blocked[monsterEncounterEnumNames[static_cast<int>(encounter)]] =
            !smoke.isValidAction(boss);
    }
    GameContext act4_gc(CharacterClass::IRONCLAD, 21, 0);
    act4_gc.act = 4;
    act4_gc.floorNum = 52;
    act4_gc.curRoom = Room::ELITE;
    act4_gc.miscRng = Random(act4_gc.seed + act4_gc.floorNum);
    BattleContext act4;
    act4.init(act4_gc, MonsterEncounter::SHIELD_AND_SPEAR);
    act4.potions[0] = Potion::SMOKE_BOMB;
    act4.potionCount = 1;

    py::dict result;
    result["normal_legal"] = normal_legal;
    result["escaped"] = escaped;
    result["map_screen"] = gc.screenState == ScreenState::MAP_SCREEN;
    result["reward_callback_called"] = reward_callback_called;
    result["bosses_blocked"] = blocked;
    result["back_attack_blocked"] = !smoke.isValidAction(act4);
    return result;
}

py::dict stance_mechanics_probe() {
    BattleContext bc;
    bc.player.stance = Stance::CALM;
    bc.player.energy = 0;

    auto change = Actions::ChangeStance(Stance::WRATH).actFunc;
    change(bc);
    while (!bc.actionQueue.isEmpty()) {
        auto action = bc.actionQueue.popFront();
        action(bc);
    }
    py::dict calm_exit;
    calm_exit["stance"] = stanceStrings[static_cast<int>(bc.player.stance)];
    calm_exit["energy"] = bc.player.energy;

    auto mantra = Actions::BuffPlayer<PS::MANTRA>(12).actFunc;
    mantra(bc);
    while (!bc.actionQueue.isEmpty()) {
        auto action = bc.actionQueue.popFront();
        action(bc);
    }
    py::dict divinity;
    divinity["stance"] = stanceStrings[static_cast<int>(bc.player.stance)];
    divinity["energy"] = bc.player.energy;
    divinity["mantra"] = bc.player.getStatus<PS::MANTRA>();

    py::dict result;
    result["calm_exit"] = calm_exit;
    result["divinity_entry"] = divinity;
    return result;
}

py::dict orb_mechanics_probe() {
    GameContext gc(CharacterClass::IRONCLAD, 41, 0);
    gc.floorNum = 1;
    gc.curRoom = Room::MONSTER;
    gc.miscRng = Random(gc.seed + gc.floorNum);
    BattleContext bc;
    bc.init(gc, MonsterEncounter::TWO_LOUSE);
    auto drain = [&bc]() {
        while (!bc.actionQueue.isEmpty()) {
            auto action = bc.actionQueue.popFront();
            action(bc);
        }
    };

    bc.player.increaseOrbSlots(2);
    bc.player.setStatusValueNoChecks<PS::FOCUS>(2);
    const int hp_before = bc.monsters.arr[0].curHp + bc.monsters.arr[1].curHp;
    bc.player.channelOrb(bc, Orb::LIGHTNING);
    bc.player.channelOrb(bc, Orb::FROST);
    bc.player.channelOrb(bc, Orb::DARK);  // Full slots auto-evoke Lightning.
    drain();

    py::dict auto_evoke;
    auto_evoke["damage"] = hp_before - bc.monsters.arr[0].curHp - bc.monsters.arr[1].curHp;
    auto_evoke["first"] = static_cast<int>(bc.player.orbs[0]);
    auto_evoke["second"] = static_cast<int>(bc.player.orbs[1]);
    auto_evoke["dark_evoke"] = bc.player.orbEvokeAmounts[1];

    bc.player.triggerEndOfTurnOrbs(bc);
    drain();
    py::dict passive;
    passive["block"] = bc.player.block;
    passive["dark_evoke"] = bc.player.orbEvokeAmounts[1];

    bc.player.evokeOrb(bc);  // Frost.
    drain();
    py::dict frost_evoke;
    frost_evoke["block"] = bc.player.block;
    frost_evoke["first"] = static_cast<int>(bc.player.orbs[0]);

    bc.monsters.arr[0].curHp = 50;
    bc.monsters.arr[1].curHp = 20;
    bc.player.evokeOrb(bc);  // Dark targets the lowest current HP.
    drain();
    py::dict dark_evoke;
    dark_evoke["first_hp"] = bc.monsters.arr[0].curHp;
    dark_evoke["second_hp"] = bc.monsters.arr[1].curHp;

    bc.player.channelOrb(bc, Orb::FUSION);
    const int energy_before = bc.player.energy;
    bc.player.triggerEndOfTurnOrbs(bc);
    drain();
    bc.player.evokeOrb(bc);
    drain();
    py::dict plasma;
    plasma["energy_gained"] = bc.player.energy - energy_before;

    for (int i = 0; i < Player::MAX_ORB_SLOTS; ++i) {
        bc.player.orbs[i] = Orb::EMPTY;
        bc.player.orbEvokeAmounts[i] = 0;
    }
    bc.player.orbSlots = 2;
    bc.player.energy = 0;
    bc.player.setHasRelic<R::GOLD_PLATED_CABLES>(false);
    bc.player.orbs[0] = Orb::FUSION;
    bc.player.orbs[1] = Orb::FUSION;
    bc.triggerStartOfTurnOrbs();
    drain();
    py::dict start_turn;
    start_turn["each_plasma"] = bc.player.energy;

    bc.player.energy = 0;
    bc.player.setHasRelic<R::GOLD_PLATED_CABLES>(true);
    bc.player.orbs[1] = Orb::EMPTY;
    bc.triggerStartOfTurnOrbs();
    drain();
    start_turn["cables_first_plasma"] = bc.player.energy;

    bc.player.energy = 0;
    bc.player.orbs[0] = Orb::LIGHTNING;
    bc.player.orbs[1] = Orb::FUSION;
    bc.triggerStartOfTurnOrbs();
    drain();
    start_turn["cables_non_plasma_first"] = bc.player.energy;

    bc.player.increaseOrbSlots(99);
    py::dict result;
    result["slot_cap"] = bc.player.orbSlots;
    result["auto_evoke"] = auto_evoke;
    result["passive"] = passive;
    result["frost_evoke"] = frost_evoke;
    result["dark_evoke"] = dark_evoke;
    result["plasma"] = plasma;
    result["start_turn"] = start_turn;
    return result;
}

py::dict damage_pipeline_probe() {
    auto fresh_battle = [](GameContext &gc, BattleContext &bc) {
        gc.floorNum = 1;
        gc.curRoom = Room::MONSTER;
        gc.miscRng = Random(gc.seed + gc.floorNum);
        bc.init(gc, MonsterEncounter::JAW_WORM);
        bc.player.curHp = 80;
        bc.player.maxHp = 80;
        bc.player.block = 0;
    };

    py::dict result;

    {
        GameContext gc(CharacterClass::IRONCLAD, 51, 0);
        BattleContext bc;
        fresh_battle(gc, bc);
        bc.player.buff<PS::INTANGIBLE>(1);
        bc.player.damage(bc, 10, false);
        result["intangible_damage"] = 80 - bc.player.curHp;
    }

    {
        GameContext gc(CharacterClass::IRONCLAD, 52, 0);
        BattleContext bc;
        fresh_battle(gc, bc);
        bc.player.block = 1;
        bc.player.buff<PS::INTANGIBLE>(1);
        bc.player.buff<PS::BUFFER>(1);
        bc.player.damage(bc, 10, false);
        py::dict value;
        value["damage"] = 80 - bc.player.curHp;
        value["block"] = bc.player.block;
        value["buffer"] = bc.player.getStatus<PS::BUFFER>();
        result["intangible_block_buffer"] = value;
    }

    {
        GameContext gc(CharacterClass::IRONCLAD, 53, 0);
        BattleContext bc;
        fresh_battle(gc, bc);
        bc.player.block = 99;
        bc.player.buff<PS::BUFFER>(1);
        bc.player.loseHp(bc, 5, true);
        py::dict value;
        value["damage"] = 80 - bc.player.curHp;
        value["block"] = bc.player.block;
        value["buffer"] = bc.player.getStatus<PS::BUFFER>();
        result["hp_loss_buffer"] = value;
    }

    {
        GameContext gc(CharacterClass::IRONCLAD, 54, 0);
        BattleContext bc;
        fresh_battle(gc, bc);
        bc.player.setHasRelic<RelicId::TORII>(true);
        bc.player.setHasRelic<RelicId::TUNGSTEN_ROD>(true);
        bc.player.attacked(bc, 0, 5);
        result["torii_tungsten_five"] = 80 - bc.player.curHp;
        bc.player.attacked(bc, 0, 6);
        result["torii_threshold_six"] = 80 - bc.player.curHp;
    }

    {
        GameContext gc(CharacterClass::IRONCLAD, 55, 0);
        BattleContext bc;
        fresh_battle(gc, bc);
        bc.player.block = 4;
        bc.player.setHasRelic<RelicId::TORII>(true);
        bc.player.setHasRelic<RelicId::TUNGSTEN_ROD>(true);
        bc.player.attacked(bc, 0, 8);
        py::dict value;
        value["damage"] = 80 - bc.player.curHp;
        value["block"] = bc.player.block;
        result["block_before_relics"] = value;
    }

    {
        GameContext gc(CharacterClass::IRONCLAD, 56, 0);
        BattleContext bc;
        fresh_battle(gc, bc);
        bc.player.buff<PS::BUFFER>(1);
        bc.player.attacked(bc, 0, 7);
        bc.player.attacked(bc, 0, 7);
        py::dict value;
        value["damage"] = 80 - bc.player.curHp;
        value["buffer"] = bc.player.getStatus<PS::BUFFER>();
        result["buffer_multi_hit"] = value;
    }

    return result;
}

py::dict curl_up_lethal_probe() {
    auto fresh_louse = [](GameContext &gc, BattleContext &bc) -> Monster & {
        gc.floorNum = 1;
        gc.curRoom = Room::MONSTER;
        gc.miscRng = Random(gc.seed + gc.floorNum);
        bc.init(gc, MonsterEncounter::TWO_LOUSE);
        auto &monster = bc.monsters.arr[0];
        monster.block = 0;
        monster.setStatus<MS::CURL_UP>(4);
        return monster;
    };
    auto drain = [](BattleContext &bc) {
        while (!bc.actionQueue.isEmpty()) {
            auto action = bc.actionQueue.popFront();
            action(bc);
        }
    };

    py::dict result;
    {
        GameContext gc(CharacterClass::IRONCLAD, 73, 0);
        BattleContext bc;
        auto &monster = fresh_louse(gc, bc);
        monster.curHp = 5;
        monster.maxHp = 5;
        monster.attacked(bc, 5);
        drain(bc);
        py::dict value;
        value["hp"] = monster.curHp;
        value["block"] = monster.block;
        value["curl_up"] = monster.getStatus<MS::CURL_UP>();
        result["lethal"] = value;
    }
    {
        GameContext gc(CharacterClass::IRONCLAD, 74, 0);
        BattleContext bc;
        auto &monster = fresh_louse(gc, bc);
        monster.curHp = 6;
        monster.maxHp = 6;
        monster.attacked(bc, 5);
        drain(bc);
        py::dict value;
        value["hp"] = monster.curHp;
        value["block"] = monster.block;
        value["curl_up"] = monster.getStatus<MS::CURL_UP>();
        result["nonlethal"] = value;
    }
    {
        GameContext gc(CharacterClass::IRONCLAD, 75, 0);
        BattleContext bc;
        auto &monster = fresh_louse(gc, bc);
        monster.curHp = 10;
        monster.maxHp = 10;
        bc.addToBot(Actions::AttackEnemy(monster.idx, 7));
        bc.addToBot(Actions::AttackEnemy(monster.idx, 7));
        drain(bc);
        py::dict value;
        value["hp"] = monster.curHp;
        value["block"] = monster.block;
        value["curl_up"] = monster.getStatus<MS::CURL_UP>();
        result["multi_hit_lethal"] = value;
    }
    return result;
}

py::dict just_applied_probe() {
    BattleContext bc;
    auto &player = bc.player;

    player.debuff<PS::WEAK>(2, true);
    player.debuff<PS::VULNERABLE>(2, true);
    player.debuff<PS::FRAIL>(2, true);
    player.applyAtEndOfRoundPowers(bc);
    py::dict first_round;
    first_round["weak"] = player.getStatus<PS::WEAK>();
    first_round["vulnerable"] = player.getStatus<PS::VULNERABLE>();
    first_round["frail"] = player.getStatus<PS::FRAIL>();
    first_round["weak_just_applied"] = player.wasJustApplied<PS::WEAK>();
    player.applyAtEndOfRoundPowers(bc);
    py::dict second_round;
    second_round["weak"] = player.getStatus<PS::WEAK>();
    second_round["vulnerable"] = player.getStatus<PS::VULNERABLE>();
    second_round["frail"] = player.getStatus<PS::FRAIL>();

    BattleContext non_monster;
    non_monster.player.debuff<PS::WEAK>(2, false);
    non_monster.player.applyAtEndOfRoundPowers(non_monster);

    BattleContext stacked;
    stacked.player.debuff<PS::WEAK>(2, true);
    stacked.player.debuff<PS::WEAK>(1, true);
    stacked.player.applyAtEndOfRoundPowers(stacked);

    BattleContext timed_buffs;
    timed_buffs.player.buff<PS::INTANGIBLE>(1);
    timed_buffs.player.buff<PS::DOUBLE_DAMAGE>(1);
    timed_buffs.player.applyAtEndOfRoundPowers(timed_buffs);
    py::dict buffs;
    buffs["intangible"] = timed_buffs.player.getStatus<PS::INTANGIBLE>();
    buffs["double_damage"] = timed_buffs.player.getStatus<PS::DOUBLE_DAMAGE>();

    BattleContext draw_reduction;
    const int base_draw = draw_reduction.player.cardDrawPerTurn;
    draw_reduction.player.debuff<PS::DRAW_REDUCTION>(1, true);
    draw_reduction.player.debuff<PS::DRAW_REDUCTION>(1, true);
    py::dict draw;
    draw["after_stack"] = draw_reduction.player.cardDrawPerTurn;
    draw_reduction.player.applyAtEndOfRoundPowers(draw_reduction);
    draw["after_first_round"] = draw_reduction.player.cardDrawPerTurn;
    draw["present_after_first_round"] =
        draw_reduction.player.hasStatus<PS::DRAW_REDUCTION>();
    draw_reduction.player.applyAtEndOfRoundPowers(draw_reduction);
    while (!draw_reduction.actionQueue.isEmpty()) {
        auto action = draw_reduction.actionQueue.popFront();
        action(draw_reduction);
    }
    draw["after_second_round"] = draw_reduction.player.cardDrawPerTurn;
    draw["present_after_second_round"] =
        draw_reduction.player.hasStatus<PS::DRAW_REDUCTION>();
    draw["base"] = base_draw;

    py::dict result;
    result["monster_applied_first_round"] = first_round;
    result["monster_applied_second_round"] = second_round;
    result["non_monster_applied_after_round"] =
        non_monster.player.getStatus<PS::WEAK>();
    result["stacked_new_power_after_round"] =
        stacked.player.getStatus<PS::WEAK>();
    result["timed_buffs_after_round"] = buffs;
    result["draw_reduction"] = draw;
    return result;
}

py::dict retain_ethereal_probe() {
    auto setup = [](GameContext &gc, BattleContext &bc,
                    std::initializer_list<CardInstance> hand) {
        gc.floorNum = 1;
        gc.curRoom = Room::MONSTER;
        gc.miscRng = Random(gc.seed + gc.floorNum);
        bc.init(gc, MonsterEncounter::JAW_WORM);
        bc.cards.cardsInHand = 0;
        bc.cards.drawPile.clear();
        bc.cards.discardPile.clear();
        bc.cards.exhaustPile.clear();
        int unique_id = 100;
        for (auto card : hand) {
            card.uniqueId = unique_id++;
            bc.cards.hand[bc.cards.cardsInHand++] = card;
        }
    };
    auto drain = [](BattleContext &bc) {
        while (!bc.actionQueue.isEmpty() &&
                bc.inputState != InputState::CARD_SELECT) {
            auto action = bc.actionQueue.popFront();
            action(bc);
        }
    };
    auto names = [](const auto &begin, const auto &end) {
        py::list result;
        for (auto it = begin; it != end; ++it) result.append(it->getName());
        return result;
    };
    auto zones = [&names](const BattleContext &bc) {
        py::dict value;
        value["hand"] = names(
            bc.cards.hand.begin(), bc.cards.hand.begin() + bc.cards.cardsInHand);
        value["discard"] = names(bc.cards.discardPile.begin(), bc.cards.discardPile.end());
        value["exhaust"] = names(bc.cards.exhaustPile.begin(), bc.cards.exhaustPile.end());
        return value;
    };

    py::dict result;
    {
        GameContext gc(CharacterClass::IRONCLAD, 61, 0);
        BattleContext bc;
        CardInstance retained(CardId::GHOSTLY_ARMOR);
        retained.retain = true;
        setup(gc, bc, {retained, CardInstance(CardId::GHOSTLY_ARMOR),
                       CardInstance(CardId::DEFEND_RED)});
        bc.discardAtEndOfTurn();
        drain(bc);
        auto value = zones(bc);
        value["retained_flag_reset"] = !bc.cards.hand[0].retain;
        result["explicit_retain_beats_ethereal"] = value;
    }
    {
        GameContext gc(CharacterClass::IRONCLAD, 62, 0);
        BattleContext bc;
        setup(gc, bc, {CardInstance(CardId::GHOSTLY_ARMOR),
                       CardInstance(CardId::DEFEND_RED)});
        bc.player.setHasRelic<R::RUNIC_PYRAMID>(true);
        bc.discardAtEndOfTurn();
        drain(bc);
        result["runic_pyramid"] = zones(bc);
    }
    {
        GameContext gc(CharacterClass::IRONCLAD, 63, 0);
        BattleContext bc;
        setup(gc, bc, {CardInstance(CardId::GHOSTLY_ARMOR),
                       CardInstance(CardId::DEFEND_RED)});
        bc.player.buff<PS::EQUILIBRIUM>(1);
        bc.discardAtEndOfTurn();
        drain(bc);
        result["equilibrium"] = zones(bc);
    }
    {
        GameContext gc(CharacterClass::IRONCLAD, 64, 0);
        BattleContext bc;
        setup(gc, bc, {CardInstance(CardId::PROTECT),
                       CardInstance(CardId::DEFEND_RED)});
        bc.discardAtEndOfTurn();
        drain(bc);
        result["self_retain"] = zones(bc);
    }
    {
        GameContext gc(CharacterClass::IRONCLAD, 65, 0);
        BattleContext bc;
        setup(gc, bc, {CardInstance(CardId::GHOSTLY_ARMOR),
                       CardInstance(CardId::DEFEND_RED)});
        fixed_list<int, 10> selected;
        selected.push_back(0);
        selected.push_back(1);
        bc.chooseRetainCards(selected);
        py::dict value;
        value["ethereal_selected"] = bc.cards.hand[0].retain;
        value["normal_selected"] = bc.cards.hand[1].retain;
        result["manual_retain_selection"] = value;
    }
    {
        GameContext gc(CharacterClass::IRONCLAD, 66, 0);
        BattleContext bc;
        setup(gc, bc, {CardInstance(CardId::PROTECT),
                       CardInstance(CardId::DEFEND_RED)});
        const int original_cost = bc.cards.hand[0].cost;
        bc.player.buff<PS::ESTABLISHMENT>(1);
        bc.player.buff<PS::RETAIN_CARDS>(1);
        bc.player.applyEndOfTurnPowers(bc);
        drain(bc);
        py::dict value;
        value["task"] = cardSelectTaskStrings[
            static_cast<int>(bc.cardSelectInfo.cardSelectTask)];
        value["pick_count"] = bc.cardSelectInfo.pickCount;
        value["can_pick_zero"] = bc.cardSelectInfo.canPickZero;
        value["self_retain_cost_reduction"] =
            original_cost - bc.cards.hand[0].cost;
        result["power_hooks"] = value;
    }

    return result;
}

py::dict card_turn_lifecycle_probe() {
    GameContext gc(CharacterClass::IRONCLAD, 67, 0);
    gc.floorNum = 1;
    gc.curRoom = Room::MONSTER;
    gc.miscRng = Random(gc.seed + gc.floorNum);
    BattleContext bc;
    bc.init(gc, MonsterEncounter::JAW_WORM);
    bc.cards.cardsInHand = 1;
    bc.cards.hand[0] = CardInstance(CardId::PRIDE);
    bc.cards.hand[0].uniqueId = 100;
    bc.cards.drawPile.clear();
    CardInstance defend(CardId::DEFEND_RED);
    defend.uniqueId = 101;
    bc.cards.drawPile.push_back(defend);

    const int rng_before = bc.cardRandomRng.counter;
    bc.callEndOfTurnActions();
    while (!bc.actionQueue.isEmpty()) {
        auto action = bc.actionQueue.popFront();
        action(bc);
    }

    py::dict pride;
    pride["innate"] = isCardInnate(CardId::PRIDE);
    pride["draw_size"] = static_cast<int>(bc.cards.drawPile.size());
    pride["top"] = bc.cards.drawPile.back().getName();
    pride["copy_has_new_identity"] =
        bc.cards.drawPile.back().uniqueId != bc.cards.hand[0].uniqueId;
    pride["rng_calls"] = bc.cardRandomRng.counter - rng_before;

    BattleContext queued;
    queued.cards.cardsInHand = 3;
    queued.cards.hand[0] = CardInstance(CardId::BURN);
    queued.cards.hand[1] = CardInstance(CardId::REGRET);
    queued.cards.hand[2] = CardInstance(CardId::DOUBT);
    queued.callEndOfTurnActions();
    py::dict no_trigger;
    no_trigger["queued"] = queued.cardQueue.size;
    auto burn = queued.cardQueue.popFront();
    auto regret = queued.cardQueue.popFront();
    auto doubt = queued.cardQueue.popFront();
    no_trigger["order"] = py::make_tuple(
        burn.card.getName(), regret.card.getName(), doubt.card.getName());
    no_trigger["regret_hand_count"] = regret.regretCardCount;
    no_trigger["trigger_on_use"] =
        burn.triggerOnUse || regret.triggerOnUse || doubt.triggerOnUse;

    py::dict result;
    result["pride"] = pride;
    result["no_trigger_cards"] = no_trigger;
    return result;
}

py::dict power_turn_lifecycle_probe() {
    auto setup = [](GameContext &gc, BattleContext &bc) {
        gc.floorNum = 1;
        gc.curRoom = Room::MONSTER;
        gc.miscRng = Random(gc.seed + gc.floorNum);
        bc.init(gc, MonsterEncounter::TWO_LOUSE);
        bc.actionQueue.clear();
    };
    auto drain = [](BattleContext &bc) {
        while (!bc.actionQueue.isEmpty()) {
            auto action = bc.actionQueue.popFront();
            action(bc);
        }
    };

    py::dict loop;
    {
        GameContext gc(CharacterClass::DEFECT, 68, 0);
        BattleContext bc;
        setup(gc, bc);
        bc.player.orbSlots = 1;
        bc.player.orbs[0] = Orb::FUSION;
        bc.player.energy = 0;
        bc.player.buff<PS::LOOP>(2);
        bc.player.applyStartOfTurnPowers(bc);
        drain(bc);
        loop["plasma_energy"] = bc.player.energy;
    }
    {
        GameContext gc(CharacterClass::DEFECT, 69, 0);
        BattleContext bc;
        setup(gc, bc);
        bc.player.orbSlots = 1;
        bc.player.orbs[0] = Orb::DARK;
        bc.player.orbEvokeAmounts[0] = 6;
        bc.player.buff<PS::LOOP>(2);
        bc.player.applyStartOfTurnPowers(bc);
        loop["dark_evoke"] = bc.player.orbEvokeAmounts[0];
    }

    py::dict cables;
    {
        GameContext gc(CharacterClass::DEFECT, 70, 0);
        BattleContext bc;
        setup(gc, bc);
        bc.player.orbSlots = 2;
        bc.player.orbs[0] = Orb::FROST;
        bc.player.orbs[1] = Orb::DARK;
        bc.player.orbEvokeAmounts[1] = 6;
        bc.player.setHasRelic<R::GOLD_PLATED_CABLES>(true);
        bc.player.triggerEndOfTurnOrbs(bc);
        drain(bc);
        cables["block"] = bc.player.block;
        cables["dark_evoke"] = bc.player.orbEvokeAmounts[1];
    }

    py::dict brutality;
    {
        GameContext gc(CharacterClass::IRONCLAD, 71, 0);
        BattleContext bc;
        setup(gc, bc);
        bc.cards.cardsInHand = 0;
        bc.cards.drawPile.clear();
        CardInstance card(CardId::DEFEND_RED);
        card.uniqueId = 100;
        bc.cards.drawPile.push_back(card);
        bc.player.curHp = 20;
        bc.player.buff<PS::BRUTALITY>(1);
        bc.player.applyStartOfTurnPostDrawPowers(bc);

        const int hp_before = bc.player.curHp;
        auto first = bc.actionQueue.popFront();
        first(bc);
        brutality["first_action_draws"] = bc.cards.cardsInHand == 1;
        brutality["hp_after_first"] = bc.player.curHp;
        auto second = bc.actionQueue.popFront();
        second(bc);
        brutality["hp_lost_after_second"] = hp_before - bc.player.curHp;
    }

    py::dict result;
    result["loop"] = loop;
    result["cables_end_turn"] = cables;
    result["brutality"] = brutality;
    return result;
}

py::dict stable_power_order_probe() {
    GameContext gc(CharacterClass::IRONCLAD, 72, 0);
    gc.floorNum = 1;
    gc.curRoom = Room::MONSTER;
    gc.miscRng = Random(gc.seed + gc.floorNum);
    BattleContext bc;
    bc.init(gc, MonsterEncounter::TWO_LOUSE);
    bc.actionQueue.clear();

    bc.player.buff<PS::DEMON_FORM>(1);
    bc.player.buff<PS::BRUTALITY>(1);
    bc.player.buff<PS::DRAW_CARD_NEXT_TURN>(1);
    auto encode_order = [](const Player &player) {
        py::list result;
        for (const auto &[status, amount] : player.orderedPowers()) {
            result.append(playerStatusEnumStrings[static_cast<int>(status)]);
        }
        return result;
    };

    py::dict result;
    result["initial"] = encode_order(bc.player);
    bc.player.removeStatus<PS::DEMON_FORM>();
    bc.player.buff<PS::DEMON_FORM>(1);
    result["after_reapply"] = encode_order(bc.player);

    bc.player.removeStatus<PS::DEMON_FORM>();
    bc.player.removeStatus<PS::BRUTALITY>();
    bc.player.removeStatus<PS::DRAW_CARD_NEXT_TURN>();
    bc.player.buff<PS::DEMON_FORM>(1);
    bc.player.buff<PS::BRUTALITY>(1);
    bc.player.applyStartOfTurnPostDrawPowers(bc);
    const int strength_before = bc.player.strength;
    auto first = bc.actionQueue.popFront();
    first(bc);
    result["first_callback_is_demon_form"] = bc.player.strength == strength_before + 1;
    return result;
}

py::dict complete_power_order_probe() {
    Monster monster;
    monster.buff<MS::FLIGHT>(3);
    monster.buff<MS::METALLICIZE>(2);
    monster.buff<MS::REACTIVE>(10);
    monster.addDebuff<MS::WEAK>(2, false);
    monster.buff<MS::INTANGIBLE>(1);

    auto encode_monster = [](const auto &powers) {
        py::list result;
        for (const auto &[status, amount] : powers) {
            result.append(monsterStatusEnumStrings[static_cast<int>(status)]);
        }
        return result;
    };
    const auto traversal_snapshot = monster.orderedPowers();

    py::dict result;
    result["monster_initial"] = encode_monster(traversal_snapshot);
    monster.removeStatus<MS::FLIGHT>();
    monster.buff<MS::FLIGHT>(3);
    result["monster_after_reapply"] = encode_monster(monster.orderedPowers());
    monster.removeStatus<MS::REACTIVE>();
    monster.buff<MS::INVINCIBLE>(300);
    result["snapshot_after_mutation"] = encode_monster(traversal_snapshot);
    result["monster_current"] = encode_monster(monster.orderedPowers());

    BattleContext player_context;
    auto &player = player_context.player;
    player.buff<PS::WEAK>(2);
    player.buff<PS::DOUBLE_DAMAGE>(1);
    player.buff<PS::FRAIL>(2);
    player.applyAtEndOfRoundPowers(player_context);
    py::dict player_round;
    player_round["double_damage"] = player.getStatus<PS::DOUBLE_DAMAGE>();
    player_round["frail"] = player.getStatus<PS::FRAIL>();
    player_round["weak"] = player.getStatus<PS::WEAK>();
    result["player_end_round"] = player_round;
    return result;
}

}  // namespace

PYBIND11_MODULE(_lightspeed, module) {
    module.doc() = "Canonical FullRun sts_lightspeed bridge and rule probes";
    module.def("rng_probe", &rng_probe, py::arg("seed"));
    module.def("shuffle_probe", &shuffle_probe, py::arg("seed"));
    module.def("action_queue_probe", &action_queue_probe);
    module.def("card_color_probe", &card_color_probe);
    module.def("card_metadata_probe", &card_metadata_probe);
    module.def("potion_metadata_probe", &potion_metadata_probe);
    module.def("relic_metadata_probe", &relic_metadata_probe);
    module.def("run_fairy_potion_probe", &run_fairy_potion_probe);
    module.def("smoke_bomb_core_probe", &smoke_bomb_core_probe);
    module.def("stance_mechanics_probe", &stance_mechanics_probe);
    module.def("orb_mechanics_probe", &orb_mechanics_probe);
    module.def("damage_pipeline_probe", &damage_pipeline_probe);
    module.def("curl_up_lethal_probe", &curl_up_lethal_probe);
    module.def("just_applied_probe", &just_applied_probe);
    module.def("retain_ethereal_probe", &retain_ethereal_probe);
    module.def("card_turn_lifecycle_probe", &card_turn_lifecycle_probe);
    module.def("power_turn_lifecycle_probe", &power_turn_lifecycle_probe);
    module.def("stable_power_order_probe", &stable_power_order_probe);
    module.def("complete_power_order_probe", &complete_power_order_probe);
    py::class_<LightspeedBattle>(module, "LightspeedBattle")
        .def(py::init<>())
        .def("reset", &LightspeedBattle::reset,
             py::arg("seed"), py::arg("encounter"), py::arg("ascension") = 0,
             py::arg("deck") = std::vector<std::string>{},
             py::arg("relics") = std::vector<std::string>{},
             py::arg("replace_relics") = false)
        .def("set_card_piles", &LightspeedBattle::set_card_piles,
             py::arg("hand"), py::arg("draw"), py::arg("discard"),
             py::arg("exhaust"))
        .def("reset_card_probe", &LightspeedBattle::reset_card_probe,
             py::arg("seed"), py::arg("card_id"), py::arg("upgraded"))
        .def("set_player_health", &LightspeedBattle::set_player_health,
             py::arg("current_hp"), py::arg("max_hp"))
        .def("apply_scenario", &LightspeedBattle::apply_scenario,
             py::arg("scenario"))
        .def("set_potions", &LightspeedBattle::set_potions, py::arg("potions"))
        .def("set_rng_state", &LightspeedBattle::set_rng_state, py::arg("rng"))
        .def("set_discovery_retrieval_updates",
             &LightspeedBattle::set_discovery_retrieval_updates,
             py::arg("updates"))
        .def("load_checkpoint", &LightspeedBattle::load_checkpoint,
             py::arg("checkpoint"))
        .def("step", &LightspeedBattle::step,
             py::arg("kind"), py::arg("card_index") = -1,
             py::arg("potion_index") = -1, py::arg("target_index") = -1,
             py::arg("choice_index") = -1)
        .def("snapshot", &LightspeedBattle::snapshot);
    py::class_<LightspeedRunState>(module, "LightspeedRunState")
        .def(py::init<>())
        .def("reset", &LightspeedRunState::reset,
             py::arg("seed"), py::arg("ascension") = 0,
             py::arg("math_seed") = py::none())
        .def("snapshot", &LightspeedRunState::snapshot)
        .def("load_state", &LightspeedRunState::load_state, py::arg("state"))
        .def("legal_actions", &LightspeedRunState::legal_actions)
        .def("_set_skip_battles_for_testing",
             &LightspeedRunState::set_skip_battles_for_testing,
             py::arg("enabled"))
        .def("_set_discovery_retrieval_updates_for_validation",
             &LightspeedRunState::set_discovery_retrieval_updates_for_validation,
             py::arg("updates"))
        .def("_reset_last_hand_card_costs_for_validation",
             &LightspeedRunState::reset_last_hand_card_costs_for_validation,
             py::arg("count"))
        .def("step", &LightspeedRunState::step, py::arg("bits"))
        .def("advance_all_rng", &LightspeedRunState::advance_all_rng)
        .def("courier_restock_probe", &LightspeedRunState::courier_restock_probe,
             py::arg("purchased_card"))
        .def("scripted_playout", &LightspeedRunState::scripted_playout)
        .def("scripted_playout_act1", &LightspeedRunState::scripted_playout_act1)
        .def("resolve_battle_scripted", &LightspeedRunState::resolve_battle_scripted)
        .def("search_battle_suffix", &LightspeedRunState::search_battle_suffix,
             py::arg("simulations"));
    module.attr("lightspeed_commit") = "7476a81954020087da31d41d16fddf475746ec2d";
}

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
    if (p.strength != 0) result.append(power("STRENGTH", p.strength, "Strength"));
    if (p.dexterity != 0) result.append(power("DEXTERITY", p.dexterity, "Dexterity"));
    if (p.focus != 0) result.append(power("FOCUS", p.focus, "Focus"));
    if (p.artifact != 0) result.append(power("ARTIFACT", p.artifact, "Artifact"));
    for (const auto &[status, amount] : p.statusMap) {
        if (!p.hasStatusRuntime(status)) continue;
        result.append(power(
            playerStatusEnumStrings[static_cast<int>(status)], amount,
            playerStatusStrings[static_cast<int>(status)]));
    }
    constexpr PlayerStatus boolean_statuses[] = {
        PS::CONFUSED, PS::HEX, PS::BARRICADE, PS::BLASPHEMER,
        PS::CORRUPTION, PS::ELECTRO, PS::SURROUNDED,
        PS::MASTER_REALITY, PS::PEN_NIB, PS::WRATH_NEXT_TURN,
    };
    for (const auto status : boolean_statuses) {
        if (!p.hasStatusRuntime(status)) continue;
        result.append(power(
            playerStatusEnumStrings[static_cast<int>(status)], 1,
            playerStatusStrings[static_cast<int>(status)]));
    }
    return result;
}

py::list monster_powers(const Monster &m) {
    py::list result;
    if (m.strength != 0) {
        result.append(power("Strength", m.strength, "Strength"));
    }
    for (int i = 0; i < static_cast<int>(MS::INVALID); ++i) {
        const auto status = static_cast<MS>(i);
        if (status == MS::STRENGTH) continue;
        if (!m.hasStatusInternal(status)) continue;
        result.append(power(
            enemyStatusStrings[i], m.getStatusInternal(status),
            enemyStatusStrings[i]));
    }
    return result;
}

const char *intent_name(const Monster &monster) {
    switch (monster.moveHistory[0]) {
        case MMID::GENERIC_ESCAPE_MOVE:
        case MMID::LOOTER_ESCAPE:
            return "ESCAPE";
        case MMID::ACID_SLIME_L_LICK:
        case MMID::ACID_SLIME_M_LICK:
        case MMID::ACID_SLIME_S_LICK:
        case MMID::GREEN_LOUSE_SPIT_WEB:
        case MMID::LAGAVULIN_SIPHON_SOUL:
        case MMID::RED_SLAVER_ENTANGLE:
        case MMID::SENTRY_BOLT:
        case MMID::SLIME_BOSS_GOOP_SPRAY:
        case MMID::SPIKE_SLIME_L_LICK:
        case MMID::SPIKE_SLIME_M_LICK:
        case MMID::THE_GUARDIAN_VENT_STEAM:
            return "DEBUFF";
        case MMID::ACID_SLIME_L_CORROSIVE_SPIT:
        case MMID::ACID_SLIME_M_CORROSIVE_SPIT:
        case MMID::BLUE_SLAVER_RAKE:
        case MMID::FAT_GREMLIN_SMASH:
        case MMID::GREMLIN_NOB_SKULL_BASH:
        case MMID::HEXAGHOST_INFERNO:
        case MMID::HEXAGHOST_SEAR:
        case MMID::RED_SLAVER_SCRAPE:
        case MMID::SPIKE_SLIME_L_FLAME_TACKLE:
        case MMID::SPIKE_SLIME_M_FLAME_TACKLE:
            return "ATTACK_DEBUFF";
        case MMID::CULTIST_INCANTATION:
        case MMID::FUNGI_BEAST_GROW:
        case MMID::GREMLIN_NOB_BELLOW:
        case MMID::HEXAGHOST_INFLAME:
        case MMID::RED_LOUSE_GROW:
            return "BUFF";
        case MMID::JAW_WORM_BELLOW:
            return "DEFEND_BUFF";
        case MMID::JAW_WORM_THRASH:
            return "ATTACK_DEFEND";
        case MMID::LOOTER_SMOKE_BOMB:
        case MMID::SHIELD_GREMLIN_PROTECT:
        case MMID::THE_GUARDIAN_CHARGING_UP:
        case MMID::THE_GUARDIAN_DEFENSIVE_MODE:
            return "DEFEND";
        case MMID::LAGAVULIN_SLEEP:
            return "SLEEP";
        case MMID::HEXAGHOST_ACTIVATE:
        case MMID::GREMLIN_WIZARD_CHARGING:
        case MMID::SLIME_BOSS_PREPARING:
        case MMID::ACID_SLIME_L_SPLIT:
        case MMID::SLIME_BOSS_SPLIT:
        case MMID::SPIKE_SLIME_L_SPLIT:
            return "UNKNOWN";
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

py::dict rng_state(const Random &rng) {
    py::dict result;
    result["counter"] = rng.counter;
    result["seed0"] = rng.seed0;
    result["seed1"] = rng.seed1;
    return result;
}

int relic_counter(const RelicInstance &relic, const Player &player) {
    switch (relic.id) {
        case RelicId::BURNING_BLOOD: return -1;
        case RelicId::HAPPY_FLOWER: return player.happyFlowerCounter;
        case RelicId::INCENSE_BURNER: return player.incenseBurnerCounter;
        case RelicId::INK_BOTTLE: return player.inkBottleCounter;
        case RelicId::NUNCHAKU: return player.nunchakuCounter;
        case RelicId::PEN_NIB: return player.penNibCounter;
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
    if (state.contains("screen_continuation_serialized") &&
            !state["screen_continuation_serialized"].cast<bool>()) {
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
            result["event_data"] = gc.info.eventData;
            if (gc.curEvent == Event::NEOW) {
                py::list options;
                for (const auto &option : gc.info.neowRewards) {
                    py::dict value;
                    value["bonus"] = static_cast<int>(option.r);
                    value["drawback"] = static_cast<int>(option.d);
                    options.append(value);
                }
                result["neow_options"] = options;
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
            result["continuation"] = "map";
            break;
        case ScreenState::BOSS_RELIC_REWARDS: {
            py::list relics;
            for (const auto relic : gc.info.bossRelics) relics.append(static_cast<int>(relic));
            result["boss_relics"] = relics;
            break;
        }
        case ScreenState::CARD_SELECT: {
            // The callback after selection depends on the event/reward/shop
            // that opened this screen and cannot be inferred from these fields.
            result["complete"] = false;
            result["transform_rng"] = static_cast<int>(gc.info.transformRng);
            result["select_type"] = static_cast<int>(gc.info.selectScreenType);
            result["select_count"] = gc.info.toSelectCount;
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
            result["continuation"] = "map";
            break;
        case ScreenState::SHOP_ROOM:
            result["shop"] = shop_state(gc.info.shop);
            result["continuation"] = "map";
            break;
        case ScreenState::MAP_SCREEN:
        case ScreenState::REST_ROOM:
        case ScreenState::BATTLE:
        case ScreenState::INVALID:
            break;
    }
    return result;
}

void restore_screen_info(GameContext &gc, const py::dict &state) {
    if (state["screen_state"].cast<int>() != static_cast<int>(gc.screenState)) {
        throw std::invalid_argument("Screen info does not match progress screen state");
    }
    if (!state["complete"].cast<bool>()) {
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
            }
            break;
        case ScreenState::REWARDS:
            gc.info.rewardsContainer = restore_rewards(state["rewards"].cast<py::dict>());
            gc.info.stolenGold = state["stolen_gold"].cast<int>();
            break;
        case ScreenState::BOSS_RELIC_REWARDS: {
            const auto relics = state["boss_relics"].cast<py::list>();
            if (relics.size() != 3) throw std::invalid_argument("Boss relic checkpoint must contain three relics");
            for (int i = 0; i < 3; ++i) gc.info.bossRelics[i] = static_cast<RelicId>(relics[i].cast<int>());
            break;
        }
        case ScreenState::CARD_SELECT: {
            gc.info.transformRng = static_cast<RngReference>(state["transform_rng"].cast<int>());
            gc.info.selectScreenType = static_cast<CardSelectScreenType>(state["select_type"].cast<int>());
            gc.info.toSelectCount = state["select_count"].cast<int>();
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
            break;
        case ScreenState::SHOP_ROOM:
            gc.info.shop = restore_shop(state["shop"].cast<py::dict>());
            break;
        default:
            break;
    }
    if (state.contains("continuation")) {
        const auto continuation = state["continuation"].cast<std::string>();
        if (continuation == "map") {
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
        gc_->regainControlAction = [](GameContext &) {};
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
        bc_ = std::make_unique<BattleContext>();
        bc_->init(*gc_, parse_encounter(encounter));
        finalized_ = false;
        escaped_ = false;
        multi_select_bits_ = 0;
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

    void load_checkpoint(const py::dict &checkpoint) {
        const auto game = checkpoint["game_state"].cast<py::dict>();
        const auto combat = game["combat_state"].cast<py::dict>();
        const auto input_state = game["input_state"].cast<std::string>();
        if (input_state != "PLAYER_NORMAL") {
            throw std::invalid_argument(
                "Checkpoint restore currently requires PLAYER_NORMAL input state");
        }

        reset(
            game["seed"].cast<std::uint64_t>(),
            game["encounter"].cast<std::string>(),
            game["ascension_level"].cast<int>(),
            {}, {}, false);

        restore_relics(game);

        restore_player(combat["player"].cast<py::dict>());
        restore_cards(combat);
        restore_monsters(combat);

        bc_->turn = combat["turn"].cast<int>() - 1;
        const auto combat_internal = combat["_internal"].cast<py::dict>();
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
        bc_->inputState = InputState::PLAYER_NORMAL;
        bc_->outcome = Outcome::UNDECIDED;
        bc_->actionQueue.clear();
        bc_->cardQueue.clear();

        const auto rng = checkpoint["rng"].cast<py::dict>();
        restore_rng(bc_->aiRng, rng["ai"].cast<py::dict>());
        restore_rng(bc_->monsterHpRng, rng["monster_hp"].cast<py::dict>());
        restore_rng(bc_->shuffleRng, rng["shuffle"].cast<py::dict>());
        restore_rng(bc_->cardRandomRng, rng["card_random"].cast<py::dict>());
        restore_rng(bc_->miscRng, rng["misc"].cast<py::dict>());
        restore_rng(bc_->potionRng, rng["potion"].cast<py::dict>());

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
        search::Action action;
        const auto task = bc_->cardSelectInfo.cardSelectTask;
        const bool multi_select = bc_->inputState == InputState::CARD_SELECT &&
            (task == CardSelectTask::EXHAUST_MANY || task == CardSelectTask::GAMBLE);
        if (kind == "choose" && multi_select) {
            if (choice_index < 0 || choice_index >= bc_->cards.cardsInHand) {
                throw std::invalid_argument("Refusing invalid multi-select card index");
            }
            const std::uint32_t bit = 1U << choice_index;
            if ((multi_select_bits_ & bit) != 0) {
                throw std::invalid_argument("CommunicationMod cannot unselect a chosen card");
            }
            if (task == CardSelectTask::EXHAUST_MANY &&
                selected_count() >= bc_->cardSelectInfo.pickCount) {
                throw std::invalid_argument("Multi-select card limit reached");
            }
            multi_select_bits_ |= bit;
            return;
        }
        if (kind == "potion" && potion_index >= 0 && potion_index < bc_->potionCapacity &&
            bc_->potions[potion_index] == Potion::SMOKE_BOMB) {
            if (is_boss_encounter()) {
                throw std::invalid_argument("Smoke Bomb cannot be used in a boss combat");
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
        if (bc_->inputState == InputState::CARD_SELECT &&
            (bc_->cardSelectInfo.cardSelectTask == CardSelectTask::EXHAUST_MANY ||
             bc_->cardSelectInfo.cardSelectTask == CardSelectTask::GAMBLE)) {
            multi_select_bits_ = 0;
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
        game["floor"] = bc_->floorNum;
        game["seed"] = bc_->seed;
        game["encounter"] = monsterEncounterEnumNames[static_cast<int>(bc_->encounter)];
        game["class"] = "IRONCLAD";
        game["ascension_level"] = bc_->ascension;
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
            value["counter"] = relic_counter(relic, bc_->player);
            relics.append(value);
        }
        game["relics"] = relics;

        const bool terminal = escaped_ || bc_->outcome != Outcome::UNDECIDED;
        const char *outcome = escaped_ ? "ESCAPED" : bc_->outcome == Outcome::PLAYER_VICTORY
            ? "PLAYER_VICTORY"
            : bc_->outcome == Outcome::PLAYER_LOSS ? "PLAYER_LOSS" : "UNDECIDED";
        payload["outcome"] = outcome;
        game["outcome"] = outcome;
        game["room_phase"] = terminal ? "COMPLETE" : "COMBAT";
        game["input_state"] = bc_->inputState == InputState::PLAYER_NORMAL
            ? "PLAYER_NORMAL"
            : bc_->inputState == InputState::CARD_SELECT ? "CARD_SELECT" : "INTERNAL";
        if (!terminal) game["combat_state"] = combat_state();
        payload["game_state"] = game;

        py::dict rng;
        rng["ai"] = rng_state(bc_->aiRng);
        rng["monster_hp"] = rng_state(bc_->monsterHpRng);
        rng["shuffle"] = rng_state(bc_->shuffleRng);
        rng["card_random"] = rng_state(bc_->cardRandomRng);
        rng["misc"] = rng_state(bc_->miscRng);
        rng["potion"] = rng_state(bc_->potionRng);
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
                    task == CardSelectTask::GAMBLE) {
                    commands.append("proceed");
                }
                enumerate_choice_actions(actions);
            }
        }
        payload["available_commands"] = commands;
        payload["_legal_actions"] = actions;
        return payload;
    }

private:
    std::unique_ptr<GameContext> gc_;
    std::unique_ptr<BattleContext> bc_;
    bool finalized_ = false;
    bool escaped_ = false;
    std::uint32_t multi_select_bits_ = 0;

    int selected_count() const {
        auto bits = multi_select_bits_;
        int count = 0;
        while (bits != 0) {
            count += bits & 1U;
            bits >>= 1U;
        }
        return count;
    }

    bool is_boss_encounter() const {
        return bc_->encounter == MonsterEncounter::SLIME_BOSS ||
            bc_->encounter == MonsterEncounter::THE_GUARDIAN ||
            bc_->encounter == MonsterEncounter::HEXAGHOST;
    }

    static CardInstance restore_card(const py::dict &value) {
        const auto id = value["id"].cast<std::string>();
        const int upgrades = value["upgrades"].cast<int>();
        CardInstance card(parse_card(id + (upgrades > 0 ? "+" + std::to_string(upgrades) : "")));
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
            card.uniqueId = static_cast<std::int16_t>(bc_->cards.nextUniqueCardId++);
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
            monster.id = parse_monster(value["monster_id"].cast<std::string>());
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
        player["card_draw_per_turn"] = bc_->player.cardDrawPerTurn;
        player["cards_played_this_turn"] = bc_->player.cardsPlayedThisTurn;
        player["attacks_played_this_turn"] = bc_->player.attacksPlayedThisTurn;
        player["skills_played_this_turn"] = bc_->player.skillsPlayedThisTurn;
        player["powers"] = player_powers(bc_->player);
        py::dict player_internal;
        player_internal["just_applied_bits"] = bc_->player.justAppliedBits;
        player_internal["gold"] = bc_->player.gold;
        player_internal["stance"] = static_cast<int>(bc_->player.stance);
        player_internal["orb_slots"] = bc_->player.orbSlots;
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
            const auto damage = monster.getMoveBaseDamage(*bc_);
            py::dict value;
            value["id"] = monster.getName();
            value["name"] = monster.getName();
            value["monster_id"] = monsterIdStrings[static_cast<int>(monster.id)];
            value["current_hp"] = monster.curHp;
            value["max_hp"] = monster.maxHp;
            value["block"] = monster.block;
            value["intent"] = intent_name(monster);
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
            monster_internal["move_previous"] = monsterMoveStrings[static_cast<int>(monster.moveHistory[1])];
            monster_internal["is_escaping"] = monster.isEscapingB;
            monster_internal["escape_next"] = monster.escapeNext;
            value["_internal"] = monster_internal;
            value["move_base_damage"] = damage.damage;
            value["move_adjusted_damage"] = damage.damage > 0
                ? monster.calculateDamageToPlayer(*bc_, damage.damage) : 0;
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
        py::list potion_ids;
        for (int index = 0; index < 5; ++index) {
            potion_ids.append(static_cast<int>(bc_->potions[index]));
        }
        combat_internal["potion_ids"] = potion_ids;
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
        py::list options;

        auto append_cards = [this, &options](const auto &begin, const auto &end) {
            int index = 0;
            for (auto it = begin; it != end; ++it, ++index) {
                auto value = card_dict(*it);
                value["choice_index"] = index;
                value["selected"] = (multi_select_bits_ & (1U << index)) != 0;
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
                    !(potion == Potion::SMOKE_BOMB && is_boss_encounter())) {
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
        if (task == CardSelectTask::EXHAUST_MANY || task == CardSelectTask::GAMBLE) {
            const bool can_select_more = task == CardSelectTask::GAMBLE ||
                selected_count() < bc_->cardSelectInfo.pickCount;
            if (can_select_more) {
                for (int index = 0; index < bc_->cards.cardsInHand; ++index) {
                    if ((multi_select_bits_ & (1U << index)) == 0) {
                        result.append(action_dict("choose", -1, -1, -1, index));
                    }
                }
            }
            result.append(action_dict("proceed"));
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
        math_seed_ = math_seed.is_none()
            ? seed - static_cast<std::uint64_t>(897897)
            : math_seed.cast<std::uint64_t>();
        gc_->mathUtilRng = Random(math_seed_);
        map_assign_burning_elite_ = true;
    }

    py::dict snapshot() const {
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
        run_state["map"] = gc_->map->toString(true);
        run_state["burning_elite_x"] = gc_->map->burningEliteX;
        run_state["burning_elite_y"] = gc_->map->burningEliteY;
        run_state["burning_elite_buff"] = gc_->map->burningEliteBuff;
        result["run_state"] = run_state;
        result["rng"] = full_run_rng_state(*gc_);
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
        result["ordered_pools"] = ordered_pool_state(*gc_);
        result["player_state"] = run_player_state(*gc_);
        auto progress = run_progress_state(*gc_);
        auto screen = screen_info_state(*gc_);
        progress["screen_continuation_serialized"] = screen["complete"];
        result["progress_state"] = progress;
        result["screen_info"] = screen;
        result["legal_actions"] = run_legal_actions(*gc_);
        return result;
    }

    void load_state(const py::dict &state) {
        const auto run = state["run_state"].cast<py::dict>();
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
    }

    py::list legal_actions() const {
        require_reset();
        return run_legal_actions(*gc_);
    }

    py::dict step(std::uint32_t bits) {
        require_reset();
        const search::GameAction action(bits);
        if (!action.isValidAction(*gc_)) {
            throw std::invalid_argument("Run action is not legal in the current state");
        }
        action.execute(*gc_);
        return snapshot();
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

private:
    std::unique_ptr<GameContext> gc_;
    std::uint64_t math_seed_ = 0;
    bool map_assign_burning_elite_ = true;

    void require_reset() const {
        if (!gc_) throw std::logic_error("Run state has not been reset");
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

}  // namespace

PYBIND11_MODULE(_lightspeed, module) {
    module.doc() = "Step-wise sts_lightspeed battle bridge for spirecomm";
    module.def("rng_probe", &rng_probe, py::arg("seed"));
    module.def("shuffle_probe", &shuffle_probe, py::arg("seed"));
    module.def("action_queue_probe", &action_queue_probe);
    module.def("stance_mechanics_probe", &stance_mechanics_probe);
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
        .def("set_player_health", &LightspeedBattle::set_player_health,
             py::arg("current_hp"), py::arg("max_hp"))
        .def("set_potions", &LightspeedBattle::set_potions, py::arg("potions"))
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
        .def("step", &LightspeedRunState::step, py::arg("bits"))
        .def("advance_all_rng", &LightspeedRunState::advance_all_rng)
        .def("courier_restock_probe", &LightspeedRunState::courier_restock_probe,
             py::arg("purchased_card"));
    module.attr("lightspeed_commit") = "7476a81954020087da31d41d16fddf475746ec2d";
}

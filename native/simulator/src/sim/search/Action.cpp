//
// Created by keega on 9/17/2021.
//

#include "sim/search/Action.h"

#include <functional>

using namespace sts;

search::Action::Action(std::uint32_t bits) : bits(bits) {}
search::Action::Action(search::ActionType actionType) : bits(static_cast<int>(actionType) << 29) {}
search::Action::Action(search::ActionType actionType, int idx1) : bits((static_cast<int>(actionType) << 29) | (idx1 & 0xFFFF)) {}
search::Action::Action(search::ActionType actionType, int idx1, int idx2) : bits((static_cast<int>(actionType) << 29) | ((idx2 & 0x1FFF) << 16) | (idx1 & 0xFFFF)) {}
//
//search::Action::Action(search::ActionType actionType) : Action(actionType, 0, 0) {}
//
//search::Action::Action(search::ActionType actionType, int idx1) : Action(actionType, idx1, 0) {}
//
//search::Action::Action(search::ActionType actionType, int idx1, int idx2) : actionType(actionType), idx1(idx1),
//                                                                            idx2(idx2) {}

bool search::Action::operator==(const search::Action &rhs) const {
    return bits == rhs.bits;
//    return actionType == rhs.actionType && idx1 == rhs.idx1 && idx2 == rhs.idx2;
}

search::Action search::Action::orderedHandSelection(const std::vector<int> &indices) {
    if (indices.size() > 10) throw std::invalid_argument("Too many hand selections");
    constexpr unsigned factorial[] = {1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880};
    std::vector<int> remaining{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    unsigned rank = 0;
    for (unsigned i = 0; i < indices.size(); ++i) {
        const auto found = std::find(remaining.begin(), remaining.end(), indices[i]);
        if (found == remaining.end()) throw std::invalid_argument("Invalid or duplicate hand selection");
        rank += (found - remaining.begin()) * factorial[9 - i];
        remaining.erase(found);
    }
    return Action(static_cast<std::uint32_t>(
        (3U << 29) | (1U << 28) | (indices.size() << 24) | rank));
}

bool search::Action::operator!=(const search::Action &rhs) const {
    return !(rhs == *this);
}

search::ActionType search::Action::getActionType() const {
    return static_cast<ActionType>((bits >> 29) & 0xF);
//    return actionType;
}

int search::Action::getSourceIdx() const {
//    return idx1;
    return bits & 0xFFFF;
}

int search::Action::getTargetIdx() const {
//    return idx2;
    return (bits >> 16) & 0x1FFF;
}

int search::Action::getSelectIdx() const {
//    return idx1;
    return bits & 0xFFFF;
}

fixed_list<int, 10> search::Action::getSelectedIdxs() const {
    fixed_list<int,10> ret;
    if (bits & (1U << 28)) {
        const unsigned count = (bits >> 24) & 0xF;
        unsigned rank = bits & 0xFFFFFF;
        constexpr unsigned factorial[] = {1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880};
        if (count > 10) throw std::invalid_argument("Invalid ordered selection count");
        std::vector<int> remaining{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (unsigned i = 0; i < count; ++i) {
            const unsigned digit = rank / factorial[9 - i];
            rank %= factorial[9 - i];
            if (digit >= remaining.size()) throw std::invalid_argument("Invalid ordered selection rank");
            ret.push_back(remaining[digit]);
            remaining.erase(remaining.begin() + digit);
        }
        if (rank != 0) throw std::invalid_argument("Noncanonical ordered selection rank");
        return ret;
    }
    int i = 0;
    int bitsCopy = bits & 0x3FF;
    while (bitsCopy) {
        if (bitsCopy & 0x1) {
            ret.push_back(i);
        }
        bitsCopy >>= 1;
        ++i;
    }
    return ret;
}

bool isValidPotionAction(const BattleContext &bc, const search::Action &a) {
    // Stock keeps the potion command available while a combat card-selection
    // screen is open (for example Colorless Potion's Discovery screen).
    if (bc.inputState != InputState::PLAYER_NORMAL &&
            bc.inputState != InputState::CARD_SELECT) {
        return false;
    }

    if (a.getSourceIdx() < 0 || a.getSourceIdx() > 5) {
        return false;
    }

    const auto p = bc.potions[a.getSourceIdx()];
    if (p == Potion::INVALID || p == Potion::EMPTY_POTION_SLOT) {
        return false;
    }

    if (a.getTargetIdx() > 5) {
        // discard action
        return true;

    } else {

        if (p == sts::Potion::FAIRY_POTION) {
            return false;
        }

        if (p == sts::Potion::SMOKE_BOMB && !bc.canUseSmokeBomb()) {
            return false;
        }

        if (!potionRequiresTarget(p)) {
            return true;
        }

        if (a.getTargetIdx() < 0) {
            return false;
        }

        return bc.monsters.arr[a.getTargetIdx()].isTargetable();
    }


}

bool isValidCardAction(const BattleContext &bc, const search::Action &a) {
    if (bc.inputState != InputState::PLAYER_NORMAL) {
        return false;
    }

    if (!bc.isCardPlayAllowed()) {
        return false;
    }

    if (a.getSourceIdx() < 0 || a.getSourceIdx() >= bc.cards.cardsInHand) {
        return false;
    }

    const auto &c = bc.cards.hand[a.getSourceIdx()];

    if (a.getTargetIdx() < 0 || a.getTargetIdx() >= 5) {
        return false;
    }

    return c.canUse(bc, a.getTargetIdx(), false);
}

bool isValidSingleCardSelectAction(const BattleContext &bc, const search::Action &a) {
    if (bc.inputState != InputState::CARD_SELECT) {
        return false;
    }

    switch (bc.cardSelectInfo.cardSelectTask) {
        case CardSelectTask::CODEX:
            return a.getSelectIdx() >= 0 && a.getSelectIdx() < 4;

        case CardSelectTask::DISCOVERY:
            return a.getSelectIdx() >= 0 && a.getSelectIdx() < 3;

        case CardSelectTask::HOLOGRAM:
        case CardSelectTask::HEADBUTT:
        case CardSelectTask::MEDITATE:
            return a.getSelectIdx() >= 0 && a.getSelectIdx() < bc.cards.discardPile.size();

        case CardSelectTask::LIQUID_MEMORIES_POTION:
            return bc.cardSelectInfo.pickCount == 1 && a.getSelectIdx() >= 0 &&
                a.getSelectIdx() < bc.cards.discardPile.size();

        case CardSelectTask::EXHUME:
            if (a.getSelectIdx() < 0 || a.getSelectIdx() >= bc.cards.exhaustPile.size()) {
                return false;
            }
            return bc.cards.exhaustPile[a.getSelectIdx()].id != sts::CardId::EXHUME;

        case CardSelectTask::EXHAUST_ONE:
        case CardSelectTask::FORETHOUGHT:
        case CardSelectTask::NIGHTMARE:
        case CardSelectTask::RECYCLE:
        case CardSelectTask::SETUP:
        case CardSelectTask::WARCRY:
            return a.getSelectIdx() >= 0 && a.getSelectIdx() < bc.cards.cardsInHand;

        case CardSelectTask::ARMAMENTS:
            if (a.getSelectIdx() < 0 || a.getSelectIdx() >= bc.cards.cardsInHand) {
                return false;
            }
            return bc.cards.hand[a.getSelectIdx()].canUpgrade();

        case CardSelectTask::DUAL_WIELD: {
            if (a.getSelectIdx() < 0 || a.getSelectIdx() >= bc.cards.cardsInHand) {
                return false;
            }
            const auto &c = bc.cards.hand[a.getSelectIdx()];
            return c.getType() == sts::CardType::POWER || c.getType() == sts::CardType::ATTACK;
        }

        case CardSelectTask::SECRET_TECHNIQUE: {
            if (a.getSelectIdx() < 0 || a.getSelectIdx() >= bc.cards.drawPile.size()) {
                return false;
            }
            return bc.cards.drawPile[a.getSelectIdx()].getType() == CardType::SKILL;
        }

        case CardSelectTask::SECRET_WEAPON: {
            if (a.getSelectIdx() < 0 || a.getSelectIdx() >= bc.cards.drawPile.size()) {
                return false;
            }
            return bc.cards.drawPile[a.getSelectIdx()].getType() == CardType::ATTACK;
        }

        case CardSelectTask::SEEK:
            return a.getSelectIdx() >= 0 && a.getSelectIdx() < bc.cards.drawPile.size();

        case CardSelectTask::EXHAUST_MANY:
        case CardSelectTask::GAMBLE:
        default:
            return false;
    }
}

bool isValidMultiCardSelectAction(const BattleContext &bc, const search::Action &a) { // this can be sped up using compiler intrinsics
    switch (bc.cardSelectInfo.cardSelectTask) {
        case sts::CardSelectTask::EXHAUST_MANY: {
            const auto selected = a.getSelectedIdxs();
            if (selected.size() > bc.cardSelectInfo.pickCount) {
                return false;
            }
            for (auto x : selected) {
                if (x >= bc.cards.cardsInHand) {
                    return false;
                }
            }
            return true;
        }

        case sts::CardSelectTask::RETAIN_CARDS: {
            const auto selected = a.getSelectedIdxs();
            if (selected.size() > bc.cardSelectInfo.pickCount) {
                return false;
            }
            for (auto x : selected) {
                if (x < 0 || x >= bc.cards.cardsInHand) {
                    return false;
                }
            }
            return true;
        }

        case sts::CardSelectTask::LIQUID_MEMORIES_POTION: {
            return bc.cardSelectInfo.pickCount == 2 &&
                a.getSourceIdx() != a.getTargetIdx() &&
                a.getSourceIdx() < bc.cards.discardPile.size() &&
                a.getTargetIdx() < bc.cards.discardPile.size();
        }

        case sts::CardSelectTask::FORETHOUGHT: {
            if (!bc.cardSelectInfo.canPickAnyNumber) {
                return false;
            }
            const auto selected = a.getSelectedIdxs();
            for (auto x : selected) {
                if (x < 0 || x >= bc.cards.cardsInHand) {
                    return false;
                }
            }
            return true;
        }

        case sts::CardSelectTask::GAMBLE: {
            const auto selected = a.getSelectedIdxs();
            for (auto x : selected) {
                if (x >= bc.cards.cardsInHand) {
                    return false;
                }
            }
            return true;
        }

        default:
            return false;
    }
}

bool search::Action::isValidAction(const BattleContext &bc) const {
    if (bc.outcome != Outcome::UNDECIDED) {
        return false;
    }

    switch (getActionType()) {
        case ActionType::CARD:
            return isValidCardAction(bc, *this);

        case ActionType::POTION:
            return isValidPotionAction(bc, *this);

        case ActionType::SINGLE_CARD_SELECT:
            return isValidSingleCardSelectAction(bc, *this);

        case ActionType::MULTI_CARD_SELECT:
            return isValidMultiCardSelectAction(bc, *this);

        case ActionType::END_TURN:
            return bc.inputState == InputState::PLAYER_NORMAL;

        default:
            return false;
    }
}

std::ostream& printSingleCardSelectDescHelper(std::ostream &os, const BattleContext &bc, const search::Action &a) {
    // action is known to be valid here
    os << "{ " << cardSelectTaskStrings[static_cast<int>(bc.cardSelectInfo.cardSelectTask)];
    os << " (" << a.getSelectIdx() << ") ";
    // TODO we don't know if it's selecting from hand or discard
    // const auto &c = bc.cards.hand[a.getSelectIdx()];
    // c.printSimpleDesc(os);
    return os << " }";
}

std::ostream& printMultiCardSelectDescHelper(std::ostream &os, const BattleContext &bc, const search::Action &a) {
    // action is known to be valid here
    os << "{ " << cardSelectTaskStrings[static_cast<int>(bc.cardSelectInfo.cardSelectTask)];

    const auto selected = a.getSelectedIdxs();
    if (selected.empty()) {
        return os << " none";
    }

    for (int i = 0; i < selected.size(); ++i) {
        os << " (" << a.getSelectIdx() << ") ";
        const auto &c = bc.cards.hand[selected[i]];
        c.printSimpleDesc(os);
        if (i+1 < selected.size()) {
            os << ',';
        }
    }

    return os << " }";
}

std::ostream& search::Action::printDesc(std::ostream &os, const BattleContext &bc) const {
    if (!isValidAction(bc)) {
        return os << "{ INVALID ACTION }";
    }

    switch (getActionType()) {
        case ActionType::CARD: {
            const auto &c = bc.cards.hand[getSourceIdx()];
            os << "{ use card (" << getSourceIdx() << ") " << c;
            if (c.requiresTarget()) {
                const auto &m = bc.monsters.arr[getTargetIdx()];
                os << " -> (" << getTargetIdx() << ") " << m.getName();
            }
            return os << " }";
        }

        case ActionType::POTION: {
            const auto &p = bc.potions[getSourceIdx()];
            os << "{ ";
            if (getTargetIdx() == -1) {
                os << "discard potion ";
            } else {
                os << "drink potion ";
            }

            os << "(" << getSourceIdx() << ") " << getPotionName(p);

            if (potionRequiresTarget(p)) {
                const auto &m = bc.monsters.arr[getTargetIdx()];
                os << " -> (" << getTargetIdx() << ") " << m.getName();
            }
            return os << " }";
        }

        case ActionType::SINGLE_CARD_SELECT:
            return printSingleCardSelectDescHelper(os, bc, *this);

        case ActionType::MULTI_CARD_SELECT:
            return printMultiCardSelectDescHelper(os, bc, *this);

        case ActionType::END_TURN:
            return os << "{ end turn }";

        default:
            return os;
    }
}

void executeSingleCardSelectActionHelper(BattleContext &bc, search::Action a) {
    // assumed to be valid action here

    const auto idx = a.getSelectIdx();

    switch (bc.cardSelectInfo.cardSelectTask) {

        case CardSelectTask::ARMAMENTS:
            bc.chooseArmamentsCard(idx);
            break;

        case CardSelectTask::CODEX:
            if (idx != 3) { // idx 3 means skip
                bc.chooseCodexCard(bc.cardSelectInfo.codexCards()[idx]);
            }
            break;

        case CardSelectTask::DISCOVERY:
            bc.chooseDiscoveryCard(bc.cardSelectInfo.discovery_Cards()[idx]);
            break;

        case CardSelectTask::DUAL_WIELD:
            bc.chooseDualWieldCard(idx);
            break;

        case CardSelectTask::EXHAUST_ONE:
            bc.chooseExhaustOneCard(idx);
            break;

        case CardSelectTask::EXHUME:
            bc.chooseExhumeCard(idx);
            break;

        case CardSelectTask::FORETHOUGHT:
            bc.chooseForethoughtCard(idx);
            break;

        case CardSelectTask::HEADBUTT:
            bc.chooseHeadbuttCard(idx);
            break;

        case CardSelectTask::HOLOGRAM:
            bc.chooseDiscardToHandCard(idx, false);
            break;

        case CardSelectTask::LIQUID_MEMORIES_POTION:
            bc.chooseDiscardToHandCard(idx, true);
            break;

        case CardSelectTask::SECRET_TECHNIQUE:
            bc.chooseDrawToHandCards(&idx, 1);
            break;

        case CardSelectTask::SECRET_WEAPON:
            bc.chooseDrawToHandCards(&idx, 1);
            break;

        case CardSelectTask::WARCRY:
            bc.chooseWarcryCard(idx);
            break;

        case CardSelectTask::MEDITATE:
        case CardSelectTask::NIGHTMARE:
        case CardSelectTask::RECYCLE:
        case CardSelectTask::SETUP:
        case CardSelectTask::SEEK:
            // not implemented
        case CardSelectTask::INVALID:
        default:
            break;
    }
}

void executeMultiCardSelectActionHelper(BattleContext &bc, search::Action a) {
    // assumed to be valid action here
    switch (bc.cardSelectInfo.cardSelectTask) {

        case sts::CardSelectTask::EXHAUST_MANY:
            bc.chooseExhaustCards(a.getSelectedIdxs());
            break;

        case sts::CardSelectTask::GAMBLE:
            bc.chooseGambleCards(a.getSelectedIdxs());
            break;

        case sts::CardSelectTask::RETAIN_CARDS:
            bc.chooseRetainCards(a.getSelectedIdxs());
            break;

        case sts::CardSelectTask::FORETHOUGHT:
            bc.chooseForethoughtCards(a.getSelectedIdxs());
            break;

        case sts::CardSelectTask::LIQUID_MEMORIES_POTION: {
            fixed_list<int, 10> selected;
            selected.push_back(a.getSourceIdx());
            selected.push_back(a.getTargetIdx());
            bc.chooseDiscardToHandCards(selected, true);
            break;
        }

        default:
            break;
    }
}

void search::Action::execute(BattleContext &bc) const {
#ifdef sts_asserts
    if (!isValidAction(bc)) {
        std::cerr << bc.seed << " " << static_cast<int>(getActionType()) << " " << getSourceIdx() << " " << getTargetIdx() << std::endl;
        std::cerr <<  cardSelectTaskStrings[static_cast<int>(bc.cardSelectInfo.cardSelectTask)] << std::endl;
        std::cerr << bc << std::endl;
        assert(false);
    }
#endif

    const bool resumeCardSelection =
        getActionType() == ActionType::POTION && bc.inputState == InputState::CARD_SELECT;
    const auto suspendedSelection = bc.cardSelectInfo;

    switch (getActionType()) {
        case ActionType::CARD: {
            const CardQueueItem item(bc.cards.hand[getSourceIdx()], getTargetIdx(), bc.player.energy);
            bc.addToBotCard(item);
            break;
        }

        case ActionType::POTION: {
            if (getTargetIdx() > 5) {
                bc.discardPotion(getSourceIdx());
            } else {
                bc.drinkPotion(getSourceIdx(), getTargetIdx());
            }
            break;
        }

        case ActionType::SINGLE_CARD_SELECT:
            executeSingleCardSelectActionHelper(bc, *this);
            break;

        case ActionType::MULTI_CARD_SELECT:
            executeMultiCardSelectActionHelper(bc, *this);
            break;

        case ActionType::END_TURN:
            bc.endTurn();
            break;

        default:
            break;
    }

    if (resumeCardSelection) {
        // Stock consumes the potion and queues its effect behind the current
        // card-selection screen. Even a potion whose effect opens another
        // selection (such as Elixir) must not replace the suspended Discovery
        // screen; its queued action runs after that choice resolves.
        bc.cardSelectInfo = suspendedSelection;
        bc.inputState = InputState::CARD_SELECT;
        return;
    }
    bc.inputState = InputState::EXECUTING_ACTIONS;
    bc.executeActions();
}

template <typename ForwardIt>
void setupCardOptionsHelper(std::vector<search::Action> &actions, const ForwardIt begin, const ForwardIt end, const std::function<bool(const CardInstance &)> &p= nullptr) {
    for (int i = 0; begin+i != end; ++i) {
        const auto &c = begin[i];
        if (!p || (p(c))) {
            actions.emplace_back(search::ActionType::SINGLE_CARD_SELECT, i);
        }
    }
}

std::vector<search::Action> search::Action::enumerateCardSelectActions(const BattleContext &bc) {
    std::vector<search::Action> actions;

    switch (bc.cardSelectInfo.cardSelectTask) {
        case CardSelectTask::ARMAMENTS:
            setupCardOptionsHelper( actions, bc.cards.hand.begin(), bc.cards.hand.begin() + bc.cards.cardsInHand,
                                    [] (const CardInstance &c) { return c.canUpgrade(); });
            break;

        case CardSelectTask::CODEX:
            for (int i = 0; i < 4; ++i) { // i -> 3 action means skip
                actions.push_back({Action(search::ActionType::SINGLE_CARD_SELECT, i)});
            }
            break;

        case CardSelectTask::DISCOVERY:
            for (int i = 0; i < 3; ++i) {
                actions.push_back({Action(search::ActionType::SINGLE_CARD_SELECT, i)});
            }
            break;

        case CardSelectTask::DUAL_WIELD:
            setupCardOptionsHelper( actions, bc.cards.hand.begin(), bc.cards.hand.begin() + bc.cards.cardsInHand,
                                    [] (const CardInstance &c) {
                                        return c.getType() == CardType::POWER || c.getType() == CardType::ATTACK;
                                    });
            break;

        case CardSelectTask::EXHUME:
            setupCardOptionsHelper(actions, bc.cards.exhaustPile.begin(), bc.cards.exhaustPile.end(),
                                   [](const auto &c) { return c.getId() != CardId::EXHUME; });
            break;

        case CardSelectTask::EXHAUST_ONE:
            setupCardOptionsHelper(actions, bc.cards.hand.begin(), bc.cards.hand.begin() + bc.cards.cardsInHand);
            break;

        case CardSelectTask::FORETHOUGHT:
            if (bc.cardSelectInfo.canPickAnyNumber) {
                actions.push_back({search::Action(search::ActionType::MULTI_CARD_SELECT, 0)});
            } else {
                setupCardOptionsHelper(actions, bc.cards.hand.begin(), bc.cards.hand.begin() + bc.cards.cardsInHand);
            }
            break;

        case CardSelectTask::WARCRY:
            setupCardOptionsHelper(actions, bc.cards.hand.begin(), bc.cards.hand.begin() + bc.cards.cardsInHand);
            break;

        case CardSelectTask::HEADBUTT:
            setupCardOptionsHelper(actions, bc.cards.discardPile.begin(), bc.cards.discardPile.end());
            break;

        case CardSelectTask::LIQUID_MEMORIES_POTION:
            if (bc.cardSelectInfo.pickCount > 1) {
                // FullRun expands this exact multi-selection into incremental
                // policy choices. Keep a valid representative for the native API.
                actions.push_back(search::Action(search::ActionType::MULTI_CARD_SELECT, 0, 1));
            } else {
                setupCardOptionsHelper(actions, bc.cards.discardPile.begin(), bc.cards.discardPile.end());
            }
            break;

        case CardSelectTask::SECRET_TECHNIQUE:
            setupCardOptionsHelper(actions, bc.cards.drawPile.begin(), bc.cards.drawPile.end(),
                                   [] (const CardInstance &c) {
                                       return c.getType() == CardType::SKILL;
                                   });
            break;

        case CardSelectTask::SECRET_WEAPON:
            setupCardOptionsHelper(actions, bc.cards.drawPile.begin(), bc.cards.drawPile.end(),
                                   [] (const CardInstance &c) {
                                       return c.getType() == CardType::ATTACK;
                                   });
            break;

        case CardSelectTask::EXHAUST_MANY:
        case CardSelectTask::GAMBLE:
        case CardSelectTask::RETAIN_CARDS:
            // just dont deal with this right now
            actions.push_back({search::Action(search::ActionType::MULTI_CARD_SELECT, 0)});
            break;

        default:
#ifdef sts_asserts
            assert(false);
#endif
            break;
    }
    return actions;
}








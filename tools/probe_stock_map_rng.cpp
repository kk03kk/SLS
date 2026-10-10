// Observe production map phases without changing Map.cpp or its RNG.
#include <cstdint>
#include <iostream>
#include "game/Map.h"
#include "game/Random.h"

void initNodes(sts::Map &);
void createPaths(sts::Map &, sts::Random &);
void filterRedundantEdgesFromFirstRow(sts::Map &);
void assignRooms(sts::Map &, sts::Random &, int);
void assignBurningElite(sts::Map &, sts::Random &);

void rngState(const sts::Random &rng) {
    std::cout << "{\"counter\":" << rng.counter << ",\"seed0\":" << rng.seed0
              << ",\"seed1\":" << rng.seed1 << "}";
}

int main() {
    constexpr std::uint64_t seed = 131200410;
    std::cout << "[";
    bool first = true;
    for (int act : {2, 3}) for (bool burning : {false, true}) {
        sts::Random rng(seed + act * (100 * (act - 1)));
        sts::Map map;
        initNodes(map);
        createPaths(map, rng);
        filterRedundantEdgesFromFirstRow(map);
        assignRooms(map, rng, 20);
        if (burning) assignBurningElite(map, rng);
        // Stock samples the buff only on entering the burning combat.
        const auto constructorRng = rng;
        if (burning) map.burningEliteBuff = rng.random(0, 3);
        const auto actual = sts::Map::fromSeed(seed, 20, act, burning);
        const bool equal = actual.toString(true) == map.toString(true)
            && actual.burningEliteX == map.burningEliteX && actual.burningEliteY == map.burningEliteY
            && actual.burningEliteBuff == map.burningEliteBuff;
        if (!first) std::cout << ",";
        first = false;
        std::cout << "{\"seed\":" << seed << ",\"act\":" << act
                  << ",\"assign_burning\":" << (burning ? "true" : "false")
                  << ",\"production_constructor_equal\":" << (equal ? "true" : "false")
                  << ",\"burning_x\":" << map.burningEliteX << ",\"burning_y\":" << map.burningEliteY
                  << ",\"stock_constructor_phase_rng\":";
        rngState(constructorRng);
        std::cout << ",\"native_after_buff_rng\":";
        rngState(rng);
        std::cout << ",\"nodes\":[";
        bool firstNode = true;
        for (int y = 0; y < 15; ++y) for (int x = 0; x < 7; ++x) {
            const auto &node = map.getNode(x, y);
            if (!node.edgeCount && !node.parentCount) continue;
            if (!firstNode) std::cout << ",";
            firstNode = false;
            std::cout << "{\"x\":" << x << ",\"y\":" << y << ",\"symbol\":\""
                      << node.getRoomSymbol() << "\",\"children\":[";
            for (int i = 0; i < node.edgeCount; ++i) {
                if (i) std::cout << ",";
                std::cout << "{\"x\":" << node.edges[i] << ",\"y\":" << (y == 14 ? 16 : y + 1) << "}";
            }
            std::cout << "]}";
        }
        std::cout << "]}";
        if (!equal) return 2;
    }
    std::cout << "]\n";
}

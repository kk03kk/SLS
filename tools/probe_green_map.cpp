#include <cstdint>
#include <iostream>
#include "game/Map.h"

int main() {
    std::cout << "[";
    for (std::uint64_t seed = 131200362; seed < 131200364; ++seed) {
        const auto map = sts::Map::fromSeed(seed, 20, 2, true);
        if (seed != 131200362) std::cout << ",";
        std::cout << "{\"seed\":" << seed << ",\"x\":" << map.burningEliteX
                  << ",\"y\":" << map.burningEliteY << ",\"buff\":" << map.burningEliteBuff << "}";
    }
    std::cout << "]\n";
}

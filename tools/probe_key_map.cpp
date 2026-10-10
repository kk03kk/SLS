// Standalone production Map constructor; no simulator source modifications.
#include <cstdint>
#include <iomanip>
#include <iostream>
#include "game/Map.h"

int main() {
    const std::uint64_t seed = 131100064;
    std::cout << "[";
    bool first = true;
    for (int act : {2, 3}) for (bool burning : {false, true}) {
        auto map = sts::Map::fromSeed(seed, 20, act, burning);
        if (!first) std::cout << ",";
        first = false;
        std::cout << "{\"seed\":" << seed << ",\"act\":" << act
                  << ",\"assign_burning\":" << (burning ? "true" : "false")
                  << ",\"x\":" << map.burningEliteX << ",\"y\":" << map.burningEliteY
                  << ",\"buff\":" << map.burningEliteBuff << ",\"map_hex\":\"";
        for (unsigned char c : map.toString(true))
            std::cout << std::hex << std::setw(2) << std::setfill('0') << static_cast<int>(c);
        std::cout << std::dec << "\"}";
    }
    std::cout << "]\n";
}

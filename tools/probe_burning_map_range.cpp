#include <cstdint>
#include <iostream>
#include <stdexcept>
#include <string>
#include "game/Map.h"

int main(int argc, char **argv) {
    try {
        if (argc != 3) throw std::invalid_argument("START_SEED COUNT required");
        const std::uint64_t first = std::stoull(argv[1]);
        const std::uint64_t count = std::stoull(argv[2]);
        if (count == 0 || count > 64 || first + count < first)
            throw std::invalid_argument("count must be1..64 without overflow");
        std::cout << "[";
        for (std::uint64_t seed = first; seed < first + count; ++seed) {
            const auto map = sts::Map::fromSeed(seed, 20, 2, true);
            bool reachable[15][7]{};
            for (int x = 0; x < 7; ++x) reachable[0][x] = map.getNode(x,0).edgeCount > 0;
            for (int y = 0; y < 14; ++y) for (int x = 0; x < 7; ++x) {
                if (!reachable[y][x]) continue;
                const auto &node = map.getNode(x,y);
                for (int i = 0; i < node.edgeCount; ++i) reachable[y+1][node.edges[i]] = true;
            }
            if (seed != first) std::cout << ",";
            std::cout << "{\"seed\":" << seed << ",\"x\":" << map.burningEliteX
                      << ",\"y\":" << map.burningEliteY << ",\"buff\":" << map.burningEliteBuff
                      << ",\"root_reachable\":" << (reachable[map.burningEliteY][map.burningEliteX] ? "true" : "false") << "}";
        }
        std::cout << "]\n";
        return 0;
    } catch (const std::exception &error) {
        std::cerr << error.what() << "\n";
        return 2;
    }
}

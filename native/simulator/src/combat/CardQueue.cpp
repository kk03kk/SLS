//
// Created by gamerpuppy on 8/24/2021.
//

#include "combat/CardQueue.h"

using namespace sts;


bool CardQueue::isEmpty() const {
    return size == 0;
}

bool CardQueue::containsCardWithId(int uniqueId) const {
    int idx = frontIdx;
    for (int i = 0; i < size; ++i) {
        if (arr[idx].card.getUniqueId() == uniqueId) {
            return true;
        }
        ++idx;
        if (idx >= capacity) {
            idx = 0;
        }
    }
    return false;
}

void CardQueue::clear() {
    size = 0;
    backIdx = 0;
    frontIdx = 0;
}

void CardQueue::pushFront(CardQueueItem item) {
    if (size >= capacity) throw std::overflow_error("card queue overflow");
    --frontIdx;
    ++size;
    if (frontIdx < 0) {
        frontIdx = capacity - 1;
    }
    arr.at(frontIdx) = std::move(item);
}

void CardQueue::pushBack(CardQueueItem item) {
    if (size >= capacity) throw std::overflow_error("card queue overflow");
    arr.at(backIdx) = std::move(item);
    ++backIdx;
    ++size;
    if (backIdx >= capacity) {
        backIdx = 0;
    }
}

CardQueueItem CardQueue::popFront() {
    if (size <= 0) throw std::underflow_error("card queue underflow");
    CardQueueItem &item = arr.at(frontIdx);
    ++frontIdx;
    --size;
    if (frontIdx >= capacity) {
        frontIdx = 0;
    }
    return item;
}

CardQueueItem CardQueue::popBack() {
    if (size <= 0) throw std::underflow_error("card queue underflow");
    --backIdx;
    --size;
    if (backIdx < 0) {
        backIdx = arr.size() - 1;
    }
    CardQueueItem &item = arr.at(backIdx);
    return item;
}

CardQueueItem &CardQueue::front() {
    if (size <= 0) throw std::underflow_error("card queue underflow");
    return arr.at(frontIdx);
}

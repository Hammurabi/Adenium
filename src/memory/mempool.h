#pragma once
#include "bytes.h"
#include "blake.h"
#include <deque>
#include <set>
#include <memory>
#include <cstddef>
#include <new>
#include <mutex>

class mempool {
public:
    mempool() = default;

    bool insert(const bytes& data) {
        std::lock_guard<std::mutex> lock(m_Mutex);
        if (m_MemSet.find(data) != m_MemSet.end()) {
            return false;
        }
        m_MemPool.push_back(data);
        m_MemSet.insert(data);
        return true;
    }

    std::optional<bytes> fetch() {
        std::lock_guard<std::mutex> lock(m_Mutex);
        if (m_MemPool.empty()) {
            return std::nullopt;
        }
        bytes data = m_MemPool.front();
        m_MemPool.pop_front();
        m_MemSet.erase(data);
        return data;
    }
private:
    std::deque<bytes>                       m_MemPool;
    std::set<bytes>                         m_MemSet;
    std::mutex                              m_Mutex;
};
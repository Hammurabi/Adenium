#pragma once
#include <unordered_map>
#include <list>
#include <optional>

struct NoEvict {
    template<typename K, typename V>
    void operator()(const K&, const V&) const noexcept {}
};

namespace std {
    // Provide a hash specialization for the project's 'bytes' type so unordered_map can be used.
    // This uses FNV-1a 64-bit. If your 'bytes' type is large and fixed-size, consider
    // switching to a faster hash / truncated native hash.
    template<>
    struct hash<bytes> {
        size_t operator()(const bytes &b) const noexcept {
            uint64_t h = 14695981039346656037ull;
            for (auto byte : b) {
                h ^= static_cast<uint8_t>(byte);
                h *= 1099511628211ull;
            }
            return static_cast<size_t>(h);
        }
    };
} // namespace std

template<typename Key, typename Value, typename EvictFn = NoEvict>
class lru_cache {
public:
    explicit lru_cache(size_t capacity)
        : m_Capacity(capacity), m_EvictFn() {}

    bool contains(const Key& key) const {
        return m_Map.find(key) != m_Map.end();
    }

    std::optional<Value> get(const Key& key) {
        auto it = m_Map.find(key);
        if (it == m_Map.end()) return std::nullopt;

        m_Items.splice(m_Items.begin(), m_Items, it->second);
        return it->second->second;
    }

    void put(const Key& key, const Value& value) {
        auto it = m_Map.find(key);
        if (it != m_Map.end()) {
            it->second->second = value;
            m_Items.splice(m_Items.begin(), m_Items, it->second);
            return;
        }

        m_Items.emplace_front(key, value);
        m_Map[key] = m_Items.begin();

        if (m_Map.size() > m_Capacity) {
            evict_lru();
        }
    }

    void erase(const Key& key, bool call_evict = true) {
        auto it = m_Map.find(key);
        if (it != m_Map.end()) {
            if (call_evict) m_EvictFn(it->second->first, it->second->second);
            m_Items.erase(it->second);
            m_Map.erase(it);
        }
    }

    size_t size() const { return m_Map.size(); }
    size_t capacity() const { return m_Capacity; }
    void clear() {
        for (const auto& item : m_Items) {
            m_EvictFn(item.first, item.second);
        }
        m_Items.clear();
        m_Map.clear();
    }

private:
    void evict_lru() {
        auto &last = m_Items.back();
        m_EvictFn(last.first, last.second);
        m_Map.erase(last.first);
        m_Items.pop_back();
    }

private:
    size_t m_Capacity;
    EvictFn m_EvictFn;
    std::list<std::pair<Key, Value>> m_Items;
    std::unordered_map<Key, typename std::list<std::pair<Key, Value>>::iterator, std::hash<Key>> m_Map;
};

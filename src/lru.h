#pragma once
#include <iostream>
#include <unordered_map>
#include <list>
#include <string>
#include <optional>


template<typename KeyType, typename ValueType>
class LRU_Cache {
private:
    size_t _Capacity;
    std::list<std::pair<KeyType, ValueType>> _Items;
    std::unordered_map<KeyType, typename std::list<std::pair<KeyType, ValueType>>::iterator> _Cache;

public:
    LRU_Cache(size_t Capacity) : _Capacity(Capacity) {}

    std::optional<ValueType> Get(const KeyType& key) {
        auto It = _Cache.find(key);
        if (It == _Cache.end()) {
            return std::nullopt;
        }
        
        _Items.splice(_Items.begin(), _Items, it->second);
        return it->second->second;
    }

    ValueType Get(const KeyType& key, const ValueType& default_value) {
        auto Result = get(key);
        return Result.has_value() ? Result.value() : default_value;
    }

    void Put(const KeyType& key, const ValueType& value) {
        auto It = _Cache.find(key);
        
        if (It != _Cache.end()) {
            It->second->second = value;
            _Items.splice(_Items.begin(), _Items, It->second);
            return;
        }
        
        if (_Cache.size() >= _Capacity) {
            KeyType lru_key = _Items.back().first;
            _Items.pop_back();
            _Cache.erase(lru_key);
        }
        
        _Items.push_front({key, value});
        _Cache[key] = _Items.begin();
    }
    
    size_t Size() const {
        return _Cache.size();
    }
    
    bool Contains(const KeyType& key) const {
        return _Cache.find(key) != _Cache.end();
    }
    
    void Display() const {
        std::cout << "LRU _Cache (most recent first):" << std::endl;
        for (const auto& Item : _Items) {
            std::cout << Item.first << " -> " << Item.second << std::endl;
        }
        std::cout << "----------------" << std::endl;
    }

    void Clear() {
        _Items.clear();
        _Cache.clear();
    }
};
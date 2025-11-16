#pragma once
#include <string>
#include <memory>
#include "../reflect.h"
#include "../util.h"
#include "../bytes.h"

class Storage;
struct TrieNode;
typedef std::shared_ptr<TrieNode> RTrieNode;

struct TriePrefix
{
    bytes       nibbles;
    bool        isLeaf;
};

struct TrieNode
{
    TriePrefix                                          prefix;
    std::optional<const_bytes<32>>                      value;
    bitmask_array<const_bytes<32>, 16>                  children;

    std::array<RTrieNode, 16>                           inMemoryChildren;
    bool                                                dirty;              // tracks if node needs recomputing
    const_bytes<32>                                     cachedHash;         // cache hash to avoid recomputation
    Storage*                                            storage;

    TrieNode(Storage *db);
    void AddChild(uint8_t index, const const_bytes<32>& hash);
    void AddChild(uint8_t index, const RTrieNode& node);
    void RemoveChild(uint8_t index);
    void RemoveChild(const bytes& hash);
    void SetValue(const const_bytes<32> &newValue);
    RTrieNode GetChild(uint8_t index, bool cache = true);
    bool IsDirty() const;
    bool ShouldDelete();

    bool Update();
    bytes Hash();

    bytes Encode();
    void Store();
    void Delete();
    bool HasChild(uint8_t index) const {
        return children[index].has_value() || inMemoryChildren[index] != nullptr;
    }
    size_t NumChildren() const {
        size_t count = 0;
        for (uint8_t i = 0; i < 16; i++) {
            if (HasChild(i)) {
                count++;
            }
        }
        return count;
    }
    byte FirstChildIndex() const {
        for (uint8_t i = 0; i < 16; i++) {
            if (HasChild(i)) {
                return i;
            }
        }
        return 16; // invalid
    }
    bytes GetChildHash(uint8_t index) const {
        if (inMemoryChildren[index]) {
            return inMemoryChildren[index]->Hash();
        } else if (children[index].has_value()) {
            return children[index].value();
        }

        return bytes();
    }
};

RTrieNode Decode(const bytes& hash, Storage* db);

class Storage;
class RTrie
{
public:
    RTrie(Storage* Db = nullptr, const const_bytes<32>& rootHash = const_bytes<32>());
    ~RTrie();

    void Insert(const bytes& key, const const_bytes<32>& value);
    std::optional<const_bytes<32>> Search(const bytes& key);
    bool Delete(const bytes &key);

    const_bytes<32> GetRootHash() const { return m_RootHash; }
    RTrieNode GetRootNode();
private:
    static size_t FindCommonPrefixLength(const bytes& a, const bytes& b);
    bool InsertRecursive(const RTrieNode& node, const bytes& key, const const_bytes<32>& value);
    std::optional<const_bytes<32>> SearchRecursive(const RTrieNode& node, const bytes& key);
    bool DeleteRecursive(const RTrieNode& node, const bytes& key);

    Storage*    m_Db;
    const_bytes<32> m_RootHash;
};
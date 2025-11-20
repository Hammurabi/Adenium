#include "radix.h"
#include "db.h"

lru_cache<bytes, RTrieNode> TrieNodeCache(1000000);

TrieNode::TrieNode(Storage *db)
        : storage(db), prefix({}), value(std::nullopt), children({}), inMemoryChildren(), dirty(true), cachedHash() {}

void TrieNode::AddChild(uint8_t index, const const_bytes<32> &hash)
{
    if (children[index].has_value() && children[index].value() == hash) {
        return;
    }

    children[index] = hash;
    inMemoryChildren[index] = nullptr;
    dirty = true;
}

void TrieNode::AddChild(uint8_t index, const RTrieNode &node)
{
    inMemoryChildren[index] = node;
    dirty = true;
}

void TrieNode::RemoveChild(uint8_t index)
{
    if (!inMemoryChildren[index] && !children[index].has_value()) {
        return;
    }

    children[index] = std::nullopt;
    inMemoryChildren[index] = nullptr;
    dirty = true;
}

void TrieNode::RemoveChild(const bytes &hash)
{
    for (uint8_t i = 0; i < 16; i++) {
        if (children[i].has_value() && children[i].value() == hash) {
            children[i] = std::nullopt;
            dirty = true;
        }
        else if (inMemoryChildren[i] && inMemoryChildren[i]->cachedHash == hash) {
            inMemoryChildren[i] = nullptr;
            dirty = true;
            children[i] = std::nullopt;
        }
    }
}

void TrieNode::SetValue(const const_bytes<32> &newValue)
{
    if (value.has_value() && value.value() == newValue) {
            return;
    }
    
    value = newValue;
    dirty = true;
    prefix.isLeaf = true;
}

RTrieNode TrieNode::GetChild(uint8_t index, bool cache)
{
    if (inMemoryChildren[index]) {
        return inMemoryChildren[index];
    }
    
    if (children[index].has_value()) {
        RTrieNode childNode = Decode(children[index].value(), storage);
        if (cache) inMemoryChildren[index] = childNode;
        return childNode;
    }
    
    return nullptr;
}

bool TrieNode::IsDirty() const
{
    if (dirty) {
        return true;
    }

    if (value.has_value() && !prefix.isLeaf) {
        return true;
    }

    // Check only in-memory children (stored children are clean by definition)
    for (uint8_t i = 0; i < 16; i++) {
        if (inMemoryChildren[i] && inMemoryChildren[i]->IsDirty()) {
            return true;
        }
    }

    return false;
    // if (dirty) {
    //     return true;
    // }

    // if (value.has_value() && !prefix.isLeaf) {
    //     return true;
    // }

    // for (uint8_t i = 0; i < 16; i++) {
    //     if (inMemoryChildren[i] && inMemoryChildren[i]->IsDirty()) {
    //         return true;
    //     }
    // }

    // return false;
}

bool TrieNode::ShouldDelete()
{
    for (uint8_t i = 0; i < 16; i++) {
        if (HasChild(i)) {
            if (!GetChild(i)->ShouldDelete()) {
                return false;
            }
        }
    }

    return !prefix.isLeaf && !value.has_value();
}

bool TrieNode::Update()
{
    bool updated = false;

    for (uint8_t i = 0; i < 16; i++) {
        if (inMemoryChildren[i] && inMemoryChildren[i]->IsDirty()) {
            inMemoryChildren[i]->Store(inMemoryChildren[i]);
            
            bytes hash = inMemoryChildren[i]->cachedHash; // Use cached hash instead of calling Hash()
            if (hash != children[i]) {
                children[i] = hash;
                dirty = true;
                updated = true;
            }
        }
        inMemoryChildren[i] = nullptr;
    }

    if (value.has_value() && !prefix.isLeaf) {
        prefix.isLeaf = true;
        dirty = true;
        return true;
    }

    return updated || dirty;
    // bool updated = false;

    // for (uint8_t i = 0; i < 16; i++) {
    //     if (inMemoryChildren[i] && inMemoryChildren[i]->IsDirty()) {
    //         inMemoryChildren[i]->Store(inMemoryChildren[i]);
    //         // if (inMemoryChildren[i]->Update()) {
    //         //     // TrieNodeCache.put(inMemoryChildren[i]->Hash(), inMemoryChildren[i]);
    //         // }

    //         bytes hash = inMemoryChildren[i]->Hash();
    //         if (hash != children[i]) {
    //             dirty = true;
    //             updated = true;
    //         }
    //         children[i] = hash;
    //     }
    //     inMemoryChildren[i] = nullptr;
    // }

    // if (value.has_value() && !prefix.isLeaf) {
    //     prefix.isLeaf = true;
    //     dirty = true;
    //     return true;
    // }

    // return updated || dirty;
}

bytes TrieNode::Hash()
{
    // Check if we have a valid cached hash
    if (!dirty && !cachedHash->is_zero()) {
        return cachedHash;
    }

    // Only update if dirty
    if (dirty) {
        Update();
    }

    bytes encoded = Encode();
    bytes hash = Keccak(encoded);
    cachedHash = const_bytes<32>(hash);
    dirty = false;
    
    return hash;
    // if (!IsDirty() && !cachedHash->is_zero()) return cachedHash;

    // bytes encoded = Encode();
    // bytes hash = Keccak(encoded);
    // cachedHash = const_bytes<32>(hash);
    // dirty = false;
    
    // return hash;
}

bytes TrieNode::Encode()
{
    if (dirty) {
        Update();
    }

    bool isEmpty = !prefix.isLeaf;
    bool isZero  = (!isEmpty && value.value()->is_zero());

    size_t nibblesCount = prefix.nibbles.size();
    
    if (nibblesCount % 2 == 0) {
        nibblesCount += 2;
    } else {
        nibblesCount += 1;
    }

    bytes encoded;
    encoded.reserve(4 + 32 * NumChildren() + nibblesCount / 2 + (value.has_value() && !isZero ? 32 : 0));

    bitmask_t<16> childMask;
    for (uint8_t i = 0; i < 16; i++) {
        if (children[i].has_value()) {
            childMask.set(i);
        }
    }

    encoded.encode_nibbles(prefix.nibbles, true, isEmpty, isZero);
    if (value.has_value() && !isZero) encoded += value.value();
    encoded += childMask.to_bytes();
    for (uint8_t i = 0; i < 16; i++) {
       if (children[i].has_value()) {
           encoded += children[i].value();
       }
    }

    cachedEncode = encoded;
    return encoded;
}

RTrieNode Decode(const bytes &hash, Storage *db)
{
    auto cachedNode = TrieNodeCache.get(hash);
    if (cachedNode.has_value()) {
        return cachedNode.value();
    }

    RTrieNode node = std::make_shared<TrieNode>(db);
    node->cachedHash = hash;
    node->dirty = false;

    bytes data;
    data.reserve(768);
    if (!db->Get(hash, &data)) {
        throw std::runtime_error("RTrie node not found in storage for hash: " + hash.hex());
    }

    data.seek(0);

    size_t valueInfo = 0;
    node->prefix.nibbles = data.decode_nibbles(&valueInfo);

    switch (valueInfo) {
        case 0:
            node->prefix.isLeaf = false;
            break;
        case 1:
            node->prefix.isLeaf = true;
            node->value = data.decode_bytes(32);
            break;
        case 2:
            node->prefix.isLeaf = true;
            node->value = bytes(32, 0);
            break;
        default:
            throw std::runtime_error("Invalid value info in RTrie node decoding");
    }

    bytes bitmask = data.decode_bytes(2);
    bitmask_t<16> childMask(bitmask);
    for (uint8_t i = 0; i < 16; i++) {
        if (childMask.get(i)) {
            node->children[i] = data.decode_bytes(32);
        }
    }

    TrieNodeCache.put(hash, node);

    return node;
}

void TrieNode::Store(const RTrieNode& self)
{
    if (!IsDirty()) return;
    
    bytes oldHash = this->cachedHash;

    // Store dirty children first
    for (uint8_t i = 0; i < 16; i++) {
        if (inMemoryChildren[i]) {
            inMemoryChildren[i]->Store(inMemoryChildren[i]);
            children[i] = inMemoryChildren[i]->cachedHash;
            inMemoryChildren[i] = nullptr;
        }
    }

    bytes encoded = Encode();
    bytes hash = Keccak(encoded);
    cachedHash = const_bytes<32>(hash);
    
    // Only put if hash changed or this is a new node
    if (oldHash.is_zero() || oldHash != hash) {
        storage->Put(hash, encoded);
        TrieNodeCache.put(hash, self);
        
        // Delete old hash only if it changed
        if (!oldHash.is_zero() && oldHash != hash) {
            storage->Delete(oldHash);
        }
    }
    // bytes cachedHash = this->cachedHash;
    // bytes encoded = Encode();
    // bytes hash = Keccak(encoded);
    // if (storage->Get(hash).empty()) {
    //     storage->Put(hash, encoded);
    //     TrieNodeCache.put(hash, self);
    //     if (!cachedHash.is_zero() && cachedHash != hash) {
    //         storage->Delete(cachedHash);
    //     }
    // }
    // for (uint8_t i = 0; i < 16; i++) {
    //     if (inMemoryChildren[i]) {
    //         inMemoryChildren[i]->Store(inMemoryChildren[i]);
    //     }
    // }
}

void TrieNode::Delete()
{
    cachedHash = Hash();
    storage->Delete(cachedHash);
}

RTrie::RTrie(Storage *Db, const const_bytes<32> &rootHash)
    : m_Db(Db), m_RootHash(rootHash) 
{
}

RTrie::~RTrie()
{
}

void RTrie::Insert(const bytes &key, const const_bytes<32> &value)
{
    if (key.empty()) {
        throw std::invalid_argument("Key cannot be empty");
    }

    bytes keyNibbles = NibblesFromBytes(key, false);
    RTrieNode root = GetRootNode();

    if (InsertRecursive(root, keyNibbles, value)) {
        root->Store(root);
        m_Db->Delete(m_RootHash);
    }
    m_RootHash = root->Hash();
    TrieNodeCache.put(m_RootHash, root);
}

std::optional<const_bytes<32>> RTrie::Search(const bytes &key)
{
    if (key.empty())
        return std::nullopt;
    
    return SearchRecursive(GetRootNode(), NibblesFromBytes(key, false));
}

const_bytes<32> RTrie::Fetch(const bytes &key)
{
    auto result = Search(key);
    if (result.has_value()) {
        return result.value();
    }
    return const_bytes<32>();
}

bool RTrie::Delete(const bytes &key)
{
    if (key.empty())
        return false;
    
    RTrieNode root = GetRootNode();
    bool deleted = DeleteRecursive(root, NibblesFromBytes(key, false));
    if (deleted) {
        root->Store(root);
        m_Db->Delete(m_RootHash);
    }
    m_RootHash = root->Hash();
    TrieNodeCache.put(m_RootHash, root);
    return deleted;
}

RTrieNode RTrie::GetRootNode()
{
    if (m_RootHash->is_zero()) {
        return std::make_shared<TrieNode>(m_Db);
    }

    return Decode(m_RootHash, m_Db);
}

size_t RTrie::FindCommonPrefixLength(const bytes &a, const bytes &b)
{
    size_t minLen = std::min(a.size(), b.size());
    size_t i = 0;
    while (i < minLen && a[i] == b[i]) {
        i++;
    }
    return i;
}

bool RTrie::InsertRecursive(const RTrieNode& node, const bytes &key, const const_bytes<32> &value)
{
    // Case 1: Key is empty - we've reached the insertion point
    if (key.empty()) {
        node->SetValue(value); // automatically set to dirty and a leaf
        return node->IsDirty();
    }

    byte firstChar = key[0];

    if (!node->HasChild(firstChar)) {
        // No matching child, create new leaf
        RTrieNode newChild = std::make_shared<TrieNode>(m_Db);
        newChild->SetValue(value); // automatically set to dirty and a leaf
        newChild->prefix.nibbles = key;

        node->AddChild(firstChar, newChild); // automatically set to dirty
        return true;
    }

    RTrieNode child = node->GetChild(firstChar);
    size_t commonLen = FindCommonPrefixLength(child->prefix.nibbles, key);

    if (commonLen == child->prefix.nibbles.size()) {
        // Case 2a: Child prefix is fully matched, continue down
        return InsertRecursive(child, key.sub(commonLen), value);
    }
    else if (commonLen == key.size())
    {
        // Case 2b: Key is a prefix of child's prefix, need to split
        // Key is a prefix of child, need to split
        RTrieNode newNode = std::make_shared<TrieNode>(m_Db);
        newNode->prefix.nibbles = key;
        newNode->SetValue(value); // automatically set to dirty and a leaf

        // Adjust existing child's prefix to remove the common part
        bytes remaining = child->prefix.nibbles.sub(commonLen);
        assert(!remaining.empty());  // Should never be empty in Case 2b
        child->prefix.nibbles = remaining;

        newNode->AddChild(remaining[0], child);

        // Replace child with new node
        node->AddChild(firstChar, newNode);
        return true;
    }
    else
    {
        // Case 3: Partial match, need to split the child
        RTrieNode branch = std::make_shared<TrieNode>(m_Db);
        branch->prefix.nibbles = child->prefix.nibbles.sub(0, commonLen);
        branch->dirty = true;

        // Adjust existing child
        child->prefix.nibbles = child->prefix.nibbles.sub(commonLen);
        child->dirty = true; // FIX: Mark child as dirty

        if (!child->prefix.nibbles.empty()) {
            branch->AddChild(child->prefix.nibbles[0], child);
        } else {
            // Edge case: if remaining is empty, merge child's properties
            for (uint8_t i = 0; i < 16; i++) {
                if (child->HasChild(i)) {
                    branch->AddChild(i, child->GetChild(i));
                }
            }

            if (child->value.has_value()) branch->SetValue(child->value.value());
        }

        bytes remainingKey = key.sub(commonLen);
        if (!remainingKey.empty()) {
            RTrieNode newLeaf = std::make_shared<TrieNode>(m_Db);
            newLeaf->SetValue(value); // automatically set to dirty and a leaf
            newLeaf->prefix.nibbles = remainingKey;

            branch->AddChild(remainingKey[0], newLeaf);
        } else {
            // Edge case: if remainingKey is empty, branch should hold the value
            branch->SetValue(value);
        }

        // Replace original child with branch
        node->AddChild(firstChar, branch);
        return true;
    }
}

std::optional<const_bytes<32>> RTrie::SearchRecursive(const RTrieNode& node, const bytes &key)
{
    if (key.empty()) {
        return node->prefix.isLeaf ? node->value : std::nullopt;
    }

    byte firstChar = key[0];
    if (!node->HasChild(firstChar)) {
        return std::nullopt;
    }

    RTrieNode child = node->GetChild(firstChar);

    // Check if key matches child prefix
    if (!key.startswith(child->prefix.nibbles)) {
        return std::nullopt;
    }

    // Continue searching in the child
    return SearchRecursive(child, key.sub(child->prefix.nibbles.size()));
}

bool RTrie::DeleteRecursive(const RTrieNode& node, const bytes &key)
{
    if (key.empty()) {
        if (node->prefix.isLeaf) {
            node->Delete();
            node->value = std::nullopt;
            node->dirty = true;
            node->prefix.isLeaf = false;
            return true;
        }

        return false;
    }

    byte firstChar = key[0];
    if (!node->HasChild(firstChar)) {
        return false;
    }

    RTrieNode child = node->GetChild(firstChar);
    bytes oldChildHash = child->cachedHash;

    if (!key.startswith(child->prefix.nibbles)) {
        return false;
    }

    bool deleted = DeleteRecursive(child, key.sub(child->prefix.nibbles.size()));

    if (deleted)
    {
        if (!child->prefix.isLeaf && child->NumChildren() == 1)
        {
            // Compress path if child has no value and only one child
            byte onlyChildKey = child->FirstChildIndex();
            bytes onlyChildHash = child->GetChildHash(onlyChildKey);
            RTrieNode onlyChild = child->GetChild(onlyChildKey, false);
            child->RemoveChild(onlyChildKey);

            child->prefix.nibbles += onlyChild->prefix.nibbles;
            child->value = onlyChild->value;
            child->prefix.isLeaf = onlyChild->prefix.isLeaf;
            child->dirty = true;

            for (uint8_t i = 0; i < 16; i++) {
                if (onlyChild->HasChild(i)) {
                    child->AddChild(i, onlyChild->GetChildHash(i));
                }
            }

            m_Db->Delete(onlyChildHash);
        }
        else if (!child->prefix.isLeaf && child->NumChildren() == 0)
        {
            // Clean up child node
            bytes childHash = node->GetChildHash(firstChar);
            m_Db->Delete(childHash);
            node->RemoveChild(firstChar);
        }
    }

    return deleted;
}

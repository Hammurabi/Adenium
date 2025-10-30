#pragma once
#include <string>
#include <memory>
#include "../lru.h"
#include "../bytes.h"

class Storage;
class RadixNode {
    public:
    RadixNode(Storage* _S);
    bool HasChild(uint8_t Child) const;
    void SetChild(uint8_t Child, const bytes& Hash);
    void SetChild(uint8_t Child, const std::shared_ptr<RadixNode>& _Node);
    bytes GetChildHash(uint8_t Child) const;
    std::shared_ptr<RadixNode> GetChild(uint8_t Child);
    void SetEncodedPath(const bytes& EncodedPath);
    bytes GetEncodedPath() const;
    void SetValue(const bytes& Value);
    bytes GetValue() const;
    void Clear();
    bytes Encode() const;
    bool Decode(const bytes& Hash);
    bool DecodeFrom(const bytes& _Encoded);
    bytes GetHash() const;
    bytes Store();
    void Delete();
    void Drop();
    bool IsEmpty() const;
    private:
    Storage* _Storage;
    bytes _EncodedPath;
    bytes _Value;
    std::pair<bytes, std::shared_ptr<RadixNode>> _Children[16];
};


class RadixTrie
{
public:
    RadixTrie(Storage* Db);
    ~RadixTrie();

    void Insert(const bytes& key, const bytes& value);
    bytes Fetch(const bytes& Key);
    bool Search(const bytes& key, bytes* value);
    void Delete(const bytes& key);
    bool _Insert(const bytes& key, const bytes& value);
    bool _Search(const bytes& key, bytes* value);
    bool _Delete(const bytes& key);

    void Store();
    void Cancel();
    std::shared_ptr<RadixNode> GetNode(const bytes& Hash);
    bytes GetRootHash() const;
    Storage* GetStorage() const
    {
        return _Db;
    }
private:
    static bytes GetCommonPrefix(const bytes& Path1, const bytes& Path2);
    bytes GetCurrentRootHash();
    void SetCurrentRootHash();
    Storage* _Db;
    std::shared_ptr<RadixNode> _Root;
};
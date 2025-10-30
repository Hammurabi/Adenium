#include "trie.h"
#include "db.h"
#include "../util.h"

RadixTrie::RadixTrie(Storage *Db) : _Db(Db), _Root(std::make_shared<RadixNode>(_Db))
{
    bytes LastHash = GetCurrentRootHash();
    if (!LastHash.empty()) {
        if (!_Root->Decode(LastHash))
            throw std::runtime_error("Failed to load root node");
    }
}

RadixTrie::~RadixTrie()
{
}

void RadixTrie::Insert(const bytes &Key, const bytes &Value)
{
    if (_Insert(Key, Value)) {
        _Root->Store();
        SetCurrentRootHash();
    }
    else _Root->Drop();
}

bytes RadixTrie::Fetch(const bytes &Key)
{
    std::shared_ptr<RadixNode> Node = GetNode(Key);
    if (Node)
        return Node->GetValue();
    
    return "";
}

bool RadixTrie::_Insert(const bytes &Key, const bytes &Value)
{
    std::shared_ptr<RadixNode> _Node = _Root;
    bytes Nibbles = BytesToNibbles(Key, nullptr);
    if (Nibbles.empty()) {
        return false;
    }

    while (true)
    {
        if (Nibbles.empty()) {
            if (_Node->GetValue() != Value) _Node->SetValue(Value);
            return true;
        }

        uint8_t FirstByte = Nibbles[0];
        if (!_Node->HasChild(FirstByte)) {
            std::shared_ptr<RadixNode> _New = std::make_shared<RadixNode>(_Db);

            _New->SetValue(Value);
            _New->SetEncodedPath(Nibbles);
            _Node->SetChild(FirstByte, _New);
            return true;
        }

        std::shared_ptr<RadixNode> _Child = _Node->GetChild(FirstByte);
        if (_Child) {
            bytes EdgeLabel = _Child->GetEncodedPath();
            bytes CommonPrefix = GetCommonPrefix(EdgeLabel, Nibbles);
            uint32_t CPLength = CommonPrefix.size();
            if (CPLength == EdgeLabel.size()) {
                Nibbles = Nibbles.sub(CPLength);
                _Node = _Child;
            } else {
                _Node->SetChild(FirstByte, "");
                std::shared_ptr<RadixNode> _Intermediate = std::make_shared<RadixNode>(_Db);
                _Intermediate->SetEncodedPath(CommonPrefix);
                _Node->SetChild(CommonPrefix[0], _Intermediate);

                bytes ExistingSuffix = EdgeLabel.sub(CPLength);
                if (!ExistingSuffix.empty()) {
                    _Child->SetEncodedPath(ExistingSuffix);
                    _Intermediate->SetChild(ExistingSuffix[0], _Child);
                }

                bytes NewSuffix = Nibbles.sub(CPLength);
                if (!NewSuffix.empty()) {
                    std::shared_ptr<RadixNode> _NewChild = std::make_shared<RadixNode>(_Db);
                    _NewChild->SetEncodedPath(NewSuffix);
                    _NewChild->SetValue(Value);
                    _Intermediate->SetChild(NewSuffix[0], _NewChild);
                } else {
                    _Intermediate->SetValue(Value);
                }
                return true;
            }
        } else {
            throw std::runtime_error("Corrupted trie");
        }
    }
    return false;
}

bool RadixTrie::Search(const bytes &Key, bytes *Value)
{
    bool Result = _Search(Key, Value);
    _Root->Drop();
    return Result;
}

bool RadixTrie::_Search(const bytes &Key, bytes *Value)
{
    if (Key.empty()) {
        return false;
    }
    
    bytes Nibbles = BytesToNibbles(Key, nullptr);
    if (Nibbles.empty()) {
        return false;
    }

    std::shared_ptr<RadixNode> _Node = GetNode(GetRootHash());
    while (true)
    {
        if (Nibbles.empty()) {
            if (Value) *Value = _Node->GetValue();
            return true;
        }

        uint8_t FirstByte = Nibbles[0];
        if (!_Node->HasChild(FirstByte)) {
            return false;
        }

        std::shared_ptr<RadixNode> _Child = _Node->GetChild(FirstByte);
        if (_Child) {
            bytes EdgeLabel = _Child->GetEncodedPath();
            
            if (EdgeLabel.empty()) {
                return false;
            }

            Nibbles = Nibbles.sub( EdgeLabel.size() );
            _Node = _Child;
        }
        else {
            throw std::runtime_error("Corrupted trie");
        }
    }
}

void RadixTrie::Delete(const bytes &Key)
{
    if (_Delete(Key)) {
        _Root->Store();
        SetCurrentRootHash();
    }
}

bool RadixTrie::_Delete(const bytes &Key)
{
    if (Key.empty()) {
        return false;
    }
    
    bytes Nibbles = BytesToNibbles(Key, nullptr);
    if (Nibbles.empty()) {
        return false;
    }

    std::shared_ptr<RadixNode> _Node = _Root;
    while (true)
    {
        uint8_t FirstByte = Nibbles[0];
        if (!_Node->HasChild(FirstByte)) {
            return false;
        }

        std::shared_ptr<RadixNode> _Child = _Node->GetChild(FirstByte);
        if (_Child) {
            bytes EdgeLabel = _Child->GetEncodedPath();
            
            if (EdgeLabel.empty()) {
                return false;
            }

            Nibbles = Nibbles.sub( EdgeLabel.size() );


            if (Nibbles.empty()) {
                _Node->SetChild(FirstByte, "");
                _Child->Delete();
                return true;
            }
            _Node = _Child;
        }
        else {
            throw std::runtime_error("Corrupted trie");
        }
    }
}

void RadixTrie::Store()
{
    _Root->Store();
    SetCurrentRootHash();
}

void RadixTrie::Cancel()
{
    _Root->Drop();
}

std::shared_ptr<RadixNode> RadixTrie::GetNode(const bytes &Hash)
{
    if (Hash.empty()) {
        return nullptr;
    }

    bytes Encoded;
    if (!_Db->Get(Hash, &Encoded)) {
        return nullptr;
    }
    std::shared_ptr<RadixNode> _Node = std::make_shared<RadixNode>(_Db);
    if (!_Node->Decode(Encoded)) {
        return nullptr;
    }
    return _Node;
}

bytes RadixTrie::GetRootHash() const
{
    if (_Root->IsEmpty()) {
        return "";
    }

    return _Root->GetHash();
}

bytes RadixTrie::GetCommonPrefix(const bytes &Path1, const bytes &Path2)
{
    size_t MinLength = std::min(Path1.size(), Path2.size());
    for (size_t i = 0; i < MinLength; ++i) {
        if (Path1[i] != Path2[i]) {
            return Path1.sub(0, i);
        }
    }
    return Path1.sub(0, MinLength);
}

bytes RadixTrie::GetCurrentRootHash()
{
    bytes RootHash;
    if (_Db->Get("Trie.RootHash", &RootHash)) {
        return RootHash;
    }

    return bytes();
}

void RadixTrie::SetCurrentRootHash()
{
    bytes RootHash = GetRootHash();
    if (!RootHash.empty()) {
        _Db->Put("Trie.RootHash", RootHash);
    }
}

RadixNode::RadixNode(Storage *_S) : _Storage(_S), _EncodedPath(""), _Value("") {
    for (int i = 0; i < 16; ++i) _Children[i] = std::make_pair<bytes, std::shared_ptr<RadixNode>>("", nullptr);
}

bool RadixNode::HasChild(uint8_t Child) const
{
    if (Child > 15) {
        return false;
    }
    return !_Children[Child].first.empty();
}

void RadixNode::SetChild(uint8_t Child, const bytes &Hash)
{
    if (Child > 15) {
        return;
    }

    _Children[Child].first = Hash;
    _Children[Child].second = nullptr;
}

void RadixNode::SetChild(uint8_t Child, const std::shared_ptr<RadixNode> &_Node)
{
    if (Child > 15) {
        return;
    }

    bytes _Hash = _Node->GetHash();
    _Children[Child] = std::make_pair(_Hash, _Node);
}

bytes RadixNode::GetChildHash(uint8_t Child) const
{
    if (Child > 15) {
        return "";
    }
    return _Children[Child].first;
}

std::shared_ptr<RadixNode> RadixNode::GetChild(uint8_t Child)
{
    if (Child > 15) {
        return nullptr;
    }
    if (_Children[Child].first.empty()) {
        return nullptr;
    }
    if (_Children[Child].second) {
        return _Children[Child].second;
    }
    bytes Hash = _Children[Child].first;
    std::shared_ptr<RadixNode> _Node = std::make_shared<RadixNode>(_Storage);
    if (!_Node->Decode(Hash)) {
        throw std::runtime_error("Failed to decode child node");
    }
    _Children[Child] = std::make_pair(Hash, _Node);
    return _Node;
}

void RadixNode::SetEncodedPath(const bytes &EncodedPath)
{
    _EncodedPath = EncodedPath;
}

bytes RadixNode::GetEncodedPath() const
{
    return _EncodedPath;
}

void RadixNode::SetValue(const bytes &Value)
{
    _Value = Value;
}

bytes RadixNode::GetValue() const
{
    return _Value;
}

void RadixNode::Clear()
{
    _EncodedPath.clear();
    _Value.clear();
    for (int i = 0; i < 16; ++i) _Children[i] = std::make_pair<bytes, std::shared_ptr<RadixNode>>("", nullptr);
}

bytes RadixNode::Encode() const
{
    uint16_t BitMask = 0;
    bytes EncodedPath = NibblesToBytes(GetEncodedPath(), GetValue().empty(), true);
    bytes Encoded = EncodeVarInt(EncodedPath.size()) + EncodedPath;

    for (int i = 0; i < 16; ++i) {
        if (!GetChildHash(i).empty()) {
            BitMask |= (1 << i);
            Encoded += GetChildHash(i);
        }
    }

    bytes BitMaskBytes;
    BitMaskBytes.resize(2);
    // std::cout << "BitMask: " << BitMask << std::endl;
    BitMask = ToBigEndian(BitMask) >> 48;
    BitMaskBytes[0] = (BitMask >> 8) & 0xFF;
    BitMaskBytes[1] = BitMask & 0xFF;

    Encoded = BitMaskBytes + Encoded;
    if (!GetValue().empty()) Encoded += EncodeVarInt(GetValue().size()) + GetValue();
    return Encoded;
}

bool RadixNode::DecodeFrom(const bytes &_Encoded)
{
    SafeStream Stream;
    Stream.FromBytes(_Encoded);

    uint16_t BitMask = 0;
    if (!Stream.Read((uint8_t *)&BitMask, 2)) {
        return false;
    }
    BitMask = ToBigEndian(BitMask) >> 40;
    // std::cout << "BitMask: " << BitMask << std::endl;

    uint64_t EncodedPathSize = DecodeVarInt(&Stream);
    bytes EncodedPath;
    EncodedPath.resize(EncodedPathSize);
    if (!Stream.Read(reinterpret_cast<uint8_t*>(EncodedPath.data()), EncodedPathSize)) {
        return false;
    }

    _Value.clear();
    for (int i = 0; i < 16; ++i) {
        if (BitMask & (1 << i)) {
            bytes Child;
            Child.resize(32);
            if (!Stream.Read(reinterpret_cast<uint8_t*>(Child.data()), 32)) {
                return false;
            }
            SetChild(i, Child);
        }
    }

    bool HasValue = false;
    SetEncodedPath(BytesToNibbles(EncodedPath, &HasValue));

    if (HasValue) {
        uint64_t ValueSize = DecodeVarInt(&Stream);
        if (ValueSize > 1024) return false;
        bytes Value(ValueSize, '\0');
        if (!Stream.Read(reinterpret_cast<uint8_t*>(&Value[0]), ValueSize)) {
            return false;
        }
        SetValue(Value);
    }


    return true;
}

bool RadixNode::Decode(const bytes &_Hash)
{
    bytes _Encoded;
    if (!_Storage->Get(_Hash, &_Encoded)) {
        return false;
    }

    return DecodeFrom(_Encoded);
}


bytes RadixNode::GetHash() const
{
    return Keccak(Encode());
}

bytes RadixNode::Store()
{
    for (int i = 0; i < 16; ++i) {
        if (_Children[i].second) {
            auto Hash = _Children[i].second->Store();
            _Children[i].first = Hash;
        }
    }

    bytes Data = Encode();
    bytes Hash = Keccak(Data);
    if (_Storage->Get(Hash, &Data)) return Hash;
    _Storage->Put(Hash, Data);
    return Hash;
}

void RadixNode::Delete()
{
    for (int i = 0; i < 16; ++i) {
        if (HasChild(i)) {
            auto Child = GetChild(i);
            if (Child) {
                Child->Delete();
            }
        }
    }
    _Storage->Delete(GetHash());
}

void RadixNode::Drop()
{
    for (int i = 0; i < 16; ++i) _Children[i].second = nullptr;
}

bool RadixNode::IsEmpty() const
{
    for (int i = 0; i < 16; ++i) {
        if (!_Children[i].first.empty()) {
            return false;
        }
    }
    return _EncodedPath.empty() && _Value.empty();
}

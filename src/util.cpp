#include "util.h"
#include <bit>
#include <vector>
#include <iostream>
#include <openssl/rand.h>

uint32_t fnv(uint32_t x, uint32_t y) {
    return (x * FNV_PRIME) ^ y;
}

void xorSlots(DAGSlot &dest, const DAGSlot &src) {
    for (size_t i = 0; i < 64; i++)
        dest.Slot[i] ^= src.Slot[i];
}

void fnvMix(DAGSlot &a, const DAGSlot &b) {
    for (size_t i = 0; i < 16; i++) {
        uint32_t* aw    = reinterpret_cast<uint32_t*>(a.Slot + i * 4);
        uint32_t bw     = ToBigEndian32(reinterpret_cast<const uint32_t*>(b.Slot + i * 4)[0]);
        *aw = ToBigEndian32(fnv(ToBigEndian32(*aw), bw));
    }
}

void CacheMix(std::vector<DAGSlot> &cache, size_t rounds) {
    size_t n = cache.size();
    DAGSlot tmp;

    for (size_t r = 0; r < rounds; r++) {
        for (size_t i = 0; i < n; i++) {
            uint32_t *words = reinterpret_cast<uint32_t*>(cache[i].Slot);
            size_t prev_index = fnv(static_cast<uint32_t>(i ^ r), ToBigEndian32(words[0])) % n;

            std::memcpy(tmp.Slot, cache[prev_index].Slot, 64);
            for (size_t j = 0; j < 64; j++) {
                tmp.Slot[j] ^= cache[i].Slot[j];
            }

            Keccak512(tmp.Slot, 64, tmp.Slot);
            fnvMix(cache[i], tmp);
        }
    }
}

void BuildCache(std::vector<DAGSlot> &Cache)
{
    for (size_t I = 1; I < Cache.size(); I ++)
    {
        Keccak512(reinterpret_cast<const uint8_t*>(&Cache[I - 1]), 64, reinterpret_cast<uint8_t*>(&Cache[I]));
    }
}

void BuildDAG(const std::vector<DAGSlot> &cache, std::vector<DAGNode> &dag)
{
    size_t cache_size = cache.size();
    size_t dag_size = dag.size();

    for (uint64_t i = 0; i < static_cast<uint64_t>(dag_size); i++) {
        DAGNode node{};

        uint32_t c = static_cast<uint32_t>(i);
        uint32_t *a_words = reinterpret_cast<uint32_t*>(node.A.Slot);
        uint32_t *b_words = reinterpret_cast<uint32_t*>(node.B.Slot);

        for (size_t j = 0; j < 16; j++) { // 64 bytes / 4 bytes per word = 16
            a_words[j] = ToBigEndian32(c * j);
            b_words[j] = ToBigEndian32(c * j);
        }

        for (size_t j = 0; j < 256; j++) {
            size_t idx0 = fnv(uint32_t(i ^ j), ToBigEndian32(*reinterpret_cast<const uint32_t*>(&cache[i%cache_size]))) % cache_size;
            fnvMix(node.A, cache[idx0]);
            fnvMix(node.B, cache[(idx0 + 1) % cache_size]); // wrap for second half
        }

        Keccak512(reinterpret_cast<const uint8_t*>(&node.A), 64, reinterpret_cast<uint8_t*>(&node.A));
        Keccak512(reinterpret_cast<const uint8_t*>(&node.B), 64, reinterpret_cast<uint8_t*>(&node.B));

        dag[i] = node;
    }
}

void DumpDAGToFile(const std::vector<DAGNode> &dag, const std::string &filename) {
    std::ofstream out(filename, std::ios::binary);
    if (!out) {
        throw std::runtime_error("Failed to open file");
    }

    out.write(reinterpret_cast<const char*>(dag.data()), dag.size() * sizeof(DAGNode));
    out.close();
}

void LoadDAGFromFile(std::vector<DAGNode> &dag, const std::string &filename) {
    std::ifstream in(filename, std::ios::binary | std::ios::ate);
    if (!in) throw std::runtime_error("Failed to open file");

    std::streamsize size = in.tellg();
    in.seekg(0, std::ios::beg);

    size_t num_nodes = size / sizeof(DAGNode);
    dag.resize(num_nodes);

    if (!in.read(reinterpret_cast<char*>(dag.data()), size)) {
        throw std::runtime_error("Failed to read file");
    }
}

int256_t to_int256(const uint8_t *bytes)
{
    int256_t result = 0;
    for (int i = 0; i < 32; ++i) {
        result <<= 8;
        result |= bytes[i];
    }
    return result;
}

uint256_t to_uint256(const uint8_t *bytes)
{
    uint256_t result = 0;
    for (int i = 0; i < 32; ++i) {
        result <<= 8;
        result |= bytes[i];
    }
    return result;
}

uint64_t TimeMs()
{
    using namespace std::chrono;
    auto now = system_clock::now();
    auto duration = now.time_since_epoch();
    return duration_cast<milliseconds>(duration).count();
}

bytes BytesFromHex(const std::string &Hex)
{
    if (Hex.size() % 2 != 0) {
        throw std::invalid_argument("Hex string must have even length");
    }

    auto FromHex = [](char c) -> uint8_t {
        if (std::isdigit(c)) return c - '0';
        if (std::isxdigit(c)) return std::tolower(c) - 'a' + 10;
        throw std::invalid_argument("Invalid hex character");
    };

    std::string Result;
    Result.reserve(Hex.size() / 2); // Reserve space for performance

    for (size_t i = 0; i < Hex.size(); i += 2) {
        uint8_t High = FromHex(Hex[i]);
        uint8_t Low  = FromHex(Hex[i + 1]);
        Result.push_back(static_cast<char>((High << 4) | Low));
    }

    return Result;
}

std::string HexFromBytes(const bytes &Bytes)
{
    static const char* HexChars = "0123456789abcdef";

    std::string Hex;
    Hex.reserve(Bytes.size() * 2);

    for (unsigned char c : Bytes) {
        Hex.push_back(HexChars[(c >> 4) & 0xF]);
        Hex.push_back(HexChars[c & 0xF]);
    }

    return Hex;
}

std::string HexFromBytes(const std::vector<uint8_t> Bytes)
{
    return HexFromBytes(std::string(Bytes.begin(), Bytes.end()));
}

std::string HexFromBytes(const uint8_t *Bytes, size_t Length)
{
    return HexFromBytes(std::string(Bytes, Bytes + Length));
}

bytes AddressFromPubkey(const bytes &Bytes)
{
    return Keccak(Bytes).sub(0, 20);
}

bytes SecureSeed(size_t n_bytes)
{
    std::vector<uint8_t> seed(n_bytes);
    if (RAND_bytes(seed.data(), static_cast<int>(n_bytes)) != 1) {
        throw std::runtime_error("OpenSSL RAND_bytes failed");
    }
    return seed;
}

bytes DerivePBKDF2(const std::string &password, const bytes &salt, size_t key_len)
{
    std::vector<uint8_t> key(key_len);
    if (!PKCS5_PBKDF2_HMAC(password.c_str(), password.size(),
                           salt.data(), salt.size(),
                           100000, // iterations
                           EVP_sha256(),
                           key_len,
                           key.data())) {
        throw std::runtime_error("Key derivation failed");
    }
    return key;
}

bytes Encrypt_AES_GCM(const bytes &plaintext, const bytes &key, bytes &iv, bytes &tag)
{
    EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
    if (!ctx) throw std::runtime_error("EVP_CIPHER_CTX_new failed");

    iv.resize(12); // 96-bit IV for GCM
    if (!RAND_bytes(iv.data(), iv.size()))
        throw std::runtime_error("IV generation failed");

    if (1 != EVP_EncryptInit_ex(ctx, EVP_aes_256_gcm(), NULL, NULL, NULL))
        throw std::runtime_error("EncryptInit failed");
    if (1 != EVP_EncryptInit_ex(ctx, NULL, NULL, key.data(), iv.data()))
        throw std::runtime_error("EncryptInit key/iv failed");

    std::vector<uint8_t> ciphertext(plaintext.size());
    int len;
    if (1 != EVP_EncryptUpdate(ctx, ciphertext.data(), &len, plaintext.data(), plaintext.size()))
        throw std::runtime_error("EncryptUpdate failed");

    int ciphertext_len = len;
    if (1 != EVP_EncryptFinal_ex(ctx, ciphertext.data() + len, &len))
        throw std::runtime_error("EncryptFinal failed");

    ciphertext_len += len;
    ciphertext.resize(ciphertext_len);

    tag.resize(16); // 128-bit tag
    if (1 != EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_GET_TAG, tag.size(), tag.data()))
        throw std::runtime_error("Get GCM tag failed");

    EVP_CIPHER_CTX_free(ctx);
    return ciphertext;
}

bytes Decrypt_AES_GCM(const bytes &ciphertext, const bytes &key, const bytes &iv, const bytes &tag)
{
    EVP_CIPHER_CTX* ctx = EVP_CIPHER_CTX_new();
    if (!ctx) throw std::runtime_error("EVP_CIPHER_CTX_new failed");

    if (1 != EVP_DecryptInit_ex(ctx, EVP_aes_256_gcm(), NULL, NULL, NULL))
        throw std::runtime_error("DecryptInit failed");
    if (1 != EVP_DecryptInit_ex(ctx, NULL, NULL, key.data(), iv.data()))
        throw std::runtime_error("DecryptInit key/iv failed");

    std::vector<uint8_t> plaintext(ciphertext.size());
    int len;
    if (1 != EVP_DecryptUpdate(ctx, plaintext.data(), &len, ciphertext.data(), ciphertext.size()))
        throw std::runtime_error("DecryptUpdate failed");

    int plaintext_len = len;

    if (1 != EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_GCM_SET_TAG, tag.size(), const_cast<uint8_t*>(tag.data())))
        throw std::runtime_error("Set GCM tag failed");

    if (1 != EVP_DecryptFinal_ex(ctx, plaintext.data() + len, &len)) {
        EVP_CIPHER_CTX_free(ctx);
        throw std::runtime_error("Decryption failed: wrong password or data corrupted");
    }

    plaintext_len += len;
    plaintext.resize(plaintext_len);

    EVP_CIPHER_CTX_free(ctx);
    return plaintext;
}


bytes BytesToNibbles(const bytes &Bytes, bool *IsEncoded)
{
    bytes Nibbles;
    Nibbles.reserve(Bytes.size() * 2);
    for (unsigned char c : Bytes) {
        Nibbles.push_back((c >> 4) & 0x0F);
        Nibbles.push_back(c & 0x0F);
    }

    if (IsEncoded) {
        if (Nibbles[0] == ODD_NIBBLE) {
            Nibbles = Nibbles.sub(1);
            *IsEncoded = true;
        } else if (Nibbles[0] == EVEN_NIBBLE) {
            Nibbles = Nibbles.sub(2);
            *IsEncoded = true;
        } else if (Nibbles[0] == ODD_NIBBLE_EMPTY) {
            Nibbles = Nibbles.sub(1);
            *IsEncoded = false;
        } else if (Nibbles[0] == EVEN_NIBBLE_EMPTY) {
            Nibbles = Nibbles.sub(2);
            *IsEncoded = false;
        }
    }

    return Nibbles;
}

bytes NibblesToBytes(const bytes &Nibbles, bool IsEmpty, bool IsEncoded)
{
    bytes _Nibbles;

    if (IsEncoded) {
        if (Nibbles.size() % 2 == 0) {
            _Nibbles.push_back(IsEmpty ? EVEN_NIBBLE_EMPTY : EVEN_NIBBLE);
            _Nibbles.push_back(0x00);
        } else {
            _Nibbles.push_back(IsEmpty ? ODD_NIBBLE_EMPTY : ODD_NIBBLE);
        }

        _Nibbles.append(Nibbles);
    } else {
        _Nibbles = Nibbles;
    }
    
    std::string Bytes;
    Bytes.reserve(Nibbles.size() / 2);

    for (size_t i = 0; i < _Nibbles.size(); i += 2) {
        unsigned char High = (_Nibbles[i] & 0x0F) << 4;
        unsigned char Low  = _Nibbles[i + 1] & 0x0F;
        Bytes.push_back(High | Low);
    }

    return Bytes;
}

std::string EncodeBits(uint128_t Bits)
{
    std::vector<unsigned char> Bytes;
    export_bits(Bits, std::back_inserter(Bytes), 8, true);
    return std::string(reinterpret_cast<const char*>(Bytes.data()), Bytes.size());
}

uint128_t DecodeBits(SafeStream *Stream)
{
    bytes Bytes = Stream->ReadBytes(16); // must return exactly 16 bytes
    uint128_t Value;
    import_bits(Value,
                Bytes.data(),
                Bytes.data() + Bytes.size(),
                8,
                true); // big-endian
    return Value;
}

uint32_t ToBigEndian32(uint32_t Num)
{
    if (std::endian::native == std::endian::big)
        return Num;

    return ((Num & 0x000000FFU) << 24) |
           ((Num & 0x0000FF00U) << 8)  |
           ((Num & 0x00FF0000U) >> 8)  |
           ((Num & 0xFF000000U) >> 24);
}

uint64_t ToBigEndian(uint64_t Num)
{
    if (std::endian::native == std::endian::big)
        return Num;

    return ((Num & 0x00000000000000FFULL) << 56) |
           ((Num & 0x000000000000FF00ULL) << 40) |
           ((Num & 0x0000000000FF0000ULL) << 24) |
           ((Num & 0x00000000FF000000ULL) << 8)  |
           ((Num & 0x000000FF00000000ULL) >> 8)  |
           ((Num & 0x0000FF0000000000ULL) >> 24) |
           ((Num & 0x00FF000000000000ULL) >> 40) |
           ((Num & 0xFF00000000000000ULL) >> 56);
}

bytes ToBytes(uint64_t Value)
{
    bytes Bytes;
    Bytes.resize(sizeof(Value));
    uint64_t NetworkValue = ToBigEndian(Value);
    std::memcpy(&Bytes[0], &NetworkValue, sizeof(Value));
    return Bytes;
}

void ToBytes(int256_t Value, uint8_t* Bytes)
{
    for (int i = 31; i >= 0; --i) { // big-endian
        Bytes[i] = static_cast<uint8_t>(Value & 0xFF);
        Value >>= 8;
    }
}

void ToBytes(uint256_t Value, uint8_t *Bytes)
{
    for (int i = 31; i >= 0; --i) { // big-endian
        Bytes[i] = static_cast<uint8_t>(Value & 0xFF);
        Value >>= 8;
    }
}

SafeStream::SafeStream() : position_(0), buffer_("") {}

void SafeStream::Write(const bytes &data)
{
    buffer_.append(data);
}

void SafeStream::Write(const uint8_t *data, size_t size)
{
    buffer_.append(bytes(data, size));
}

bool SafeStream::Read(uint8_t *data, size_t size)
{
    if (position_ + size > buffer_.size()) {
        return false;
    }

    std::copy(buffer_.begin() + position_, buffer_.begin() + position_ + size, data);
    position_ += size;
    return true;
}

void SafeStream::ReadInto(uint8_t *data, size_t size)
{
    if (!Read(reinterpret_cast<uint8_t*>(data), size)) {
        throw std::runtime_error("Failed to read remaining bytes of VarInt");
    }
}

bytes SafeStream::ReadBytes(uint64_t Length)
{
    bytes Bytes(Length, 0);
    if (!Read(reinterpret_cast<uint8_t*>(&Bytes[0]), Length)) {
        throw std::runtime_error("Failed to read remaining bytes of VarInt");
    }

    return Bytes;
}

uint32_t SafeStream::ReadByte()
{
    uint32_t Byte = buffer_[position_];
    position_ += 1;

    return Byte;
}

bool SafeStream::Seek(size_t offset)
{
    if (offset > buffer_.size()) {
        return false;
    }
    position_ = offset;
    return true;
}

size_t SafeStream::Tell() const
{
    return position_;
}

size_t SafeStream::Size() const
{
    return buffer_.size();
}

void SafeStream::Reset()
{
        position_ = 0;
}

void SafeStream::FromBytes(const bytes &Bytes)
{
    buffer_ = Bytes;
    position_ = 0;
}

bytes SafeStream::Bytes() const
{
    return buffer_;
}

bool SafeStream::Eof() const { return position_ >= buffer_.size(); }

bytes EncodeVarInt(uint64_t value)
{
    if (value >= (1ULL << 61)) {
        throw std::runtime_error("Value exceeds maximum of 2^61 - 1");
    }
    
    // Determine the number of bytes needed (1 to 8)
    uint8_t byteLength = 0;
    if (value < (1ULL << 5)) {          // 5 bits (first 5 bits of first byte)
        byteLength = 1;
    } else if (value < (1ULL << 13)) {  // 13 bits
        byteLength = 2;
    } else if (value < (1ULL << 21)) {  // 21 bits
        byteLength = 3;
    } else if (value < (1ULL << 29)) {  // 29 bits
        byteLength = 4;
    } else if (value < (1ULL << 37)) {  // 37 bits
        byteLength = 5;
    } else if (value < (1ULL << 45)) {  // 45 bits
        byteLength = 6;
    } else if (value < (1ULL << 53)) {  // 53 bits
        byteLength = 7;
    } else {                           // 61 bits
        byteLength = 8;
    }
    
    bytes bigEndianBytes = ToBytes(value);
    std::string result;
    result.reserve(byteLength);
    
    uint8_t firstByte = (byteLength - 1) << 5;
    firstByte |= (static_cast<uint8_t>(bigEndianBytes[8 - byteLength]) & 0x1F);
    result.push_back(firstByte);
    
    for (uint8_t i = 1; i < byteLength; ++i) {
        result.push_back(bigEndianBytes[8 - byteLength + i]);
    }
    
    return result;
}

uint64_t DecodeVarInt(const std::string& bytes) {
    if (bytes.empty()) {
        throw std::runtime_error("Empty input for VarInt decoding");
    }
    
    uint8_t byteLength = ((bytes[0] >> 5) & 0x07) + 1;
    
    if (bytes.size() < byteLength) {
        throw std::runtime_error("Incomplete VarInt: expected " + 
                               std::to_string(byteLength) + " bytes, got " +
                               std::to_string(bytes.size()));
    }
    
    std::string fullBytes(8, 0);
    fullBytes[8 - byteLength] = bytes[0] & 0x1F;
    for (uint8_t i = 1; i < byteLength; ++i) {
        fullBytes[8 - byteLength + i] = bytes[i];
    }
    
    uint64_t value = 0;
    std::memcpy(&value, fullBytes.data(), 8);
    return ToBigEndian(value);
}

uint64_t DecodeVarInt(SafeStream *Stream)
{
    uint8_t FirstByte;
    if (!Stream->Read(&FirstByte, 1)) {
        throw std::runtime_error("Failed to read first byte of VarInt");
    }

    uint8_t ByteLength = ((FirstByte >> 5) & 0x07) + 1;
    FirstByte &= 0x1F;

    std::string FullBytes(8, 0);
    FullBytes[8 - ByteLength] = FirstByte;
    if (!Stream->Read(reinterpret_cast<uint8_t*>(&FullBytes[8 - ByteLength + 1]), ByteLength - 1)) {
        throw std::runtime_error("Failed to read remaining bytes of VarInt");
    }
    uint64_t Value = 0;
    std::memcpy(&Value, FullBytes.data(), 8);
    return ToBigEndian(Value);
}

bytes EncodeInteger(const cpp_int &_Integer)
{
    unsigned char Bytes[96];

    int Start = 0;
    bool IsSigned = false;
    cpp_int Integer = _Integer;
    if (Integer < 0) {
        IsSigned = true;
        Integer = -Integer;
    }

    for (int i = 0; i < 96; ++i) {
        unsigned char Byte = static_cast<unsigned char>((Integer >> (8 * (95 - i))) & 0xFF);
        if (Start || Byte != 0) Bytes[Start++] = Byte;
    }

    std::string Result = std::string(reinterpret_cast<char*>(Bytes), Start);
    uint8_t Length = static_cast<uint8_t>(Result.length());
    if (Length > 0x7F)
        throw std::runtime_error("Integer too large to encode");
    if (IsSigned) Length |= 0x80;
    Result.insert(Result.begin(), Length);
    return Result;
}

cpp_int DecodeInteger(SafeStream *Stream)
{
    cpp_int result = 0;
    uint8_t Length;
    if (!Stream->Read(&Length, 1)) {
        throw std::runtime_error("Failed to read length byte");
    }
    bool IsSigned = Length & 0x80;
    Length &= 0x7F;
    std::string Integer(Length, 0);
    if (!Stream->Read(reinterpret_cast<uint8_t*>(&Integer[0]), Length)) {
        throw std::runtime_error("Failed to read integer bytes");
    }

    for (int i = 0; i < Length; ++i) {
        result = (result << 8) | static_cast<unsigned char>(Integer[i]);
    }

    if (IsSigned) {
        result = -result;
    }

    return result;
}

uint256_t GetTargetFromCompact(uint32_t nBits)
{
    uint32_t Exponent = nBits >> 24;
    uint32_t Mantissa = nBits & 0x00FFFFFF;

    uint256_t Target = Mantissa;
    Target <<= (8 * (Exponent - 3));

    return Target;
}

uint256_t HashToInt(const std::string &Str)
{
    if (Str.size() > 32) throw std::invalid_argument("String is too long to fit into a uint256_t");

    uint256_t Result = 0;
    for (size_t i = 0; i < Str.size(); ++i) {
        Result |= (uint256_t)(unsigned char)Str[i] << (8 * (Str.size() - 1 - i));
    }
    
    return Result;
}

bool IsNumber(const std::string &Str)
{
    for (char c : Str) {
        if (!std::isdigit(c)) {
            return false;
        }
    }
    return true;
}

uint64_t FromDecimal(const std::string &Decimal)
{
    size_t DotPos = Decimal.find('.');
    std::string WholePart = Decimal.substr(0, DotPos);
    std::string FractionalPart = (DotPos != std::string::npos) ? Decimal.substr(DotPos + 1) : "0";
    uint64_t WholeValue = std::stoull(WholePart) * OneCoin;
    uint64_t FractionalValue = 0;
    for (size_t i = 0; i < FractionalPart.size(); ++i) {
        if (i >= NumDecimals)
            break;

        FractionalValue += (FractionalPart[i] - '0') * (OneCoin / static_cast<uint64_t>(std::pow(10, i + 1)));
    }
    return WholeValue + FractionalValue;
}

bytes EncodeVarBytes(const bytes &Bytes)
{
    return EncodeVarInt(static_cast<uint64_t>(Bytes.size())) + Bytes;
}

bytes DecodeVarBytes(SafeStream *Stream)
{
    uint64_t Size = DecodeVarInt(Stream);
    return Stream->ReadBytes(Size);
}
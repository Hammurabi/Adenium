#pragma once
#include <fstream>
#include <vector>
#include <string>
#include <stdexcept>
#include <cctype>
#include <cstdint>
#include <chrono>
#include <variant>
#include <boost/multiprecision/cpp_int.hpp>
#include "keccak.h"
#include "bytes.h"

struct DAGSlot {
    uint8_t Slot[64];
};

struct DAGNode {
    DAGSlot A;
    DAGSlot B;
};

// 23,058,430. 092 136 939
// Supply/8b =  .288230376
constexpr uint64_t MaxCoin             = 23058430092136939ULL;
constexpr uint64_t OneCoin             = 1000000000ULL;
constexpr uint64_t NumDecimals         = 9ULL;

constexpr uint64_t StateShardCount = 160;
constexpr uint64_t ComputeUnitsCount = 64;
constexpr uint64_t ShardCount = ( StateShardCount + ComputeUnitsCount );
constexpr uint64_t ComputeUnitStartAt = StateShardCount;
constexpr uint64_t MaxPendingTransactionsProcessed = 16777216;
constexpr uint64_t MaxBlockSize = 1048576;
constexpr uint64_t MaxTransactionSize = 65648;
constexpr uint64_t MaxTransactionCodeSize = 24576;
constexpr uint64_t MaxTransactionExcCodeSize = 24576;
// constexpr uint64_t MaxAccountSpace = 1099511600000;
constexpr uint64_t MaxAccountSpacePerIteration = 8;

constexpr uint64_t MinAccountValue             = 100;
constexpr uint64_t MinDeploymentAccountValue   = 10000;
constexpr uint64_t RegisterAccountFee          = 10000;
constexpr uint64_t TransferAccountFee          = 100;
constexpr uint64_t RegisterAssetFee            = 1000000;
constexpr uint64_t RegisterDeploymentFee       = 1000000;
constexpr uint64_t MinTransferAmount           = 1;
constexpr uint64_t TransactionFee              = 72;
constexpr uint64_t SwapFee                     = 243;
constexpr uint64_t StakeAmount                 = 8ULL * OneCoin;
constexpr uint64_t ExecutionFee                = 7200;
constexpr uint64_t GasFee                      = 1;
constexpr uint64_t CodeByteStorageFee          = 384;
constexpr uint64_t ByteStorageFee              = 24;
constexpr uint64_t FunctionCallFee             = 48;
constexpr uint64_t BaseStorageFee              = 768;
constexpr uint64_t BaseLoadFee                 = 64;
constexpr uint64_t SlotStorageFee              = 1536;
constexpr uint64_t SlotStorageRefund           = 384ULL;

constexpr uint64_t SlotDuration                = 12;
constexpr uint64_t SlotsPerEpoch               = 32;

// Execution Related
constexpr uint64_t KeccakFee                   = 32;

#define EmptySignature              bytes(96, '\0')
#define EMPTY_BYTES                 bytes(32, '\0')

// FNV constants
constexpr uint32_t FNV_PRIME = 0x01000193;
constexpr uint32_t FNV_OFFSET_BASIS = 0x811C9DC5;
#define InitialDagSeed "0815ba0efb7f9c5dec9e22012f5b92005c2847c13adf55a87bfee7974cad965ac69c8aba1acb89984d92695f2b1046733bdd599d1e062ff9baff792dc2a2c989"

// FNV hash of two 32-bit words
uint32_t fnv(uint32_t x, uint32_t y);

// XOR two DAGSlot elements
void xorSlots(DAGSlot& dest, const DAGSlot& src);

// Apply FNV mixing on 32-byte slot
void fnvMix(DAGSlot& a, const DAGSlot& b);

// Cache mixing function with FNV
void CacheMix(std::vector<DAGSlot>& cache, size_t rounds);

// DAG generation
void BuildCache(std::vector<DAGSlot>& Cache);
void BuildDAG(const std::vector<DAGSlot>& cache, std::vector<DAGNode>& dag);

void DumpDAGToFile(const std::vector<DAGNode> &dag, const std::string &filename);
void LoadDAGFromFile(std::vector<DAGNode> &dag, const std::string &filename);

using uint128_t = boost::multiprecision::uint128_t;
using int256_t = boost::multiprecision::int256_t;
using uint256_t = boost::multiprecision::uint256_t;
using boost::multiprecision::cpp_int;

int256_t to_int256(const uint8_t* bytes);
uint256_t to_uint256(const uint8_t* bytes);

uint64_t TimeMs();
bytes BytesFromHex(const std::string& Hex);
std::string HexFromBytes(const bytes& Bytes);
bytes AddressFromPubkey(const bytes& Bytes);
bytes SecureSeed(size_t n_bytes);
bytes DerivePBKDF2(const std::string& password, const bytes& salt, size_t key_len = 32);
bytes Encrypt_AES_GCM(const bytes& plaintext,
                                     const bytes& key,
                                     bytes& iv,
                                     bytes& tag);
bytes Decrypt_AES_GCM(const bytes& ciphertext,
                                     const bytes& key,
                                     const bytes& iv,
                                     const bytes& tag);


#define EVEN_NIBBLE_EMPTY 0x00
#define ODD_NIBBLE_EMPTY 0x01
#define EVEN_NIBBLE 0x02
#define ODD_NIBBLE 0x03
bytes BytesToNibbles(const bytes& Bytes, bool *IsEncoded=nullptr);
bytes NibblesToBytes(const bytes& Nibbles, bool IsEmpty, bool IsEncoded=false);

class SafeStream {
public:
    SafeStream();
    void Write(const bytes& data);
    void Write(const uint8_t* data, size_t size);
    bool Read(uint8_t* data, size_t size);
    void ReadInto(uint8_t* data, size_t size);
    uint32_t ReadByte();
    bytes ReadBytes(uint64_t Length);
    bool Seek(size_t offset);
    size_t Tell() const;
    size_t Size() const;
    void Reset();
    void FromBytes(const bytes& Bytes);
    bytes Bytes() const;
    bool Eof() const;
private:
    size_t position_;
    bytes buffer_;
};

struct HashStrengthComparator {
    bool operator()(const std::string& a, const std::string& b) const { // stronger on top
        return ComputeStrength(a) < ComputeStrength(b);
    }

    int ComputeStrength(const std::string& hash) const {
        return std::count(hash.begin(), hash.end(), '0');
    }
};

std::string EncodeBits(uint128_t Bits);
uint128_t DecodeBits(SafeStream* Stream);
uint32_t ToBigEndian32(uint32_t Num);
uint64_t ToBigEndian(uint64_t Num);
bytes ToBytes(uint64_t Value);
void ToBytes(int256_t Value, uint8_t* Bytes);
void ToBytes(uint256_t Value, uint8_t* Bytes);
bytes EncodeVarInt(uint64_t Value);
uint64_t DecodeVarInt(const bytes &VarInt);
uint64_t DecodeVarInt(SafeStream* Stream);
bytes EncodeVarInteger(uint256_t Value);
uint256_t DecodeVarInteger(SafeStream* Stream);
bytes EncodeInteger(const cpp_int &Integer);
cpp_int DecodeInteger(SafeStream* Stream);
uint256_t GetTargetFromCompact(uint32_t nBits);
uint256_t HashToInt(const std::string& Str);
bool IsNumber(const std::string& Str);

bytes EncodeVarBytes(const std::string& Bytes);
bytes DecodeVarBytes(SafeStream* Stream);

uint64_t FromDecimal(const std::string& Decimal);
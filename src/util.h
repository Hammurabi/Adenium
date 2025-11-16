#pragma once
#include <string>
#include <vector>
#include <string>
#include <chrono>
#include <cctype>
#include <fstream>
#include <variant>
#include <cstdint>
#include <stdexcept>
#include <exception>
#include <boost/multiprecision/cpp_int.hpp>
#include <optional>
#include <array>
#include <tuple>
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

typedef uint64_t uuid;
typedef uint8_t  byte;

#define NEXT_MULTIPLE_OF_K(_Len, _Align)  ( (_Len + _Align - 1) / _Align * _Align )
#define type_aligned( t ) ( NEXT_MULTIPLE_OF_K(sizeof(t), alignof(t)) )
#define NEXT_MULTIPLE_OF_8(n)  (((n) + 7) & ~7)

constexpr uint64_t GENESIS_FIRST_SLOT  = 0;

constexpr uint64_t MaxCoin             = 23058430092136939ULL;
constexpr uint64_t OneCoin             = 1000000000ULL;
constexpr uint64_t OneTenth            = 100000000ULL;
constexpr uint64_t OneHundredth        = 10000000ULL;
constexpr uint64_t OneThousandth       = 1000000ULL;
constexpr uint64_t NumDecimals         = 9ULL;

constexpr uint64_t StateShardCount = 160;
constexpr uint64_t ComputeUnitsCount = 32;
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
constexpr uint64_t StakeFee                    = OneThousandth;
constexpr uint64_t ValidatorPunishAmount       = OneTenth;
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
constexpr uint64_t CommitteeSize               = 128;
constexpr uint64_t MinStakingDuration          = SlotsPerEpoch * 675; // 3 days

// Execution Related
constexpr uint64_t KeccakFee                   = 32;

#define EmptySignature              bytes(96, 0)
#define EMPTY_BYTES                 bytes(32, 0)

// FNV constants
constexpr uint32_t FNV_PRIME = 0x01000193;
constexpr uint32_t FNV_OFFSET_BASIS = 0x811C9DC5;
#define InitialDagSeed "0815ba0efb7f9c5dec9e22012f5b92005c2847c13adf55a87bfee7974cad965ac69c8aba1acb89984d92695f2b1046733bdd599d1e062ff9baff792dc2a2c989"
template<typename T, typename U>
concept strong_typedef = !std::is_same_v<T, U> && std::is_convertible_v<T, U>;

template<typename T>
struct is_std_optional : std::false_type {};

template<typename U>
struct is_std_optional<std::optional<U>> : std::true_type {};

template<typename T>
inline constexpr bool is_std_optional_v = is_std_optional<T>::value;

template<typename T>
struct is_std_variant : std::false_type {};

template<typename... Ts>
struct is_std_variant<std::variant<Ts...>> : std::true_type {};

template<typename T>
inline constexpr bool is_std_variant_v = is_std_variant<T>::value;template <typename T>
struct is_std_vector : std::false_type {};

template <typename T, typename Alloc>
struct is_std_vector<std::vector<T, Alloc>> : std::true_type {};

template <typename T>
inline constexpr bool is_std_vector_v = is_std_vector<T>::value;template <typename T>
struct vector_element_type {};

template <typename T, typename Alloc>
struct vector_element_type<std::vector<T, Alloc>> {
    using type = T;
};

template <typename T>
using vector_element_t = typename vector_element_type<T>::type;

template <typename T>
struct is_const_bytes : std::false_type {};

template <std::size_t N>
struct is_const_bytes<const_bytes<N>> : std::true_type {};

template <typename T>
inline constexpr bool is_const_bytes_v = is_const_bytes<T>::value;

template <typename T>
struct is_std_array : std::false_type {};

template <typename T, std::size_t N>
struct is_std_array<std::array<T, N>> : std::true_type {};

// detect bitmask_array
template <typename T>
struct is_bitmask_array : std::false_type {};

template <typename T, std::size_t N>
struct is_bitmask_array<std::array<std::optional<T>, N>> : std::true_type {};

template <typename T>
inline constexpr bool is_std_array_v = is_std_array<T>::value;

template <typename T>
struct array_element_type {};

template <typename T, std::size_t N>
struct array_element_type<std::array<T, N>> {
    using type = T;
};

template <typename T, std::size_t N>
using bitmask_array = std::array<std::optional<T>, N>;

template <typename T>
struct array_size;  // primary template, undefined by default

template <typename U, std::size_t N>
struct array_size<std::array<U, N>> : std::integral_constant<std::size_t, N> {};

template <typename T>
inline constexpr std::size_t array_size_v = array_size<T>::value;

template<std::size_t N>
struct bitmask_t {
    static constexpr std::size_t num_bytes = (N + 7) / 8; // Round up to nearest byte
    std::array<uint8_t, num_bytes> data{};

    constexpr bitmask_t() noexcept = default;
    constexpr bitmask_t(const bytes& b) noexcept {
        std::size_t bytes_to_copy = (b.size() < num_bytes) ? b.size() : num_bytes;
        for (std::size_t i = 0; i < bytes_to_copy; ++i) {
            data[i] = b[i];
        }
    }

    constexpr void set(std::size_t bit) noexcept {
        data[bit / 8] |= (1u << (bit % 8));
    }

    constexpr void clear(std::size_t bit) noexcept {
        data[bit / 8] &= ~(1u << (bit % 8));
    }

    constexpr bool get(std::size_t bit) const noexcept {
        return data[bit / 8] & (1u << (bit % 8));
    }

    constexpr void reset() noexcept {
        for (auto& b : data)
            b = 0;
    }

    struct bit_ref {
        uint8_t& byte;
        uint8_t mask;

        constexpr operator bool() const noexcept {
            return byte & mask;
        }

        constexpr bit_ref& operator=(bool value) noexcept {
            if (value)
                byte |= mask;
            else
                byte &= ~mask;
            return *this;
        }

        // Allow assignment from another bit_ref
        constexpr bit_ref& operator=(const bit_ref& other) noexcept {
            return *this = static_cast<bool>(other);
        }
    };

    // operator[] for non-const objects (read/write)
    constexpr bit_ref operator[](std::size_t bit) noexcept {
        return bit_ref{ data[bit / 8], static_cast<uint8_t>(1u << (bit % 8)) };
    }

    // operator[] for const objects (read-only)
    constexpr bool operator[](std::size_t bit) const noexcept {
        return data[bit / 8] & (1u << (bit % 8));
    }

    constexpr bytes to_bytes() const noexcept {
        return bytes(data.data(), num_bytes);
    }

    constexpr operator bytes() const noexcept {
        return to_bytes();
    }
};

template<typename T>
struct is_bitmask : std::false_type {};

template<std::size_t N>
struct is_bitmask<bitmask_t<N>> : std::true_type {};

template<typename T>
inline constexpr bool is_bitmask_v = is_bitmask<T>::value;


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

uint256_t ToU256(const bytes& B);

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
#define EVEN_NIBBLE_ZERO_BYTES 0x04
#define ODD_NIBBLE_ZERO_BYTES 0x05

bytes BytesToNibbles(const bytes& Bytes, size_t *IsEncodedLeaf=nullptr);
bytes NibblesFromBytes(const bytes& Bytes, bool IsEncoded);
bytes NibblesToBytes(const bytes& Nibbles, bool IsEmpty, bool IsZero, bool IsEncoded);

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
uint32_t ToBigEndian16(uint32_t Num);
uint32_t ToBigEndian32(uint32_t Num);
uint64_t ToBigEndian(uint64_t Num);
bytes ToBytes(uint64_t Value);
void ToBytes(int256_t Value, uint8_t* Bytes);
void ToBytes(uint256_t Value, uint8_t* Bytes);
const_bytes<32> U256ToBytes(uint256_t Value);
uint64_t FromBytes(const bytes& b);
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

bytes EncodeVarBytes(const bytes& Bytes);
bytes DecodeVarBytes(SafeStream* Stream);

uint64_t FromDecimal(const std::string& Decimal);
bytes MerkleRoot(const std::vector<bytes> &leaves);
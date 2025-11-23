#pragma once
#include <string>
#include <vector>
#include <sstream>
#include <iomanip>
#include <cstring>

/*
    The 'bytes' class follows the lowercase naming convention used by the C++ Standard Template Library (STL).
    This is intentional — just like std::vector, std::string, and std::map — to make the class feel like a 
    natural language-level container rather than a user-defined object.

    The STL was designed to look and behave as an *extension of the C++ language itself*, which is why its 
    containers and algorithms use lowercase names and concise interfaces. Following that convention, 'bytes' 
    acts as a lightweight wrapper around std::vector<uint8_t>, providing a string-like and vector-like interface 
    for binary data manipulation while maintaining idiomatic C++ naming consistency.
*/

constexpr const char* NULL_STRING = "";
typedef uint8_t  byte;

class bytes;
struct bytes_hash {
    std::size_t operator()(const bytes& v) const noexcept;
};


class bytes {
public:
    using hash_type = bytes_hash;
    using value_type = uint8_t;
    using iterator = std::vector<uint8_t>::iterator;
    using const_iterator = std::vector<uint8_t>::const_iterator;

    bytes() = default;

    bytes(size_t size, uint8_t byte);
    bytes(std::string_view str);
    bytes(const char* cstr);
    bytes(const std::string& str);
    bytes(int8_t* bytes, size_t length);
    bytes(uint8_t* bytes, size_t length);
    bytes(const int8_t* bytes, size_t length);
    bytes(const uint8_t* bytes, size_t length);
    bytes(const std::vector<int8_t>& vec);
    bytes(const std::vector<uint8_t>& vec);
    bytes(const std::initializer_list<uint8_t>& list);
    bytes(const iterator& start, const iterator& end);
    bytes(const const_iterator& start, const const_iterator& end);

    iterator begin();
    iterator end();
    const_iterator begin() const;
    const_iterator end() const;

    const_iterator cbegin() const;
    const_iterator cend() const;

    size_t size() const;
    bool empty() const;
    uint8_t* data();
    const uint8_t* data() const;

    std::string to_string() const;
    std::vector<uint8_t> to_vector() const;

    uint8_t& operator[](size_t index);
    const uint8_t& operator[](size_t index) const;

    bool operator==(const bytes& other) const;
    bool operator!=(const bytes& other) const;

    bytes sub(size_t pos, size_t count = std::string::npos) const;
    
    bool operator<(const bytes& other) const;
    bool operator<(const std::string& str) const;
    bool operator<(const char* cstr) const;
    bool operator<(const std::vector<uint8_t>& vec) const;

    bytes operator+(const bytes& other) const;
    bytes operator+(const std::string& str) const;
    bytes operator+(const char* cstr) const;
    bytes operator+(const std::vector<uint8_t>& vec) const;

    friend bytes operator+(const std::string& str, const bytes& b);
    friend bytes operator+(const char* cstr, const bytes& b);

    bytes& operator+=(const bytes& other);
    bytes& operator+=(const std::string& str);
    bytes& operator+=(const char* cstr);
    bytes& operator+=(const std::vector<uint8_t>& vec);

    std::string hex() const;
    static bytes from_hex(const std::string& hex_str);

    void align(size_t alignment);
    std::vector<bytes> split(size_t chunk_size) const;

    void resize(size_t size, uint8_t byte = 0);
    void reserve(size_t size);
    void push_back(uint8_t byte);
    void append(const bytes& other);
    void clear();

    bool startswith(const bytes& prefix) const;
    bool startswith(const std::vector<uint8_t>& prefix) const;
    bool startswith(const std::string& prefix) const;
    bool startswith(const char* prefix) const;

    bool is_zero() const;


    // encoding and decoding
    size_t capacity() const;
    size_t tell() const;
    size_t remaining() const;
    void seek(size_t position);

    void encode_uint16(uint16_t value);
    void encode_uint32(uint32_t value);
    void encode_uint64(uint64_t value);
    void encode_varint(uint64_t value);
    void encode_varbytes(const bytes& data);

    uint16_t decode_uint16();
    uint32_t decode_uint32();
    uint64_t decode_uint64();
    uint64_t decode_varint();
    byte decode_byte();
    bytes decode_bytes(size_t length);
    bytes decode_varbytes();

    void encode_nibbles(const bytes& nibbles, bool encodePrefix, bool isEmpty, bool isZero);
    bytes decode_nibbles(size_t* info = nullptr);
    void decode_nibbles(bytes& out, size_t* info = nullptr);
private:
    std::vector<uint8_t> data_;
    size_t pos_;
};

template <std::size_t NUM_BYTES>
class const_bytes {
public:
    static constexpr std::size_t size_value = NUM_BYTES;  // compile-time accessible constant

    const_bytes(const bytes& b = bytes(NUM_BYTES, 0)) : inner_(b) {
        if (b.size() != NUM_BYTES)
            throw std::invalid_argument("const_bytes: size mismatch");
    }

    bytes& operator*() { return inner_; }
    const bytes& operator*() const { return inner_; }
    const bytes* operator->() const { return &inner_; }

    static constexpr std::size_t size_static() { return NUM_BYTES; }
    std::size_t size() const { return inner_.size(); }
    const uint8_t* data() const { return inner_.data(); }
    std::string hex() const { return inner_.hex(); }

    bool operator==(const const_bytes& other) const { return inner_ == *other; }
    bool operator!=(const const_bytes& other) const { return !(*this == other); }

    operator bytes() const { return inner_; }
private:
    bytes inner_;
};

typedef bytes var_bytes;

bytes u16_to_bytes(uint16_t value);
bytes v61_to_bytes(uint64_t value);
bytes from_hex(const bytes& str);

// template<typename T>
// T lton(T value) {
//     if constexpr (std::endian::native == std::endian::little) {
//         if constexpr (sizeof(T) == 2) return (value >> 8) | (value << 8);
//         else if constexpr (sizeof(T) == 4) return __builtin_bswap32(value); // GCC/Clang
//         else if constexpr (sizeof(T) == 8) return __builtin_bswap64(value);
//     }
//     return value;
// }

// template<typename T>
// T ntol(T value) {
//     return lton(value);
// }
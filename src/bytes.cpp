#include "bytes.h"
#include <bit>
#include <algorithm>
#include <stdexcept>


#define EVEN_NIBBLE_EMPTY 0x00
#define ODD_NIBBLE_EMPTY 0x01
#define EVEN_NIBBLE 0x02
#define ODD_NIBBLE 0x03
#define EVEN_NIBBLE_ZERO_BYTES 0x04
#define ODD_NIBBLE_ZERO_BYTES 0x05

std::size_t bytes_hash::operator()(const bytes& v) const noexcept {
    std::size_t hash = 0;
    for (auto b : v) {
        hash ^= std::hash<uint8_t>{}(b) + 0x9e3779b9 + (hash << 6) + (hash >> 2);
    }
    return hash;
}

// Constructors
bytes::bytes(size_t size, uint8_t byte)
    : data_(size, byte), pos_(0) {}

bytes::bytes(std::string_view str)
    : data_(str.begin(), str.end()), pos_(0) {}

bytes::bytes(const char* cstr) : pos_(0) {
    const char* safe_cstr = NULL_STRING;
    if (cstr != nullptr) safe_cstr = cstr;
    data_ = std::vector<uint8_t>(reinterpret_cast<const uint8_t*>(safe_cstr), 
                                  reinterpret_cast<const uint8_t*>(safe_cstr) + std::strlen(safe_cstr));
}

bytes::bytes(const std::string& str) 
    : data_(str.begin(), str.end()), pos_(0) {}

bytes::bytes(int8_t* bytes, size_t length)
    : data_(reinterpret_cast<uint8_t*>(bytes), reinterpret_cast<uint8_t*>(bytes) + length), pos_(0) {}

bytes::bytes(uint8_t* bytes, size_t length)
    : data_(bytes, bytes + length) {}

bytes::bytes(const int8_t* bytes, size_t length)
    : data_(reinterpret_cast<const uint8_t*>(bytes), reinterpret_cast<const uint8_t*>(bytes) + length), pos_(0) {}

bytes::bytes(const uint8_t* bytes, size_t length)
    : data_(bytes, bytes + length), pos_(0) {}

bytes::bytes(const std::vector<int8_t>& vec)
    : data_(vec.begin(), vec.end()), pos_(0) {}

bytes::bytes(const std::vector<uint8_t>& vec)
    : data_(vec), pos_(0) {}

bytes::bytes(const std::initializer_list<uint8_t>& list)
    : data_(list), pos_(0) {}

bytes::bytes(const iterator& start, const iterator& end)
    : data_(start, end), pos_(0) {}

bytes::bytes(const const_iterator& start, const const_iterator& end)
    : data_(start, end), pos_(0) {}

// Iterators
bytes::iterator bytes::begin() { 
    return data_.begin(); 
}

bytes::iterator bytes::end() { 
    return data_.end(); 
}

bytes::const_iterator bytes::begin() const { 
    return data_.begin(); 
}

bytes::const_iterator bytes::end() const { 
    return data_.end(); 
}

bytes::const_iterator bytes::cbegin() const { 
    return data_.cbegin(); 
}

bytes::const_iterator bytes::cend() const { 
    return data_.cend(); 
}

// Size and data access
size_t bytes::size() const { 
    return data_.size(); 
}

bool bytes::empty() const { 
    return data_.empty(); 
}

uint8_t* bytes::data() { 
    return data_.data(); 
}

const uint8_t* bytes::data() const { 
    return data_.data(); 
}

// Conversion methods
std::string bytes::to_string() const { 
    return std::string(data_.begin(), data_.end()); 
}

std::vector<uint8_t> bytes::to_vector() const { 
    return data_; 
}

// Operators
uint8_t& bytes::operator[](size_t index) { 
    return data_[index]; 
}

const uint8_t& bytes::operator[](size_t index) const { 
    return data_[index]; 
}

bool bytes::operator==(const bytes& other) const { 
    return data_ == other.data_; 
}

bool bytes::operator!=(const bytes& other) const { 
    return data_ != other.data_; 
}

bytes bytes::sub(size_t pos, size_t count) const {
    if (pos > data_.size())
        throw std::out_of_range("bytes::sub: position out of range");

    size_t end_pos = (count == std::string::npos || pos + count > data_.size())
        ? data_.size()
        : pos + count;

    return bytes(std::vector<uint8_t>(data_.begin() + pos, data_.begin() + end_pos));
}

bool bytes::operator<(const bytes& other) const {
    return std::lexicographical_compare(
        data_.begin(), data_.end(),
        other.data_.begin(), other.data_.end()
    );
}

bool bytes::operator<(const std::string& str) const {
    return std::lexicographical_compare(
        data_.begin(), data_.end(),
        str.begin(), str.end()
    );
}

bool bytes::operator<(const char* cstr) const {
    return std::lexicographical_compare(
        data_.begin(), data_.end(),
        cstr, cstr + std::strlen(cstr)
    );
}

bool bytes::operator<(const std::vector<uint8_t>& vec) const {
    return std::lexicographical_compare(
        data_.begin(), data_.end(),
        vec.begin(), vec.end()
    );
}

bytes bytes::operator+(const bytes& other) const {
    bytes result = *this;
    result.data_.insert(result.data_.end(), other.data_.begin(), other.data_.end());
    return result;
}

bytes bytes::operator+(const std::string& str) const {
    bytes result = *this;
    result.data_.insert(result.data_.end(), str.begin(), str.end());
    return result;
}

bytes bytes::operator+(const char* cstr) const {
    bytes result = *this;
    result.data_.insert(result.data_.end(), cstr, cstr + std::strlen(cstr));
    return result;
}

bytes bytes::operator+(const std::vector<uint8_t>& vec) const {
    bytes result = *this;
    result.data_.insert(result.data_.end(), vec.begin(), vec.end());
    return result;
}

bytes operator+(const std::string& str, const bytes& b) {
    bytes result(str);
    result.data_.insert(result.data_.end(), b.data_.begin(), b.data_.end());
    return result;
}

bytes operator+(const char* cstr, const bytes& b) {
    bytes result(cstr);
    result.data_.insert(result.data_.end(), b.data_.begin(), b.data_.end());
    return result;
}

bytes& bytes::operator+=(const bytes& other) {
    data_.insert(data_.end(), other.data_.begin(), other.data_.end());
    return *this;
}

bytes& bytes::operator+=(const std::string& str) {
    data_.insert(data_.end(), str.begin(), str.end());
    return *this;
}

bytes& bytes::operator+=(const char* cstr) {
    data_.insert(data_.end(), cstr, cstr + std::strlen(cstr));
    return *this;
}

bytes& bytes::operator+=(const std::vector<uint8_t>& vec) {
    data_.insert(data_.end(), vec.begin(), vec.end());
    return *this;
}

// Hex conversion
std::string bytes::hex() const {
    std::ostringstream oss;
    oss << std::hex << std::setfill('0');
    for (auto b : data_)
        oss << std::setw(2) << static_cast<int>(b);
    return oss.str();
}

bytes bytes::from_hex(const std::string& hex_str) {
    if (hex_str.size() % 2 != 0)
        throw std::invalid_argument("bytes::from_hex: hex string must have even length");

    std::vector<uint8_t> vec;
    vec.reserve(hex_str.size() / 2);

    for (size_t i = 0; i < hex_str.size(); i += 2) {
        char high = std::tolower(hex_str[i]);
        char low = std::tolower(hex_str[i + 1]);

        auto hex_to_val = [](char c) -> uint8_t {
            if (c >= '0' && c <= '9') return c - '0';
            if (c >= 'a' && c <= 'f') return c - 'a' + 10;
            throw std::invalid_argument("bytes::from_hex: invalid hex character");
        };

        uint8_t byte = (hex_to_val(high) << 4) | hex_to_val(low);
        vec.push_back(byte);
    }

    return bytes(vec);
}

// Utility methods
void bytes::align(size_t alignment) {
    data_.resize(((size() + alignment - 1) / alignment * alignment));
}

std::vector<bytes> bytes::split(size_t chunk_size) const {
    if (chunk_size == 0) return std::vector<bytes>({*this});

    std::vector<bytes> result;
    size_t total = data_.size();

    for (size_t i = 0; i < total; i += chunk_size) {
        size_t current_chunk_size = std::min(chunk_size, total - i);
        result.emplace_back(sub(i, current_chunk_size));
    }

    return result;
}

void bytes::resize(size_t size, uint8_t byte) {
    data_.resize(size, byte);
}

void bytes::reserve(size_t size) {
    data_.reserve(size);
}

void bytes::push_back(uint8_t byte) {
    data_.push_back(byte);
}

void bytes::append(const bytes& other) {
    data_.insert(data_.end(), other.begin(), other.end());
}

void bytes::clear() {
    data_.clear();
}

// String matching
bool bytes::startswith(const bytes& prefix) const {
    if (prefix.size() > data_.size()) return false;
    return std::equal(prefix.begin(), prefix.end(), data_.begin());
}

bool bytes::startswith(const std::vector<uint8_t>& prefix) const {
    if (prefix.size() > data_.size()) return false;
    return std::equal(prefix.begin(), prefix.end(), data_.begin());
}

bool bytes::startswith(const std::string& prefix) const {
    if (prefix.size() > data_.size()) return false;
    return std::equal(prefix.begin(), prefix.end(), data_.begin());
}

bool bytes::startswith(const char* prefix) const {
    size_t prefix_len = std::strlen(prefix);
    if (prefix_len > data_.size()) return false;
    return std::equal(prefix, prefix + prefix_len, data_.begin());
}

bool bytes::is_zero() const {
    for (auto b : data_) {
        if (b != 0) return false;
    }
    return true;
}

size_t bytes::capacity() const
{
    return data_.capacity();
}

size_t bytes::tell() const
{
    return pos_;
}

size_t bytes::remaining() const
{
    return data_.size() - pos_;
}

void bytes::seek(size_t position)
{
    if (position > data_.size())
        throw std::out_of_range("bytes::seek: position out of range");
    pos_ = position;
}

void bytes::encode_uint16(uint16_t value)
{
    if constexpr (std::endian::native == std::endian::little) value = std::byteswap(value);
    data_.push_back(static_cast<uint8_t>(value >> 8));
    data_.push_back(static_cast<uint8_t>(value & 0xFF));
}

void bytes::encode_uint32(uint32_t value)
{
    if constexpr (std::endian::native == std::endian::little) value = std::byteswap(value);
    data_.push_back(static_cast<uint8_t>(value >> 24));
    data_.push_back(static_cast<uint8_t>((value >> 16) & 0xFF));
    data_.push_back(static_cast<uint8_t>((value >> 8) & 0xFF));
    data_.push_back(static_cast<uint8_t>(value & 0xFF));
}

void bytes::encode_uint64(uint64_t value)
{
    if constexpr (std::endian::native == std::endian::little) value = std::byteswap(value);
    data_.push_back(static_cast<uint8_t>(value >> 56));
    data_.push_back(static_cast<uint8_t>((value >> 48) & 0xFF));
    data_.push_back(static_cast<uint8_t>((value >> 40) & 0xFF));
    data_.push_back(static_cast<uint8_t>((value >> 32) & 0xFF));
    data_.push_back(static_cast<uint8_t>((value >> 24) & 0xFF));
    data_.push_back(static_cast<uint8_t>((value >> 16) & 0xFF));
    data_.push_back(static_cast<uint8_t>((value >> 8) & 0xFF));
    data_.push_back(static_cast<uint8_t>(value & 0xFF));
}

void bytes::encode_varint(uint64_t value)
{
    if (value >= (1ULL << 61)) {
        throw std::runtime_error("Value exceeds maximum of 2^61 - 1");
    }

    // Determine the number of bytes needed
    uint8_t byteLength = 0;
    if (value < (1ULL << 5)) {          // 5 bits
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

    // Convert to big-endian
    uint64_t bigEndianValue = value;
    if constexpr (std::endian::native == std::endian::little) {
        bigEndianValue = std::byteswap(value);
    }

    // Extract bytes from the big-endian representation
    const uint8_t* bytes_ptr = reinterpret_cast<const uint8_t*>(&bigEndianValue);
    
    // Create first byte with length prefix and top 5 bits of value
    uint8_t firstByte = ((byteLength - 1) << 5) | (bytes_ptr[8 - byteLength] & 0x1F);
    data_.push_back(firstByte);
    
    // Append remaining bytes
    for (uint8_t i = 1; i < byteLength; ++i) {
        data_.push_back(bytes_ptr[8 - byteLength + i]);
    }
}

uint16_t bytes::decode_uint16()
{
    if (pos_ + 2 > data_.size())
        throw std::out_of_range("bytes::decode_uint16: insufficient data");
    
    uint16_t value = (static_cast<uint16_t>(data_[pos_]) << 8) |
                     static_cast<uint16_t>(data_[pos_ + 1]);
    pos_ += 2;
    
    if constexpr (std::endian::native == std::endian::little)
        value = std::byteswap(value);
    
    return value;
}

uint32_t bytes::decode_uint32()
{
    if (pos_ + 4 > data_.size())
        throw std::out_of_range("bytes::decode_uint32: insufficient data");
    
    uint32_t value = (static_cast<uint32_t>(data_[pos_]) << 24) |
                     (static_cast<uint32_t>(data_[pos_ + 1]) << 16) |
                     (static_cast<uint32_t>(data_[pos_ + 2]) << 8) |
                     static_cast<uint32_t>(data_[pos_ + 3]);
    pos_ += 4;
    
    if constexpr (std::endian::native == std::endian::little)
        value = std::byteswap(value);
    
    return value;
}

uint64_t bytes::decode_uint64()
{
    if (pos_ + 8 > data_.size())
        throw std::out_of_range("bytes::decode_uint64: insufficient data");
    
    uint64_t value = (static_cast<uint64_t>(data_[pos_]) << 56) |
                     (static_cast<uint64_t>(data_[pos_ + 1]) << 48) |
                     (static_cast<uint64_t>(data_[pos_ + 2]) << 40) |
                     (static_cast<uint64_t>(data_[pos_ + 3]) << 32) |
                     (static_cast<uint64_t>(data_[pos_ + 4]) << 24) |
                     (static_cast<uint64_t>(data_[pos_ + 5]) << 16) |
                     (static_cast<uint64_t>(data_[pos_ + 6]) << 8) |
                     static_cast<uint64_t>(data_[pos_ + 7]);
    pos_ += 8;
    
    if constexpr (std::endian::native == std::endian::little)
        value = std::byteswap(value);
    
    return value;
}

uint64_t bytes::decode_varint()
{
    if ((pos_ + 1) >= data_.size())
        throw std::out_of_range("bytes::decode_varint: insufficient data");
    
    // Read first byte to determine length
    uint8_t firstByte = data_[pos_++];
    uint8_t byteLength = ((firstByte >> 5) & 0x07) + 1;
    
    if (pos_ + byteLength - 1 > data_.size())
        throw std::out_of_range("bytes::decode_varint: insufficient data for varint");
    
    // Build a big-endian uint64_t representation
    uint64_t bigEndianValue = 0;
    uint8_t* bytes_ptr = reinterpret_cast<uint8_t*>(&bigEndianValue);
    
    // Set the top 5 bits from first byte
    bytes_ptr[8 - byteLength] = firstByte & 0x1F;
    
    // Copy remaining bytes
    for (uint8_t i = 1; i < byteLength; ++i) {
        bytes_ptr[8 - byteLength + i] = data_[pos_++];
    }
    
    // Convert from big-endian to native
    uint64_t value = bigEndianValue;
    if constexpr (std::endian::native == std::endian::little) {
        value = std::byteswap(bigEndianValue);
    }
    
    return value;
}

bytes bytes::decode_bytes(size_t length)
{
    if (pos_ + length > data_.size())
        throw std::out_of_range("bytes::decode_bytes: insufficient data");
    
    bytes result = sub(pos_, length);
    pos_ += length;
    return result;
}

void bytes::encode_nibbles(const bytes& nibbles, bool encodePrefix, bool isEmpty, bool isZero)
{
    size_t totalNibbles = nibbles.size();
    
    if (encodePrefix) {
        totalNibbles += (nibbles.size() % 2 == 0) ? 2 : 1;
    }
    
    // Encode the total packed byte count
    encode_varint(totalNibbles / 2);
    
    // Write prefix nibbles as packed bytes
    if (encodePrefix) {
        if (nibbles.size() % 2 == 0) {
            // Even: write prefix byte with padding
            unsigned char prefix = isEmpty ? EVEN_NIBBLE_EMPTY : (isZero ? EVEN_NIBBLE_ZERO_BYTES : EVEN_NIBBLE);
            unsigned char High = (prefix & 0x0F) << 4;
            unsigned char Low = 0x00;
            push_back(High | Low);
        } else {
            // Odd: pack prefix with first data nibble
            unsigned char prefix = isEmpty ? ODD_NIBBLE_EMPTY : (isZero ? ODD_NIBBLE_ZERO_BYTES : ODD_NIBBLE);
            unsigned char High = (prefix & 0x0F) << 4;
            unsigned char Low = nibbles[0] & 0x0F;
            push_back(High | Low);
        }
    }
    
    // Pack remaining data nibbles
    size_t startIdx = encodePrefix && (nibbles.size() % 2 == 1) ? 1 : 0;
    
    for (size_t i = startIdx; i < nibbles.size(); i += 2) {
        unsigned char High = (nibbles[i] & 0x0F) << 4;
        unsigned char Low = (i + 1 < nibbles.size()) ? (nibbles[i + 1] & 0x0F) : 0x00;
        push_back(High | Low);
    }
}

#include <iostream>
bytes bytes::decode_nibbles(size_t *info)
{
    bytes nibbles;
    decode_nibbles(nibbles, info);

    return nibbles;
}

void bytes::decode_nibbles(bytes &nibbles, size_t *info)
{
    uint64_t amount = decode_varint();
    if (pos_ + amount > data_.size())
        throw std::out_of_range("bytes::decode_nibbles: insufficient data for nibbles");
    
    nibbles.reserve(amount * 2);

    for (size_t i = 0; i < amount; i++) {
        uint8_t c = data_[pos_ + i];
        nibbles.push_back((c >> 4) & 0x0F);
        nibbles.push_back(c & 0x0F);
    }

    if (info && !nibbles.empty()) {
        if (nibbles[0] == ODD_NIBBLE_ZERO_BYTES) {
            nibbles = nibbles.sub(1);
            *info = 2;
        } else if (nibbles[0] == EVEN_NIBBLE_ZERO_BYTES) {
            nibbles = nibbles.sub(2);
            *info = 2;
        } else if (nibbles[0] == ODD_NIBBLE) {
            nibbles = nibbles.sub(1);
            *info = 1;
        } else if (nibbles[0] == EVEN_NIBBLE) {
            nibbles = nibbles.sub(2);
            *info = 1;
        } else if (nibbles[0] == ODD_NIBBLE_EMPTY) {
            nibbles = nibbles.sub(1);
            *info = 0;
        } else if (nibbles[0] == EVEN_NIBBLE_EMPTY) {
            nibbles = nibbles.sub(2);
            *info = 0;
        }
    }

    pos_ += amount;
}

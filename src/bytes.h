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

class bytes {
public:
    using value_type = uint8_t;
    using iterator = std::vector<uint8_t>::iterator;
    using const_iterator = std::vector<uint8_t>::const_iterator;

    bytes() = default;

    bytes(size_t size, uint8_t byte)
        : data_(size, byte) {}

    bytes(const char* cstr)
    {
        const char* safe_cstr = NULL_STRING;
        if (cstr != nullptr) safe_cstr = cstr;
        data_ = std::vector<uint8_t>(reinterpret_cast<const uint8_t*>(safe_cstr), reinterpret_cast<const uint8_t*>(safe_cstr) + std::strlen(safe_cstr));
    }

    bytes(const std::string& str) 
        : data_(str.begin(), str.end()) {}

    bytes(int8_t* bytes, size_t length)
        : data_(reinterpret_cast<uint8_t*>(bytes), reinterpret_cast<uint8_t*>(bytes) + length) {}

    bytes(uint8_t* bytes, size_t length)
        : data_(bytes, bytes + length) {}

    bytes(const int8_t* bytes, size_t length)
        : data_(reinterpret_cast<const uint8_t*>(bytes), reinterpret_cast<const uint8_t*>(bytes) + length) {}

    bytes(const uint8_t* bytes, size_t length)
        : data_(bytes, bytes + length) {}

    bytes(const std::vector<int8_t>& vec)
        : data_(vec.begin(), vec.end()) {}

    bytes(const std::vector<uint8_t>& vec)
        : data_(vec) {}

    bytes(const std::initializer_list<uint8_t>& list)
        : data_(list) {}

    bytes(const iterator& start, const iterator& end)
        : data_(start, end) {}

    bytes(const const_iterator& start, const const_iterator& end)
        : data_(start, end) {}

    iterator begin() { return data_.begin(); }
    iterator end() { return data_.end(); }
    const_iterator begin() const { return data_.begin(); }
    const_iterator end() const { return data_.end(); }

    const_iterator cbegin() const { return data_.cbegin(); }
    const_iterator cend() const { return data_.cend(); }

    size_t size() const { return data_.size(); }
    bool empty() const { return data_.empty(); }
    uint8_t* data() { return data_.data(); }
    const uint8_t* data() const { return data_.data(); }

    std::string to_string() const { return std::string(data_.begin(), data_.end()); }
    std::vector<uint8_t> to_vector() const { return data_; }

    uint8_t& operator[](size_t index) { return data_[index]; }
    const uint8_t& operator[](size_t index) const { return data_[index]; }

    bool operator==(const bytes& other) const { return data_ == other.data_; }
    bool operator!=(const bytes& other) const { return data_ != other.data_; }

    bytes sub(size_t pos, size_t count = std::string::npos) const {
        if (pos > data_.size())
            throw std::out_of_range("bytes::sub: position out of range");

        size_t end_pos = (count == std::string::npos || pos + count > data_.size())
            ? data_.size()
            : pos + count;

        return bytes(std::vector<uint8_t>(data_.begin() + pos, data_.begin() + end_pos));
    }
    
    bool operator<(const bytes& other) const {
        return std::lexicographical_compare(
            data_.begin(), data_.end(),
            other.data_.begin(), other.data_.end()
        );
    }

    bool operator<(const std::string& str) const {
        return std::lexicographical_compare(
            data_.begin(), data_.end(),
            str.begin(), str.end()
        );
    }

    bool operator<(const char* cstr) const {
        return std::lexicographical_compare(
            data_.begin(), data_.end(),
            cstr, cstr + std::strlen(cstr)
        );
    }

    bool operator<(const std::vector<uint8_t>& vec) const {
        return std::lexicographical_compare(
            data_.begin(), data_.end(),
            vec.begin(), vec.end()
        );
    }

    bytes operator+(const bytes& other) const {
        bytes result = *this;
        result.data_.insert(result.data_.end(), other.data_.begin(), other.data_.end());
        return result;
    }

    bytes operator+(const std::string& str) const {
        bytes result = *this;
        result.data_.insert(result.data_.end(), str.begin(), str.end());
        return result;
    }

    bytes operator+(const char* cstr) const {
        bytes result = *this;
        result.data_.insert(result.data_.end(), cstr, cstr + std::strlen(cstr));
        return result;
    }

    bytes operator+(const std::vector<uint8_t>& vec) const {
        bytes result = *this;
        result.data_.insert(result.data_.end(), vec.begin(), vec.end());
        return result;
    }

    friend bytes operator+(const std::string& str, const bytes& b) {
        bytes result(str);
        result.data_.insert(result.data_.end(), b.data_.begin(), b.data_.end());
        return result;
    }

    friend bytes operator+(const char* cstr, const bytes& b) {
        bytes result(cstr);
        result.data_.insert(result.data_.end(), b.data_.begin(), b.data_.end());
        return result;
    }

    bytes& operator+=(const bytes& other) {
        data_.insert(data_.end(), other.data_.begin(), other.data_.end());
        return *this;
    }

    bytes& operator+=(const std::string& str) {
        data_.insert(data_.end(), str.begin(), str.end());
        return *this;
    }

    bytes& operator+=(const char* cstr) {
        data_.insert(data_.end(), cstr, cstr + std::strlen(cstr));
        return *this;
    }

    bytes& operator+=(const std::vector<uint8_t>& vec) {
        data_.insert(data_.end(), vec.begin(), vec.end());
        return *this;
    }

    std::string hex() const {
        std::ostringstream oss;
        oss << std::hex << std::setfill('0');
        for (auto b : data_)
            oss << std::setw(2) << static_cast<int>(b);
        return oss.str();
    }

    static bytes from_hex(const std::string& hex_str) {
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

    void align(size_t alignment) {
        data_.resize(( (size() + alignment - 1) / alignment * alignment ));
    }
    
    std::vector<bytes> split(size_t chunk_size) const {
        if (chunk_size == 0) return std::vector<bytes>({*this});

        std::vector<bytes> result;
        size_t total = data_.size();

        for (size_t i = 0; i < total; i += chunk_size) {
            size_t current_chunk_size = std::min(chunk_size, total - i);
            result.emplace_back(sub(i, current_chunk_size));
        }

        return result;
    }

    void resize(size_t size, uint8_t byte = 0) {
        data_.resize(size, byte);
    }

    void reserve(size_t size) {
        data_.reserve(size);
    }

    void push_back(uint8_t byte) {
        data_.push_back(byte);
    }

    void append(const bytes& other) {
        data_.insert(data_.end(), other.begin(), other.end());
    }

    void clear() {
        data_.clear();
    }

    bool startswith(const bytes& prefix) const {
        if (prefix.size() > data_.size()) return false;
        return std::equal(prefix.begin(), prefix.end(), data_.begin());
    }

    bool startswith(const std::vector<uint8_t>& prefix) const {
        if (prefix.size() > data_.size()) return false;
        return std::equal(prefix.begin(), prefix.end(), data_.begin());
    }

    bool startswith(const std::string& prefix) const {
        if (prefix.size() > data_.size()) return false;
        return std::equal(prefix.begin(), prefix.end(), data_.begin());
    }

    bool startswith(const char* prefix) const {
        size_t prefix_len = std::strlen(prefix);
        if (prefix_len > data_.size()) return false;
        return std::equal(prefix, prefix + prefix_len, data_.begin());
    }

    bool is_zero() const {
        for (auto b : data_) {
            if (b != 0) return false;
        }
        return true;
    }
private:
    std::vector<uint8_t> data_;
};

template <std::size_t NUM_BYTES>
class const_bytes {
public:
    static constexpr std::size_t size_value = NUM_BYTES;  // compile-time accessible constant

    const_bytes(const bytes& b = bytes(NUM_BYTES, 0)) : inner_(b) {
        if (b.size() != NUM_BYTES)
            throw std::invalid_argument("const_bytes: size mismatch");
    }

    // explicit const_bytes(const std::array<uint8_t, NUM_BYTES>& arr)
    //     : inner_(arr.data(), arr.size()) {}

    // explicit const_bytes(const uint8_t* data)
    //     : inner_(data, NUM_BYTES) {}

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
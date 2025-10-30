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

class bytes {
public:
    using value_type = uint8_t;
    using iterator = std::vector<uint8_t>::iterator;
    using const_iterator = std::vector<uint8_t>::const_iterator;

    bytes() = default;

    bytes(const char* cstr) 
        : data_(reinterpret_cast<const uint8_t*>(cstr), reinterpret_cast<const uint8_t*>(cstr) + std::strlen(cstr)) {}

    bytes(const std::string& str) 
        : data_(str.begin(), str.end()) {}

    bytes(int8_t* bytes, size_t length)
        : data_(reinterpret_cast<uint8_t*>(bytes), reinterpret_cast<uint8_t*>(bytes) + length) {}

    bytes(uint8_t* bytes, size_t length)
        : data_(bytes, bytes + length) {}

    bytes(const std::vector<int8_t>& vec)
        : data_(vec.begin(), vec.end()) {}

    bytes(const std::vector<uint8_t>& vec)
        : data_(vec) {}

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

    void resize(size_t size) {
        data_.resize(size);
    }
private:
    std::vector<uint8_t> data_;
};
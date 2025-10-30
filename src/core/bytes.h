#pragma once
#include <string>
#include <vector>

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
private:
    std::vector<uint8_t> data_;
};
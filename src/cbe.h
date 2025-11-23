#pragma once
#include "util.h"
#include "bytes.h"

template<typename T>
bytes encode(const T& obj);
template<typename T>
void encode(bytes& data, const T& obj);
template<typename T>
T decode(bytes& stream);
template<typename T>
T decode(const bytes& data);

template<typename T>
struct Decoder {
    static T decode(bytes& stream) {
        return decode<T>(stream);
    }
};

template<typename T>
struct Decoder<std::optional<T>> {
    static std::optional<T> decode(bytes& stream) {
        byte has_value = stream.decode_byte();
        if (has_value) return Decoder<T>::decode(stream);
        return std::nullopt;
    }
};

template<size_t N>
struct Decoder<const_bytes<N>> {
    static const_bytes<N> decode(bytes& stream) {
        bytes data = stream.decode_bytes(N);
        return const_bytes<N>(data);
    }
};

template<typename... Ts>
struct Decoder<std::variant<Ts...>> {
    static std::variant<Ts...> decode(bytes& stream) {
        byte index = stream.decode_byte(); // variant index
        std::variant<Ts...> result;

        // Helper lambda to dispatch based on index
        bool handled = false;
        size_t i = 0;
        ((i++ == index ? (result = Decoder<Ts>::decode(stream), handled = true, void(), 0) : 0), ...);

        if (!handled) {
            throw std::runtime_error("Invalid variant index while decoding");
        }
        return result;
    }
};
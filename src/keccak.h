#include <string>
#include "keccak-tiny.h"
#include "bytes.h"

bytes Keccak(const bytes& in);
void Keccak(const uint8_t* in, size_t length, uint8_t* out);
bytes Keccak512(const bytes& in);
void Keccak512(const uint8_t* in, size_t length, uint8_t* out);
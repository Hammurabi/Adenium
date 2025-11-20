#include "keccak.h"

/*
    Tested against Ethereum reference:
        https://ethereum.stackexchange.com/questions/550/which-cryptographic-hash-function-does-ethereum-use

    Keccak("testing") ->
        5f16f4c7f149ac4f9510d9cf8cf384038ad348b3bcdc01915f95de12df9d1b02
*/

bytes Keccak(const bytes& in)
{
    uint8_t o[32];
    sha3_256(o, 32, reinterpret_cast<const uint8_t*>(in.data()), in.size());
    return bytes(o, 32);
}

void Keccak(const uint8_t *in, size_t length, uint8_t *out)
{
    sha3_256(out, 32, in, length);
}

bytes Keccak512(const bytes& in)
{
    uint8_t o[64];
    sha3_512(o, 64, reinterpret_cast<const uint8_t*>(in.data()), in.size());
    return bytes(o, 64);
}

void Keccak512(const uint8_t *in, size_t length, uint8_t *out)
{
    sha3_512(out, 64, in, length);
}
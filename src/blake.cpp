#include "blake.h"
#include <blake3.h>

bytes blake3(const bytes &input)
{
    bytes hash(32, 0);
    blake3_hasher hasher;
    blake3_hasher_init(&hasher);
    blake3_hasher_update(&hasher, input.data(), input.size());
    blake3_hasher_finalize(&hasher, hash.data(), hash.size());
    return hash;
}
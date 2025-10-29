from blspy import PrivateKey, AugSchemeMPL

REGISTER_ACCOUNT = b'\x01'
REGISTER_ASSET = b'\x02'
REGISTER_DEPLOYMENT = b'\x03'
TRANSFER_VALUE = b'\x04'
TRANSFER_ASSET = b'\x05'
EXECUTE = b'\x06'
STAKE = b'\x07'
UNSTAKE = b'\x08'
ATOMIC_SWAP = b'\x09'
TRANSFER_ACCOUNT = b'\x0A'
ATTESTATION = b'\x0B'


def encode_varint(value: int) -> bytes:
    if value >= (1 << 61):
        raise ValueError("Value exceeds maximum of 2^61 - 1")
    
    # Determine byte length
    if value < (1 << 5):
        byte_length = 1
    elif value < (1 << 13):
        byte_length = 2
    elif value < (1 << 21):
        byte_length = 3
    elif value < (1 << 29):
        byte_length = 4
    elif value < (1 << 37):
        byte_length = 5
    elif value < (1 << 45):
        byte_length = 6
    elif value < (1 << 53):
        byte_length = 7
    else:
        byte_length = 8
    
    # Convert to big-endian bytes
    big_endian_bytes = value.to_bytes(8, 'big')
    
    # First byte: high 3 bits = size, low 5 bits = first part of value
    first_byte = ((byte_length - 1) << 5) | (big_endian_bytes[8 - byte_length] & 0x1F)
    result = bytes([first_byte]) + big_endian_bytes[9 - byte_length:]
    return result

def encode_varbytes(b: bytes) -> bytes:
    return encode_varint(len(b)) + b

# Transaction functions
def register_account_transaction(sender: int, nonce: int, recipient: bytes, amount: int) -> bytes:
    if len(recipient) != 20:
        return b''
    return REGISTER_ACCOUNT + encode_varint(sender) + encode_varint(nonce) + recipient + encode_varint(amount)

def register_asset_transaction(sender: int, nonce: int, name: bytes, unit_name: bytes, decimals: int, total_supply: int, manager: int) -> bytes:
    return (REGISTER_ASSET +
            encode_varint(sender) +
            encode_varint(nonce) +
            encode_varbytes(name) +
            encode_varbytes(unit_name) +
            encode_varint(decimals) +
            encode_varint(total_supply) +
            encode_varint(manager))

def register_deployment_transaction(sender: int, nonce: int, amount: int, code: bytes, exc_code: bytes) -> bytes:
    return (REGISTER_DEPLOYMENT +
            encode_varint(sender) +
            encode_varint(nonce) +
            encode_varint(amount) +
            encode_varbytes(code) +
            encode_varbytes(exc_code))

def transfer_value_transaction(sender: int, recipient: int, amount: int, nonce: int) -> bytes:
    return (TRANSFER_VALUE +
            encode_varint(sender) +
            encode_varint(nonce) +
            encode_varint(recipient) +
            encode_varint(amount))

def transfer_asset_transaction(sender: int, recipient: int, uuid: int, amount: int, nonce: int) -> bytes:
    return (TRANSFER_ASSET +
            encode_varint(sender) +
            encode_varint(nonce) +
            encode_varint(recipient) +
            encode_varint(uuid) +
            encode_varint(amount))

def execute_code_transaction(sender: int, nonce: int, amount: int, gas_limit: int, deployment: bytes, exc_code: bytes) -> bytes:
    return (EXECUTE +
            encode_varint(sender) +
            encode_varint(nonce) +
            encode_varint(amount) +
            encode_varint(gas_limit) +
            deployment +
            encode_varbytes(exc_code))

def stake_transaction(sender: int, stake_amount: int, nonce: int) -> bytes:
    return STAKE + encode_varint(sender) + encode_varint(nonce) + encode_varint(stake_amount)

def unstake_transaction(sender: int, nonce: int) -> bytes:
    return UNSTAKE + encode_varint(sender) + encode_varint(nonce)

def atomic_swap(sender: int, nonce: int, recipient: int, token: int, amount: int, time: int, secret: bytes) -> bytes:
    return (ATOMIC_SWAP +
            encode_varint(sender) +
            encode_varint(nonce) +
            encode_varint(recipient) +
            encode_varint(token) +
            encode_varint(amount) +
            encode_varint(time) +
            encode_varbytes(secret))

def transfer_account(sender: int, nonce: int, recipient: bytes) -> bytes:
    return TRANSFER_ACCOUNT + encode_varint(sender) + encode_varint(nonce) + recipient

def attest_block(sender: int, uuid: int, slot: int, validator_index: int, block: bytes) -> bytes:
    return (ATTESTATION +
            encode_varint(uuid) +
            encode_varint(slot) +
            encode_varint(sender) +
            encode_varint(validator_index) +
            block)

def sign_transaction(transaction: bytes, private_key: PrivateKey) -> bytes:
    """
    Signs a transaction with a BLS private key using AugSchemeMPL.
    
    Args:
        transaction (bytes): The transaction data.
        private_key (PrivateKey): A blspy PrivateKey object.
    
    Returns:
        bytes: The serialized signature concatenated with the transaction.
    """
    signature = AugSchemeMPL.sign(private_key, transaction)
    serialized_sig = bytes(signature)
    return serialized_sig + transaction
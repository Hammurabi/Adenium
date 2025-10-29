import os
import base64
import json
import mnemonic
import dearpygui.dearpygui as dpg
from Crypto.Cipher import AES
from Crypto.Protocol.KDF import PBKDF2
from blspy import PrivateKey, AugSchemeMPL

# =====================================================
# Configuration
# =====================================================
APP_WIDTH, APP_HEIGHT = 900, 600
cwd = os.getcwd()
WALLET_FILE = os.path.join(cwd, 'wallet', "data.json")

# =====================================================
# Encryption / Decryption helpers (AES-GCM)
# =====================================================
def encrypt_data(data: bytes, password: str) -> bytes:
    salt = os.urandom(16)
    key = PBKDF2(password, salt, dkLen=32, count=100000)
    cipher = AES.new(key, AES.MODE_GCM)
    ciphertext, tag = cipher.encrypt_and_digest(data)
    return base64.b64encode(salt + cipher.nonce + tag + ciphertext).decode()

def decrypt_data(data: bytes, password: str) -> bytes:
    decoded = base64.b64decode(data)
    salt, nonce, tag, ciphertext = decoded[:16], decoded[16:32], decoded[32:48], decoded[48:]
    key = PBKDF2(password, salt, dkLen=32, count=100000)
    cipher = AES.new(key, AES.MODE_GCM, nonce=nonce)
    return cipher.decrypt_and_verify(ciphertext, tag)

# =====================================================
# Wallet functions
# =====================================================
def create_wallet(seed, password: str):
    sk = AugSchemeMPL.key_gen(seed)
    pk = sk.get_g1()
    
    # Convert to bytes
    sk_bytes = bytes(sk)
    pk_bytes = bytes(pk)  # public key can still use .serialize()

    wallet_data = {
        "sk": encrypt_data(sk_bytes, password),
        "pk": pk_bytes.hex()
    }

    with open(WALLET_FILE, "w") as f:
        json.dump(wallet_data, f, indent=4)
    return wallet_data

def wallet_exists():
    return os.path.exists(WALLET_FILE)

def load_wallet(password: str):
    with open(WALLET_FILE, "r") as f:
        data = json.load(f)
    data['sk'] = PrivateKey.from_bytes(decrypt_data(data['sk'], password))
    return data

# def load_wallet():
#     with open(WALLET_FILE, "r") as f:
#         data = json.load(f)
#     decrypted = decrypt_data(data, password)
#     return json.loads(decrypted.decode())

# def load_or_create():
#     # load or generate key
#     if os.path.exists(WALLET_FILE):
#         with open(WALLET_FILE, 'r') as f:
#             wallet = json.load(f)
#     else:
#         with open(WALLET_FILE, 'w') as f:
#             json.dump({
#             }, f)
#         # if verbose:
#         print("[+] Generated new node key:", pub_bytes.hex(), flush=True)

# =====================================================
# GUI Callbacks
# =====================================================
def unlock_wallet(sender, app_data, user_data):
    password = dpg.get_value("password_input")
    try:
        wallet = load_wallet(password)
        dpg.configure_item("unlock_window", show=False)
        dpg.configure_item("wallet_window", show=True)
        dpg.set_value("pubkey_text", wallet["pk"])
        dpg.set_item_user_data("wallet_window", wallet)
    except Exception as e:
        dpg.set_value("status_text_unlock", f"Incorrect password or corrupted wallet")
        print(e.with_traceback())


def copy_pubkey(sender, app_data, user_data):
    wallet = dpg.get_item_user_data("wallet_window")
    dpg.set_clipboard_text(wallet["pk"])


# =====================================================
# Transaction-related windows
# =====================================================

def back_to_wallet(from_window):
    dpg.configure_item(from_window, show=False)
    dpg.configure_item("wallet_window", show=True)


# ---------- Send Transaction Window ----------
def send_transaction(sender, app_data, user_data):
    dpg.configure_item("wallet_window", show=False)
    dpg.configure_item("send_tx_window", show=True)

# ---------- Register Account Window ----------
def register_account(sender, app_data, user_data):
    dpg.configure_item("wallet_window", show=False)
    dpg.configure_item("register_account_window", show=True)

# ---------- Register Code Window ----------
def register_code(sender, app_data, user_data):
    dpg.configure_item("wallet_window", show=False)
    dpg.configure_item("register_code_window", show=True)

# ---------- Send Code Window ----------
def send_code(sender, app_data, user_data):
    dpg.configure_item("wallet_window", show=False)
    dpg.configure_item("send_code_window", show=True)

# =====================================================
# GUI Setup
# =====================================================
dpg.create_context()

# =====================================================
# Create Wallet Callbacks
# =====================================================
def create_wallet_gui(sender, app_data, user_data):
    password = dpg.get_value("create_password_input")
    if not password:
        dpg.set_value("status_text_create", "Password cannot be empty")
        return
    mnemonic_value = dpg.get_value("mnemonic_text")
    seed = mnemo.to_seed(mnemonic_value, passphrase="")
    
    wallet = create_wallet(seed[:32], password)
    dpg.configure_item("create_window", show=False)
    dpg.configure_item("wallet_window", show=True)
    dpg.set_value("pubkey_text", wallet["pk"])
    dpg.set_item_user_data("wallet_window", wallet)
    dpg.set_value("status_text_wallet", "✅ Wallet created successfully")
    dpg.configure_item("create_window", show=True)


# =====================================================
# Recover Wallet Callback
# =====================================================
def recover_wallet_gui(sender, app_data, user_data):
    mnemonic_value = dpg.get_value("recover_mnemonic_input")
    password = dpg.get_value("recover_password_input")

    if not mnemonic_value or not password:
        dpg.set_value("status_text_recover", "Please fill in all fields")
        return

    try:
        if mnemo.check(mnemonic_value):
            seed = mnemo.to_seed(mnemonic_value, passphrase="")
            wallet = create_wallet(seed[:32], password)

            dpg.configure_item("recover_window", show=False)
            dpg.configure_item("wallet_window", show=True)
            dpg.set_value("pubkey_text", wallet["pk"])
            dpg.set_item_user_data("wallet_window", wallet)
            dpg.set_value("status_text_wallet", "Wallet recovered successfully")
        else:
            dpg.set_value("status_text_recover", "Invalid mnemonic phrase")
    except Exception as e:
        dpg.set_value("status_text_recover", f"Invalid mnemonic or error: {e}")

# =====================================================
# Modified Unlock Wallet Window (centered layout)
# =====================================================
with dpg.window(
    label="Unlock Wallet",
    tag="unlock_window",
    width=APP_WIDTH,
    height=APP_HEIGHT,
    pos=(0, 0),
    no_close=True,
    no_move=True,
    no_resize=True,
    no_collapse=True,
    show=False
):
    # Title
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH - 200) // 2)
        dpg.add_text("Enter Password:")

    # Password input
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH - 450) // 2)
        dpg.add_input_text(tag="password_input", password=True, width=400)

    # Buttons
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH - 450) // 2)
        dpg.add_button(label="Unlock", callback=unlock_wallet, width=196)
        dpg.add_button(label="Recover", callback=lambda: (
            dpg.configure_item("unlock_window", show=False),
            dpg.configure_item("recover_window", show=True)
        ), width=196)

    # Status text
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH - 500) // 2)
        dpg.add_text("", tag="status_text_unlock", color=(255, 0, 0))

# =====================================================
# Recover Wallet Window (new)
# =====================================================
with dpg.window(
    label="Recover Wallet",
    tag="recover_window",
    width=APP_WIDTH,
    height=APP_HEIGHT,
    pos=(0, 0),
    no_close=True,
    no_move=True,
    no_resize=True,
    no_collapse=True,
    show=False
):
    # --- Instruction text ---
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH - 730) // 2)
        dpg.add_text("Enter your mnemonic phrase below to restore access to your wallet.", wrap=880, color=(200, 80, 35))

    dpg.add_separator()

    # --- Mnemonic input ---
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH - 830) // 2)
        dpg.add_input_text(tag="recover_mnemonic_input", multiline=True, width=800, height=120)

    # --- Password input ---
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH - 250) // 2)
        dpg.add_text("Choose a new password:")

    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH - 430) // 2)
        dpg.add_input_text(tag="recover_password_input", password=True, width=400)

    # --- Recover button ---
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH - 430) // 2)
        dpg.add_button(label="Recover Wallet", callback=recover_wallet_gui, width=196)
        dpg.add_button(label="Back", width=196, callback=lambda: (
            dpg.configure_item("recover_window", show=False),
            dpg.configure_item("unlock_window", show=True)
        ))

    # --- Status text ---
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH - 300) // 2)
        dpg.add_text("", tag="status_text_recover", color=(255, 0, 0))


# Create wallet window (doesn’t exist)
with dpg.window(
    label="Create Wallet",
    tag="create_window",
    width=APP_WIDTH,
    height=APP_HEIGHT,
    pos=(0,0),
    no_close=True,
    no_move=True,
    no_resize=True,
    no_collapse=True,
    show=False
):
    # --- Title ---
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH-300)//2)  # center the next item
        dpg.add_text("Create a new wallet:")

    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH-880)//2)
        dpg.add_text("Please back up your mnemonic phrase securely. It is the only way to recover your wallet if you lose access.", wrap=880, color=(200, 80, 35))

    # --- Mnemonic display box ---
    mnemo = mnemonic.Mnemonic("english")
    mnemonic_phrase = mnemo.generate(strength=256)

    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH-300)//2)
        dpg.add_text("Your mnemonic phrase:")
    
    dpg.add_separator()
    
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH-880)//2)
        dpg.add_text(mnemonic_phrase, wrap=880, color=(200, 180, 35), tag="mnemonic_text")
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH - 300) // 2)
        dpg.add_button(label="Copy", callback=lambda: dpg.set_clipboard_text(mnemonic_phrase), width=200)

    dpg.add_separator()

    # --- Password input ---
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH-300)//2)
        dpg.add_text("Type in a password:")

    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH-500)//2)
        dpg.add_input_text(tag="create_password_input", password=True, width=400)

    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH-300)//2)
        dpg.add_button(label="Create Wallet", callback=create_wallet_gui, width=200)

    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH-500)//2)
        dpg.add_text("", tag="status_text_create", color=(255,0,0))

if wallet_exists():
    dpg.configure_item("unlock_window", show=True)
else:
    dpg.configure_item("create_window", show=True)
    
# Wallet window (fixed position, unmovable)
with dpg.window(
    label="Adenium Wallet", 
    tag="wallet_window", 
    width=APP_WIDTH, 
    height=APP_HEIGHT, 
    show=False, 
    no_close=True,
    no_move=True,
    no_resize=True,
    no_collapse=True
):

    # Calculate centered width
    center_x = (APP_WIDTH - 600) // 2  # since input box width = 600

    dpg.add_spacer(height=40)
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=center_x)
        dpg.add_text("Adenium Wallet", bullet=True)

    dpg.add_spacer(height=20)
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=center_x)
        dpg.add_text("Public Key:")

    with dpg.group(horizontal=True):
        dpg.add_spacer(width=center_x)
        dpg.add_input_text(tag="pubkey_text", width=600, readonly=True)

    dpg.add_spacer(height=10)
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=center_x)
        dpg.add_button(label="Copy Public Key", callback=copy_pubkey, width=200)

    dpg.add_separator()
    dpg.add_spacer(height=10)

    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH-430)//2)
        dpg.add_button(label="Send Transaction", callback=send_transaction, width=196)
        dpg.add_button(label="Register Account", callback=register_account, width=196)

    with dpg.group(horizontal=True):
        dpg.add_spacer(width=(APP_WIDTH-430)//2)
        dpg.add_button(label="Register Code", callback=register_code, width=196)
        dpg.add_button(label="Send Code", callback=send_code, width=196)

    dpg.add_spacer(height=10)
    with dpg.group(horizontal=True):
        dpg.add_spacer(width=center_x)
        dpg.add_text("", tag="status_text_wallet")

with dpg.window(
    label="Send Transaction",
    tag="send_tx_window",
    width=APP_WIDTH,
    height=APP_HEIGHT,
    pos=(0, 0),
    show=False,
    no_close=True,
    no_move=True,
    no_resize=True,
):
    dpg.add_text("Send Transaction", bullet=True)
    dpg.add_input_text(label="Recipient (ID)", width=200, tag="tx_recipient")
    dpg.add_input_text(label="Asset ID (Optional)", width=200, tag="tx_assetid")
    dpg.add_input_text(label="Amount", width=200, tag="tx_amount")
    dpg.add_button(label="Send", width=200, callback=lambda: dpg.set_value("status_text_wallet", "Transaction sent (mock)."))
    dpg.add_button(label="Back", width=200, callback=lambda: back_to_wallet("send_tx_window"))


with dpg.window(
    label="Register Account",
    tag="register_account_window",
    width=APP_WIDTH,
    height=APP_HEIGHT,
    pos=(0, 0),
    show=False,
    no_close=True,
    no_move=True,
    no_resize=True,
):
    dpg.add_text("Register Account", bullet=True)
    dpg.add_input_text(label="Recipient (Public Key)", width=400, tag="reg_acc_key")
    dpg.add_input_text(label="Amount", width=200, tag="reg_acc_amount")
    dpg.add_button(label="Register", width=200, callback=lambda: dpg.set_value("status_text_wallet", "Account registered (mock)."))
    dpg.add_button(label="Back", width=200, callback=lambda: back_to_wallet("register_account_window"))


with dpg.window(
    label="Register Code",
    tag="register_code_window",
    width=APP_WIDTH,
    height=APP_HEIGHT,
    pos=(0, 0),
    show=False,
    no_close=True,
    no_move=True,
    no_resize=True,
):
    dpg.add_text("Register Code", bullet=True)
    dpg.add_input_text(label="Code", width=800, multiline=True, tag="reg_code_input")
    dpg.add_input_text(label="Amount", width=200, tag="reg_code_amount")
    dpg.add_input_text(label="Max Gas", width=200, tag="reg_code_maxgas")
    dpg.add_button(label="Register", width=200, callback=lambda: dpg.set_value("status_text_wallet", "Code registered (mock)."))
    dpg.add_button(label="Back", width=200, callback=lambda: back_to_wallet("register_code_window"))


with dpg.window(
    label="Send Code",
    tag="send_code_window",
    width=APP_WIDTH,
    height=APP_HEIGHT,
    pos=(0, 0),
    show=False,
    no_close=True,
    no_move=True,
    no_resize=True,
):
    dpg.add_text("Send Code", bullet=True)
    dpg.add_input_text(label="Code Hash", width=600, tag="send_code_hash")
    dpg.add_input_text(label="Amount", width=200, tag="send_code_amount")
    dpg.add_input_text(label="Max Gas", width=200, tag="send_code_maxgas")
    dpg.add_button(label="Send", width=200, callback=lambda: dpg.set_value("status_text_wallet", "Code sent (mock)."))
    dpg.add_button(label="Back", width=200, callback=lambda: back_to_wallet("send_code_window"))


# =====================================================
# Center window and set always-on-top
# =====================================================
viewport_w = 1920
viewport_h = 1080
x = int((viewport_w - APP_WIDTH) / 2)
y = int((viewport_h - APP_HEIGHT) / 2)

dpg.create_viewport(title="Adenium Wallet", width=APP_WIDTH, height=APP_HEIGHT, x_pos=x, y_pos=y, always_on_top=True, resizable=False)
dpg.set_global_font_scale(1.5)
dpg.setup_dearpygui()
dpg.show_viewport()
dpg.start_dearpygui()
dpg.destroy_context()

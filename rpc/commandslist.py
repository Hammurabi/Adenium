# import requests to make our lives easier
import requests
# import socket to do IP address checks
import util
# import commands
import commands
import cmd_node
import cmd_quit
import cmd_close
import cmd_createwallet
import cmd_encryptwallet
import cmd_dumpwallet
import cmd_walletpassphrasechange
import cmd_walletpassphrase
import cmd_setnoncewallet
import cmd_loadwallet
import cmd_getaccount
import cmd_walletfromdump
import cmd_signtransaction
import cmd_broadcasttransaction
import cmd_gettransaction
import cmd_broadcastblock
import cmd_createnextblock
import cmd_submitnonce

# registers all commands to the command manager
def register_all(cmdManager):
    # this command sets the node ip and port
    cmdManager.register('setnode', cmd_node.parse)
    # this command exists the rpc client but does not affect the server
    cmdManager.register('quit', cmd_quit.parse)
    # this command exists the rpc client and sends a 'terminate' signal to the server
    cmdManager.register('close', cmd_close.parse)

    # wallet commands

    # this command generates a new wallet (name, (optional)encrypt)
    cmdManager.register('createwallet', cmd_createwallet.parse)
    # this command encrypts wallet (name)
    cmdManager.register('encryptwallet', cmd_encryptwallet.parse)
    # this command prints the raw wallet data (name)
    # Note: if the wallet is encrypted, then it's safe
    # to copy the json dump around as the private key
    # will be encrypted.
    cmdManager.register('dumpwallet', cmd_dumpwallet.parse)
    # this command changes the encryption password of a wallet (name)
    cmdManager.register('walletpassphrasechange', cmd_walletpassphrasechange.parse)
    # this command sets the encryption password of the loaded wallet (timeout)
    cmdManager.register('walletpassphrase', cmd_walletpassphrase.parse)
    # this command changes the saved nonce in the wallet (nonce)
    cmdManager.register('setnoncewallet', cmd_setnoncewallet.parse)
    # this command loads a wallet from file to memory (name)
    cmdManager.register('loadwallet', cmd_loadwallet.parse)
    # this command returns the account associated with an address (address)
    cmdManager.register('getaccount', cmd_getaccount.parse)
    # this command creates a wallet from dump (dump)
    cmdManager.register('walletfromdump', cmd_walletfromdump.parse)
    # this command signs a transaction using the loaded wallet (transaction)
    cmdManager.register('signtransaction', cmd_signtransaction.parse)
    # this command broadcasts a transaction to the network (transaction)
    cmdManager.register('broadcasttransaction', cmd_broadcasttransaction.parse)
    # this command retrieves a transaction using it's hash (txid)
    cmdManager.register('gettransaction', cmd_gettransaction.parse)

    # mining commands
    # this command broadcasts a block to the network (block)
    cmdManager.register('broadcastblock', cmd_broadcastblock.parse)
    # this command generates the next block and sends the header back for mining (address)
    cmdManager.register('createnextblock', cmd_createnextblock.parse)
    # this command submits the nonce to the server and checks for valid proof of work then broadcasts the block (nonce)
    cmdManager.register('submitnonce', cmd_submitnonce.parse)


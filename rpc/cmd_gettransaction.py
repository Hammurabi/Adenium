# import commands
import commands
# password helper
import json
from getpass import getpass

# define 'gettransaction' command
def parse(cmd, arguments, connection):
    if len(arguments) != 2:
        print("error: '"+cmd.name+"' requires one argument.")
    else:
        transaction = arguments[1]

        response, tx    = connection.send_request(cmd.name, {'txid':transaction})
        print("alert: server responded with '"+response.response+"'.")
        if response.response == 'failed':
            print("reason: " + response.reason)
        else:
            print("---------------------------------")
            print(tx)
            print("---------------------------------")


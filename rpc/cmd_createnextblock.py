# import commands
import commands
# password helper
import json
from getpass import getpass

# define 'createnextblock' command
def parse(cmd, arguments, connection):
    if len(arguments) != 1:
        print("error: '"+cmd.name+"' does not require any arguments.")
    else:
        response, header    = connection.send_request(cmd.name, {})
        print("alert: server responded with '"+response.response+"'.")
        if response.response == 'failed':
            print("reason: " + response.reason)
        else:
            print("---------------------------------")
            print(header)
            print("---------------------------------")


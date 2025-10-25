# import commands
import commands
# password helper
from getpass import getpass

# define 'broadcastblock' command
def parse(cmd, arguments, connection):
    if len(arguments) != 2:
        print("error: '"+cmd.name+"' requires one argument.")
    else:
        transaction = arguments[1]

        response    = connection.post_request(cmd.name, {})
        print("alert: server responded with '"+response.response+"'.")
        if response.response == 'failed':
            print("reason: " + response.reason)


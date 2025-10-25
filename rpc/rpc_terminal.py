# import socket to do IP address checks
import util
# import commands
import commands
# import commands
import commandslist
# import connection
import rpc_connection

# valid commands
def start():
    # setup the initial connection
    connection = rpc_connection.rpc_connection()
    # create a command manager
    cmdManager = commands.CommandsManager()
    # register all our commands
    commandslist.register_all(cmdManager)
    # enter an infinite loop
    while (True):
        # scan the command line for commands
        text        = input(">")
        # this shouldn't happen
        if not text:
            continue
        # split the command
        arguments   = text.split(" ")
        # parse the command
        cmdManager.parse(arguments, connection)

# define 'exit' command
def exit_parse(command, arguments):
    print("alert: terminating process")
    quit()
# define 'quit' command
def quit_parse(command, arguments):
    print("alert: terminating process")
    requests.get(ip + port + "/quit")
    quit()
# define 'getblock' command
def getblock_parse(command, arguments):
    # check correct amount of arguments exists
    if len(arguments < 2):
        print("error: 'getblock' command requires a minimum of two arguments.")
        pass

    # check that the block-id is base16 encoded
    if util.is_base16_encoded(arguments[1]):
        print("error: 'getblock' command requires second argument to be base16 encoded.")
        pass

    # getblock <hash>
    # getblock <hash> includeTx txAsHash includeEv evAsHash format
    if len(arguments) > 2 and len(arguments) < 7:
        print("error: 'getblock' command missing arguments.")
        pass
    
# define 'base16' characters
base16   = ['0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f']

# define 'base58' characters
base58   = ['1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G',
            'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q',
            'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
            'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g',
            'h', 'i', 'j', 'k', 'm', 'n', 'o', 'p',
            'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
            'y', 'z']

# start the loop
start()
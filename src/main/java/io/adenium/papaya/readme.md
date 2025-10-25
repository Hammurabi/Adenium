# Papaya Language Specification
## Papaya Archive
A papaya archive is a collection of compiled classes, structs, contracts, and modules.
They can be used as a Transacton payload to deploy or invoke contracts.

Many techniques are used in order to ensure the small footprint of a Papaya archive.

## Structures
## struct
Structs in Papaya are treated as data containers and should be used as such, they have no initializer-blocks therefore default-initialization is illegal without a constructor, a static-initialization block is called once during class-loading, there are no default utility functions in structs, members are not restricted to the 'public' access modifier.
## class
Classes in Papaya contain a default-initializer block, a static-initializer block, and default utility functions. Too many instances of a class should not be created during tight-loops.
## contract
Contracts are the interface between a decentralized application and a programmer, they are similar to classes with one exception, the 'new' keyword would create a new 'smart contract' instance in the ledger but not in memory.
Contracts have their own set of utility-functions which are unique to them.

# Contract Creation Transactions
Papaya contracts can be deployed on the Adenium chain via a "DeployContractTransaction",
the transaction must contain the Papaya archive, and a "main" entry point to instantiate
the contract.
# Contract Invocation Transactions
Papaya contracts can be interacted with on the Adenium chain via an "InvokeTransaction",
these types of transactions can only call functions declared in a contract.

# Immutability
There are multiple types of contracts, by default, contracts are mutable, but they can be made immutable using the keyword of the same name, token and NFT interfaces are immutable by default, this is to prevent malicious behaviour by developers after deploying the contract.
# Use-cases
Smart contracts can be used to create derivatives, tokens, and other types of assets, they can also be used to interact with the global Adenium ledger, making them very useful as a cheap alternative to databases, financial institutions spend billions of dollars annually on databases, a high percentage of the cost comes from securing these databases against cyber-attacks, and natural disasters. Smart contracts can be the solution to these problems, as the information is stored in an immutable ledger on the blockchain, institutions could deploy their applications to the network and be able to run their own Adenium nodes for a fraction of the cost.
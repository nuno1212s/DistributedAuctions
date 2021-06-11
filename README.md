# DistributedAuctions

A Block chain implementation (With Proof of stake proposal) with a full kademlia implementation supporting Broadcasting, individual message sending, resistance to sybil attacks and eclipse through trust mechanisms and CRC requests for node validation.

## Current State

At the moment the chain is setup to start by connecting to a boostrap node that is currently offline so it won't work. Setup a base boostrap node, configure the IP and then add new nodes. They will auto connect and 4 seconds later they will publish a test transaction.
This transaction is only valid on the genesis block so it should only work for the first block.

There is currently no UI, this is just a base implementation with unit testing for validating the correctness of the implementation.

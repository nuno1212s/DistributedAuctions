package me.evmanu.p2p.nodeoperations;

import me.evmanu.p2p.kademlia.NodeTriple;

public interface StoreOperationBase extends Operation{

    public void handleSuccessfulStore(NodeTriple triple);

    public void handleFailedStore(NodeTriple triple);
}

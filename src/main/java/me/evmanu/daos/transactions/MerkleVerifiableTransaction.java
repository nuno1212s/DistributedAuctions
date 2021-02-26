package me.evmanu.daos.transactions;

import java.util.List;

public class MerkleVerifiableTransaction extends Transaction{


    //TODO: Implement this class as well
    private List<byte[]> merkleNodes;

    public MerkleVerifiableTransaction(int blockNumber, short version,
                                       ScriptSignature[] inputs, ScriptPubKey[] outputs) {
        super(blockNumber, version, inputs, outputs);
    }
}

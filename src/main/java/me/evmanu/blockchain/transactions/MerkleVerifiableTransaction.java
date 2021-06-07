package me.evmanu.blockchain.transactions;

import java.util.List;

public class MerkleVerifiableTransaction extends Transaction {


    //TODO: Implement this class as well
    private List<byte[]> merkleNodes;

    public MerkleVerifiableTransaction(short version,
                                       TransactionType type,
                                       ScriptSignature[] inputs, ScriptPubKey[] outputs) {
        super(version, type, inputs, outputs);
    }
}

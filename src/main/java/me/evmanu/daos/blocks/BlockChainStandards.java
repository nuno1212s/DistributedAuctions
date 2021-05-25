package me.evmanu.daos.blocks;

import java.util.concurrent.TimeUnit;

public class BlockChainStandards {

    //TODO: Mara, usa esta variável para ver os zeros que são precisos
    public static final int ZEROS_REQUIRED = 1;

    public static final int MAX_FORK_DISTANCE = 4;

    /**
     * Every EPOCH_SIZE time, generate a new Proof of stake block
     * <p>
     * There are 10 possible signers, which can all sign the block. In case more than one of them
     * Sign the block, we use their position on the list as a tie breaker (So the first name on the list
     * Will have the priority to sign the block).
     * <p>
     * If none of the signers actually sign the block, we are stuck in a very difficult spot,
     * But for the sake of this assignment we assume that at least one of the signers will sign it
     * <p>
     * If we wanted to make sure that this situation cannot happen / if it happens, it gets handled properly,
     * We can just accept signatures from people not on this list EPOCH_SIZE time after the block was supposed to be
     * minted, if it was not.
     */
    public static final float STAKE_AMOUNT = 25f;
    public static final long EPOCH_SIZE = TimeUnit.MINUTES.toMillis(5);
    public static final int ALLOWED_SIGNERS = 10;

}

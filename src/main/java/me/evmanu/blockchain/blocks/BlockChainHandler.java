package me.evmanu.blockchain.blocks;

import lombok.Getter;
import me.evmanu.blockchain.blocks.blockchains.BlockChainComparator;

import java.util.*;

public class BlockChainHandler {

    private final static BlockChainComparator comparator = new BlockChainComparator();

    private final List<BlockChain> blockChains = Collections.synchronizedList(new LinkedList<>());

    @Getter
    private final TransactionPool transactionPool;

    public BlockChainHandler() {
        this.transactionPool = new TransactionPool();
    }

    public Optional<BlockChain> getBestCurrentChain() {
        if (blockChains.isEmpty()) return Optional.empty();

        return Optional.of(this.blockChains.get(0));
    }

    protected Optional<BlockChain> getBlockChainForBlock(Block block) {

        synchronized (this.blockChains) {
            for (BlockChain blockChain : this.blockChains) {

                //If the latest block is not the previous block to the received block
                //then it cannot be part of this chain
                if (blockChain.getBlockCount() < block.getHeader().getBlockNumber() - 1) {
                    //This block is larger than
                    continue;
                }

            /*
            Get the previous block to this block
             */
                Block chainBlock = blockChain.getBlockByNumber(block.getHeader().getBlockNumber() - 1);

                if (Arrays.equals(chainBlock.getHeader().getBlockHash(),
                        block.getHeader().getPreviousBlockHash())) {
                    return Optional.of(blockChain);
                }
            }
        }

        return Optional.empty();
    }

    public boolean addBlockToChainAndUpdate(Block block) {

        Optional<BlockChain> chainForBlock = getBlockChainForBlock(block);

        if (chainForBlock.isPresent()) {

            Optional<BlockChain> forked = chainForBlock.get().addBlock(block);

            forked.ifPresent(this.blockChains::add);

            synchronized (this.blockChains) {

                this.blockChains.sort(comparator);

            }

            return true;
        }

        return false;
    }

    public Optional<Block> getBlockByPreviousHashAndBlockNumber(long blockNum, byte[] prevHash) {

        synchronized (this.blockChains) {
            for (BlockChain blockChain : this.blockChains) {
                if (blockChain.getBlockCount() > blockNum) {

                    Block block = blockChain.getBlockByNumber(blockNum);

                    if (Arrays.equals(block.getHeader().getPreviousBlockHash(), prevHash)) {
                        return Optional.of(block);
                    }
                }
            }
        }

        return Optional.empty();
    }

}

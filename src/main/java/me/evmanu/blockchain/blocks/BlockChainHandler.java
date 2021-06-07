package me.evmanu.blockchain.blocks;

import me.evmanu.blockchain.blocks.blockchains.BlockChainComparator;

import java.util.*;

public class BlockChainHandler {

    private final static BlockChainComparator comparator = new BlockChainComparator();

    private final List<BlockChain> blockChains = Collections.synchronizedList(new LinkedList<>());

    public BlockChainHandler() {

    }

    protected Optional<BlockChain> getBlockChainForBlock(Block block) {

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

        return Optional.empty();
    }

    protected boolean addBlockToChainAndUpdate(Block block) {

        synchronized (this.blockChains) {

            Optional<BlockChain> chainForBlock = getBlockChainForBlock(block);

            if (chainForBlock.isPresent()) {

                Optional<BlockChain> forked = chainForBlock.get().addBlock(block);

                forked.ifPresent(this.blockChains::add);

                this.blockChains.sort(comparator);

                return true;
            }
        }

        return false;
    }

}

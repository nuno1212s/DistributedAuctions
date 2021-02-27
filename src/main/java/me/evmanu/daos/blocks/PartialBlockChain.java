package me.evmanu.daos.blocks;

import java.util.List;

public class PartialBlockChain extends BlockChain {

    //Maybe implement this so that we don't have to store all the blocks since genesis in RAM?
    //TODO
    public PartialBlockChain(long blockCount, short version, List<Block> blocks) {
        super(blockCount, version, blocks);
    }
}

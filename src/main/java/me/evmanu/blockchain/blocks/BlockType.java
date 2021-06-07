package me.evmanu.blockchain.blocks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.evmanu.blockchain.blocks.blockchains.PoSBlock;
import me.evmanu.blockchain.blocks.blockchains.PoWBlock;

@AllArgsConstructor
@Getter
public enum BlockType {
    PROOF_OF_STAKE(PoSBlock.class),
    PROOF_OF_WORK(PoWBlock.class);

    private final Class<? extends Block> blockClass;
}

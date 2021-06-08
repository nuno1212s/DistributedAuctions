package me.evmanu.messages.adapters;

import com.google.gson.*;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.BlockType;
import me.evmanu.blockchain.transactions.Transaction;
import me.evmanu.messages.MessageStandards;
import me.evmanu.util.ByteWrapper;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

public class BlockAdapter implements JsonSerializer<Block>, JsonDeserializer<Block> {

    @Override
    public JsonElement serialize(Block src, Type typeOfSrc, JsonSerializationContext context) {

        JsonObject object = new JsonObject();

        object.addProperty(MessageStandards.BLOCK_TYPE, src.getBlockType().ordinal());

        object.add(MessageStandards.DATA_NAME, context.serialize(src, src.getClass()));

        return object;
    }

    @Override
    public Block deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject obj = (JsonObject) json;

        int blockType = obj.get(MessageStandards.BLOCK_TYPE).getAsInt();

        BlockType block = BlockType.values()[blockType];

        return context.deserialize(obj.get(MessageStandards.DATA_NAME), block.getBlockClass());
    }

    public JsonElement serializeTransactions(LinkedHashMap<ByteWrapper, Transaction> transactions,
                                             JsonSerializationContext context) {

        List<Transaction> transactionList = new LinkedList<>();

        transactions.forEach((id, t) -> {
            transactionList.add(t);
        });

        return context.serialize(transactionList);
    }

    public LinkedHashMap<ByteWrapper, Transaction> fromTransactions(List<Transaction> transactions) {
        return null;
    }

}

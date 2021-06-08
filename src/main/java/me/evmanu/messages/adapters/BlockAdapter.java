package me.evmanu.messages.adapters;

import com.google.gson.*;
import me.evmanu.blockchain.blocks.Block;
import me.evmanu.blockchain.blocks.BlockType;
import me.evmanu.messages.MessageStandards;

import java.lang.reflect.Type;

public class BlockAdapter implements JsonSerializer<Block>, JsonDeserializer<Block> {

    @Override
    public JsonElement serialize(Block src, Type typeOfSrc, JsonSerializationContext context) {

        //TODO: FIX THIS
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

}

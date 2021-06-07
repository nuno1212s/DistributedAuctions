package me.evmanu.messages.adapters;

import com.google.gson.*;
import me.evmanu.messages.Message;
import me.evmanu.messages.MessageStandards;

import java.lang.reflect.Type;

public class MessageAdapter implements JsonSerializer<Message>, JsonDeserializer<Message> {

    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        return null;
    }

    @Override
    public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {

        JsonObject object = new JsonObject();

        object.addProperty(MessageStandards.MESSAGE_TYPE_NAME, src.getType().ordinal());
        object.add(MessageStandards.DATA_NAME, context.serialize(src));

        return object;
    }
}

package me.evmanu.messages.adapters;

import com.google.gson.*;
import me.evmanu.messages.Message;
import me.evmanu.messages.MessageStandards;
import me.evmanu.messages.MessageType;

import java.lang.reflect.Type;

public class MessageAdapter implements JsonSerializer<Message>, JsonDeserializer<Message> {

    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

        JsonObject object = (JsonObject) json;

        int type = object.get(MessageStandards.MESSAGE_TYPE_NAME).getAsInt();

        var data = object.get(MessageStandards.DATA_NAME);

        MessageType value = MessageType.values()[type];

        return new Message(context.deserialize(data, value.getTypeClass()));
    }

    @Override
    public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {

        JsonObject object = new JsonObject();

        object.addProperty(MessageStandards.MESSAGE_TYPE_NAME, src.getContent().getType().ordinal());
        object.add(MessageStandards.DATA_NAME, context.serialize(src.getContent()));

        System.out.println("Serialized message as " + object.toString());

        return object;
    }
}

package com.ytjojo.practice.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.ytjojo.practice.DateTimeHelper;

import java.lang.reflect.Type;
import java.util.Date;

/**
 * Created by Administrator on 2018/3/21 0021.
 */

public class GsonDateDeserializer implements JsonDeserializer<Date> {
    @Override
    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return DateTimeHelper.getDateFromString(json.getAsString());
    }
}

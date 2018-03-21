package com.ytjojo.http.coverter;

import android.support.v4.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.Primitives;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import com.ytjojo.http.util.CollectionUtils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Map;

import okhttp3.RequestBody;
import okio.Buffer;

/**
 * Created by Administrator on 2018/3/19 0019.
 */

public class GsonJsonAttrConverter extends JsonAttrRequestBodyConverter {


    public static GsonJsonAttrConverter create() {
        GsonBuilder builder = new GsonBuilder();
        builder.enableComplexMapKeySerialization()
                .serializeNulls();
        builder.setDateFormat("yyyy-MM-dd HH:mm:ss");
        builder.registerTypeAdapterFactory(DateTypeAdapter.FACTORY);
        return create(builder.create());
    }
    public static GsonJsonAttrConverter create(Gson gson) {
        return new GsonJsonAttrConverter(gson);
    }
    private GsonJsonAttrConverter(Gson gson){
        this.gson = gson;
    }

    Gson gson;

    @Override
    public RequestBody convert(Map<String, Pair<Type, Object>> value) throws IOException {
        Buffer buffer = new Buffer();
        Writer jsonWriter = new OutputStreamWriter(buffer.outputStream(), JsonAttrRequestBodyConverter.UTF_8);
        JsonWriter writer = gson.newJsonWriter(jsonWriter);
        writer.setSerializeNulls(true);
        writer.beginObject();
        if (!CollectionUtils.isEmpty(value)) {
            for (Map.Entry<String, Pair<Type, Object>> entry : value.entrySet()) {

                Pair<Type, Object> pair = entry.getValue();
                final Object arg = pair.second;
                writer.name(entry.getKey());
                if (arg == null) {
                    writer.nullValue();
                } else {
                    Type type = pair.first;
                    if (Primitives.isPrimitive(type)) {
//                        Type type =  $Gson$Types.canonicalize(handler.getType());
//                        $Gson$Types.getRawType(type);
                        TypeAdapter<?> typeAdapter = gson.getAdapter(TypeToken.get(type));
                        TypeAdapter<Object> adapter = (TypeAdapter<Object>) typeAdapter;
                        adapter.write(writer, arg);
                    } else {
                        writer.jsonValue(gson.toJson(arg, type));
                    }
                }
            }
        }
        writer.endObject();
        writer.close();
        return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
    }
}

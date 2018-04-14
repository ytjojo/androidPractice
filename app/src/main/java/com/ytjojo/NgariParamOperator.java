package com.ytjojo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.Primitives;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import com.ytjojo.http.coverter.DateTypeAdapter;
import com.ytjojo.http.util.CollectionUtils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;

import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.IRequestOperator;
import retrofit2.ParameterRequestOperator;
import retrofit2.ExtendParameterHandler;
import retrofit2.http.ArrayItem;
import retrofit2.http.NgariJsonPost;

/**
 * Created by Administrator on 2018/3/19 0019.
 */

public class NgariParamOperator implements ParameterRequestOperator {



    public static NgariParamOperator create() {
        GsonBuilder builder = new GsonBuilder();
        builder.enableComplexMapKeySerialization()
                .serializeNulls();
        builder.setDateFormat("yyyy-MM-dd HH:mm:ss");
        builder.registerTypeAdapterFactory(DateTypeAdapter.FACTORY);
        return create(builder.create());
    }
    public static NgariParamOperator create(Gson gson) {
        return new NgariParamOperator(gson);
    }
    private NgariParamOperator(Gson gson){
        this.gson = gson;
    }
    Gson gson;

    @Override
    public void operate(@NonNull IRequestOperator requestOperator, @Nullable ArrayList<Annotation> annotations, @Nullable ArrayList<ExtendParameterHandler<?>> handlers, @NonNull Object... args) throws IOException {

        if(handlers.get(0).getAnnotation() instanceof ArrayItem){
            String method = null;
            String serviceId = null;
            if(annotations !=null) {
                for(Annotation annotation :annotations){
                    if(annotation instanceof NgariJsonPost){
                        NgariJsonPost ngariJsonPost = (NgariJsonPost) annotation;
                        method = ngariJsonPost.method();
                        serviceId = ngariJsonPost.serviceId();
                    }
                }
            }
            Buffer buffer = new Buffer();
            Writer jsonWriter = new OutputStreamWriter(buffer.outputStream(), UTF_8);
            JsonWriter writer = gson.newJsonWriter(jsonWriter);
            writer.setSerializeNulls(true);
            if(serviceId!=null && method !=null){
                writer.beginObject();
                writer.name("serviceId").value(serviceId)
                        .name("method").value(method)
                        .name("body");
            }
            writer .beginArray();
            if(!CollectionUtils.isEmpty(handlers)){
                for (ExtendParameterHandler<?> handler:handlers) {
                    final int index = handler.getIndex();
                    final Object item = args[index];
                    if (item == null) {
                        writer.nullValue();
                    } else {
                        Type type = handler.getType();
                        if(Primitives.isPrimitive(handler.getType())){
//                        Type type =  $Gson$Types.canonicalize(handler.getType());
//                        $Gson$Types.getRawType(type);
                            TypeAdapter<Object> adapter = (TypeAdapter<Object>)  gson.getAdapter(TypeToken.get(type));
                            adapter.write(writer,item);
                        }else{
                            writer.jsonValue(gson.toJson(item, handler.getType()));
                        }
                    }
                }
            }
            writer.endArray();
            if(serviceId!=null && method !=null){
                writer.endObject();
            }
            writer.close();
            requestOperator.setBody(RequestBody.create(MEDIA_TYPE, buffer.readByteString()));
        }

    }
}

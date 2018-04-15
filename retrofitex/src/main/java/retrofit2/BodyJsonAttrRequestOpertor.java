package retrofit2;

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
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;

import okhttp3.RequestBody;
import okio.Buffer;

/**
 * Created by Administrator on 2018/4/14 0014.
 */

public class BodyJsonAttrRequestOpertor implements ParameterRequestOperator {
    @Override
    public void operate(IRequestOperator requestOperator, ArrayList<Annotation> annotations, ArrayList<ExtendParameterHandler<?>> handlers, Object... args) throws IOException {
        Gson gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
        Buffer buffer = new Buffer();
        Writer jsonWriter = new OutputStreamWriter(buffer.outputStream(), UTF_8);
        JsonWriter writer = gson.newJsonWriter(jsonWriter);
        writer.setSerializeNulls(true);
        writer.beginObject();
        if(!CollectionUtils.isEmpty(handlers)){
            for (ExtendParameterHandler<?> handler:handlers) {
                final int index = handler.getIndex();
                final Object item = args[index];
                writer.name(handler.getParamName());
                if (item == null) {
                    writer.nullValue();
                } else {
                    Type type = handler.getType();
                    if(Primitives.isPrimitive(handler.getType())){
//                        Type type =  $Gson$Types.canonicalize(handler.getType());
//                        $Gson$Types.getRawType(type);
                        TypeAdapter<?> typeAdapter = gson.getAdapter(TypeToken.get(type));
                        TypeAdapter<Object> adapter = (TypeAdapter<Object>) typeAdapter;
                        adapter.write(writer,item);
                    }else{
                        writer.jsonValue(gson.toJson(item, handler.getType()));
                    }
                }
            }
        }
        writer.endObject();
        writer.close();
        requestOperator.setBody(RequestBody.create(MEDIA_TYPE, buffer.readByteString()));
    }
}
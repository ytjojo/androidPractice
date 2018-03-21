package com.ytjojo.practice.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by Administrator on 2018/3/20 0020.
 */

public class Key extends JsonDeserializer<HashMap<Object, Object>>
        implements ContextualDeserializer {
    JavaType mJavaType;
    JavaType mKeyJavaType;//key对应的JavaType
    JavaType mValueJavaType;//Value对应的JavaType
    JsonDeserializer<Object> _valueDeserializer;
    JsonDeserializer<Object> _keyDeserializer;
    @Override
    public HashMap<Object, Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        HashMap hashMap = new HashMap();
        if (p.getCurrentToken() == JsonToken.START_ARRAY) {
            JsonToken next = p.nextToken();
            while (p.nextToken() != JsonToken.END_ARRAY)
                if (next != null && next == JsonToken.START_ARRAY) {
                    p.nextToken();
                    Object key = _keyDeserializer.deserialize(p, ctxt);
                    p.nextToken();
                    Object value = _valueDeserializer.deserialize(p, ctxt);
                    hashMap.put(key, value);
                    p.nextToken();
                }
        }
        return hashMap;
    }
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        if (property != null) {
            //如果当前Hashmap是类的属性property不为null，如果根就是hashmap，就会为null；
            mJavaType = property.getType();  // -> beanProperty is null when the StringConvertible type is a root value
        } else {
            mJavaType = ctxt.getContextualType();
        }
        mKeyJavaType = mJavaType.getKeyType();
        mValueJavaType = mJavaType.getContentType();
        _keyDeserializer = ctxt.findContextualValueDeserializer(mKeyJavaType, property);
        _valueDeserializer = ctxt.findContextualValueDeserializer(mValueJavaType, property);
        return this;
    }

}

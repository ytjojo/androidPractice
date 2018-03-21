package com.ytjojo.practice;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Administrator on 2018/3/21 0021.
 */

public class NullSerializer {

    static class MyNullArrayJsonSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            if (value == null) {
                jgen.writeStartArray();
                jgen.writeEndArray();
            } else {
                jgen.writeObject(value);
            }
        }
    }
    public static void regeist(ObjectMapper mapper) {
//        mapper.getSerializerFactory().withSerializerModifier(new MyBeanSerializerModifier());
        //上面的方法无效，必须调用setSerializerFactory
        mapper.setSerializerFactory(mapper.getSerializerFactory().withSerializerModifier(new MyBeanSerializerModifier()));
    }
    static class MyBeanSerializerModifier extends BeanSerializerModifier {
        private static JsonSerializer<Object> sNullArrayJsonSerializer = new MyNullArrayJsonSerializer();
        @Override
        public List<BeanPropertyWriter> changeProperties(
                SerializationConfig config, BeanDescription beanDesc,
                 List<BeanPropertyWriter> beanProperties) {
            // 循环所有的beanPropertyWriter
            for (int i = 0; i < beanProperties.size(); i++) {
                BeanPropertyWriter writer = beanProperties.get(i);
                // 判断字段的类型，如果是array，list，set则注册nullSerializer
                if (isArrayType(writer)) {
                    //给writer注册一个自己的nullSerializer
                    writer.assignNullSerializer(this.defaultNullArrayJsonSerializer());
                }
            }
            return beanProperties;
        }

        // 判断是什么类型
        protected boolean isArrayType(BeanPropertyWriter writer) {
            Class<?> clazz = writer.getPropertyType();
            return Collection.class.isAssignableFrom(clazz) || clazz.isArray();
        }

        protected JsonSerializer<Object> defaultNullArrayJsonSerializer() {
            return sNullArrayJsonSerializer;
        }
    }

    @Test
    public void array() throws JsonProcessingException {
        Bean bean = new Bean();
        ObjectMapper objectMapper = new ObjectMapper();
        regeist(objectMapper);
        String json = objectMapper.writeValueAsString(bean);
        System.out.println(json);
    }

    public static class Bean {
        public ArrayList<String> list;
    }
}

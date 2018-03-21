package com.ytjojo.practice;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.ytjojo.practice.jackson.Key;

import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2018/3/20 0020.
 */

public class Jackson {

    @Test
    public void mapSerializerKey() throws IOException{
        String json = "[[{\"x\":10,\"y\":10},\"Ten\"],[{\"x\":20,\"y\":20},\"Twenty\"]]";
        final  ObjectMapper mapper =new ObjectMapper();
        SimpleModule simpleModule =  new SimpleModule();
        simpleModule.addSerializer(Map.class, new JsonSerializer<Map>() {
            @Override
            public void serialize(Map value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
                if(value != null){
                    gen.writeStartArray();
                    for(Object item: value.entrySet()){
                        Map.Entry entry = (Map.Entry) item;
                        gen.writeStartArray();
                        gen.writeObject(entry.getKey());
                        gen.writeObject(entry.getValue());
                        gen.writeEndArray();
                    }
                    gen.writeEndArray();
                }
            }
        });
        mapper.registerModule(simpleModule);
//        mapper.getSerializerProvider().setDefaultKeySerializer(new JsonSerializer<Object>(){
//            @Override
//            public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
////                gen.writeObject(value);
////                mapper.writeValue(gen,value);
//                gen.writeFieldName(mapper.writeValueAsString(value));
//            }
//        });
//        HashMap<Point,String> map = mapper.readValue(json, new TypeReference<HashMap<Point,String>>() {});
//        System.out.println(map.keySet().size()+"");

        HashMap<Point,String> map = new HashMap<>();
        map.put(new Point(3,5),"sdw");
        map.put(new Point(6,2),"fasow");
        HashMap<String,String> stringmap =new HashMap<>();
        stringmap.put("2","sss");
        stringmap.put("511","sss");
        json = mapper.writeValueAsString(map);
        System.out.println(json);
        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
        builder.enableComplexMapKeySerialization();
        Gson gson =  builder.create();
         json =  gson.toJson(map);
        System.out.println(json);



    }
    @Test
    public void mapDeserializerKey() throws IOException{
        String json = "[[{\"x\":10,\"y\":10},\"Ten\"],[{\"x\":20,\"y\":20},\"Twenty\"]]";
        final  ObjectMapper mapper =new ObjectMapper();
        SimpleModule simpleModule =  new SimpleModule();

        simpleModule.addDeserializer(HashMap.class,new Key());
        mapper.registerModule(simpleModule);
        HashMap<Point,String> map = null;
//        json ="[{\"x\":\"22\"},{\"y\":\"3\"}]";

//        mapper.readValue(json,  new TypeReference<ArrayList<Point>>() {});
        JavaType javaType = mapper.getTypeFactory().constructType(new TypeReference<HashMap<Point,String>>() {}.getType());


         map = mapper.readValue(json, new TypeReference<HashMap<Point,String>>() {});
        System.out.println(map.keySet().size()+"" +map);

        GsonBuilder builder = new GsonBuilder();
        builder.serializeNulls();
//        builder.enableComplexMapKeySerialization();
        Gson gson =  builder.create();
         map =  gson.fromJson(json,new TypeToken<HashMap<Point,String>>(){}.getType());

        System.out.println(map.entrySet().iterator().next().getKey().x);



    }
    public static class Point {
        public int x;
        public int y;
        public Point(){}
        public Point(int x,int y){
            this.x = x;
            this.y = y;
        }
    }
}

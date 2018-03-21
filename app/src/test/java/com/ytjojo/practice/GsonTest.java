package com.ytjojo.practice;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.$Gson$Types;
import com.google.gson.internal.Primitives;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;
import com.ytjojo.http.coverter.DateTypeAdapter;
import com.ytjojo.http.coverter.UTCDateUtils;

import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okio.ByteString;

/**
 * Created by Administrator on 2017/7/29 0029.
 */

public class GsonTest {



    @Test
    public void jsonTest() {
        String json = "{\"name\":2222,\"age\":\"11111.00\",\"birthday\":\"2017-12-12\",\"day\":\"2013-06-17 07:01:29\"}";
        System.out.println(json);
        GsonBuilder builder = new GsonBuilder();
        builder.enableComplexMapKeySerialization()
                .serializeNulls();
        Gson gson = builder.create();
        gson = new Gson();
        People people = gson.fromJson(json, People.class);
        System.out.println(people.age + people.name + people.date + people.birthday + people.day);


    }

    public static class People {
        public String name;
        public int age;
        public Date birthday;
        public Date day;
        public Date date;
    }

    @Test
    public void testKey() {
        final CharSequence delimiter = "\", \"";
        System.out.println(delimiter);
        String abc = key("abc");
        String abc123cd = key("abc", "123", "cd");
        String abc123 = key("abc", "123", null);
        //System.out.println(  ByteString.decodeHex(abc123cd).utf8());
        System.out.println(abc.length());
        System.out.println(abc123.length());
        System.out.println(abc123cd.length());
        System.out.println(abc);
        System.out.println(abc123cd);
        System.out.println(abc123);
    }

    private static final String PREFIX_DYNAMIC_KEY = "$d$d$d$";
    private static final String PREFIX_DYNAMIC_KEY_GROUP = "$g$g$g$";

    public static String key(String url, String dynamicKey, String dynamicKeyGroup) {
        return key(url + PREFIX_DYNAMIC_KEY + dynamicKey + PREFIX_DYNAMIC_KEY_GROUP + dynamicKeyGroup);
    }

    public static String key(String key) {
        return ByteString.encodeUtf8(key).md5().utf8();
    }

    public static class DateModel {
        public Date birthday;
    }

    @Test
    public void testDate() {
        String json = "{\"birthday\":\"Thu Oct 16 07:13:48 GMT 2014\"}";
//        String json ="{\"birthday\":\"Thu Oct 16 07:13:48 GMT 2015\"}";
        System.out.println(json);
        GsonBuilder builder = new GsonBuilder();
        builder.enableComplexMapKeySerialization()
                .serializeNulls();
        builder.registerTypeAdapterFactory(DateTypeAdapter.FACTORY);
        Gson gson = builder.create();
        DateModel people = gson.fromJson(json, DateModel.class);
        System.out.println(people.birthday);
    }

    @Test
    public void dateTest() {
        String s = "2016-08-12T16:00Z";
//        Pattern GMT_PATTERN1
//                = Pattern.compile(".*GMT\\+{0,1}(\\d{2,2}):{0,1}\\d{0,2}.*");
//        Matcher matcher = GMT_PATTERN1.matcher(s);
//        System.out.println(matcher.matches());
//        System.out.println(matcher.group(1)+ "   ");

        Date date = new Date();
//        s = date.toString();
        System.out.println(s);
        date = UTCDateUtils.parseDate(s);
//        long m = GMTDateUtils.parseDate(s,0,s.length());
//        date = new Date();
//        date.setTime(m);
        System.out.println(date.toString());
    }

    @Test
    public void testString() {
        Gson gson = new Gson();
//        String json= "{\"body\":{\"birthday\":\"2016-11-22\"}}";
        String json = "{\"body\":\"2016-11-22\"}";
        System.out.println(json);
        JsonElement element = gson.fromJson(json, JsonElement.class);
        JsonObject jsonObject = (JsonObject) element;
        JsonElement element1 = jsonObject.get("body");
        if (element1.isJsonPrimitive()) {
            JsonPrimitive jsonPrimitive = (JsonPrimitive) element1;
            System.out.println(jsonPrimitive.getAsString());
        } else if (element1.isJsonObject()) {
            JsonObject jsonObject1 = (JsonObject) element1;
            System.out.println(gson.toJson(jsonObject1));
        }
    }

    @Test
    public void setDate() {
        GsonBuilder builder = new GsonBuilder();
        builder.enableComplexMapKeySerialization()
                .serializeNulls();
        builder.setDateFormat("yyyy-MM-dd HH:mm:ss");
        builder.registerTypeAdapterFactory(DateTypeAdapter.FACTORY);
        Gson gson = builder.create();
        DateModel dateModel = new DateModel();
        dateModel.birthday = new Date();
        String json = gson.toJson(dateModel);
        System.out.println(json);
    }

    @Test
    public void jsonWriter() throws IOException {
        Gson gson = new Gson();
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = gson.newJsonWriter(stringWriter);
        jsonWriter.beginObject();
        jsonWriter.name("name")
                .value("张三")
                .name("emails")
                .beginArray()
                .value("ytjojo@163.com")
                .value("ytjojo@qq.com")
                .endArray()
                .name("age")
                .value(20)
                .endObject();
        jsonWriter.flush();
        jsonWriter.close();
        String json = stringWriter.toString();
        System.out.println(json);

    }

    @Test
    public void JacksonjsonWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        JsonGenerator generator = new ObjectMapper().getFactory().createGenerator(stringWriter);
        generator.writeStartObject();
        generator.writeStringField("name", "张三");
        generator.writeNumberField("age", 20);
        generator.writeFieldName("emails");
        generator.writeStartArray();

        generator.writeString("ytjojo@163.com");
        generator.writeString("ytjojo@qq.com");

        generator.writeEndArray();
        generator.writeFieldName("bean");
        generator.writeEndObject();
        generator.flush();
        generator.close();

        String json = stringWriter.toString();
        System.out.println(json);

    }

    @Test
    public void gsonReadTree() {
        Gson gson = new Gson();
        String json = "{\"name\":\"张三\",\"age\":20,\"emails\":[\"ytjojo@163.com\",\"ytjojo@qq.com\"]}";
        JsonElement rootEl = gson.fromJson(json, JsonElement.class);
        if (rootEl.isJsonObject()) {
            JsonObject jsonObject = rootEl.getAsJsonObject();
            JsonElement nameEl = jsonObject.get("name");
            if (nameEl.isJsonPrimitive()) {
                String name = nameEl.getAsString();
                System.out.println(name);
            }
             JsonElement emailsEl = jsonObject.get("emails");
            if(emailsEl.isJsonArray()){
                JsonArray jsonArray = emailsEl.getAsJsonArray();
                for(int i= 0 ;i< jsonArray.size() ;i++){
                    String email =jsonArray.get(i).getAsString();
                    System.out.println(email);
                }
            }
            JsonElement ageEl = jsonObject.get("age");
            if(ageEl.isJsonPrimitive()){
                JsonPrimitive agePmt= ageEl.getAsJsonPrimitive();
                if(agePmt.isNumber()){
                    int age = agePmt.getAsInt();
                    System.out.println(age+"");
                }
            }

        }


    }
    @Test
    public void JacksonReadTree() throws IOException {
        String json = "{\"name\":\"张三\",\"age\":20,\"emails\":[\"ytjojo@163.com\",\"ytjojo@qq.com\"]}";

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node = objectMapper.readTree(json);
        if(node.isObject()){
            JsonNode nameNode = node.path("name");
            if(nameNode.isTextual()){
                String name = nameNode.asText();
                System.out.println(name);
            }
            JsonNode ageNode = node.path("age");
            if(ageNode.isNumber()){
                int age = ageNode.asInt();
                System.out.println(age+"");
            }
            JsonNode emailsNode = node.path("emails");
            if(emailsNode.isArray()){
                for (int i = 0; i < emailsNode.size(); i++) {
                   JsonNode emailNode =  emailsNode.get(i);
                    if(emailNode.isTextual()){
                        String email = emailNode.asText();
                        System.out.println(email);
                    }
                }
            }
        }
    }
    @Test
    public void type(){
       Type type = new TypeToken<ArrayList<String>>(){}.getType();
        //等同于一下代码
        type = $Gson$Types.newParameterizedTypeWithOwner(null,ArrayList.class,String.class);
        if(type instanceof ParameterizedType){
            System.out.println("ParameterizedType");
        }
        boolean isWrapperType =   Primitives.isWrapperType(Integer.class);
        boolean isPrimitive =   Primitives.isPrimitive(String.class);
        boolean isintPrimitive =   Primitives.isPrimitive(int.class);
        boolean isIntegerPrimitive =   Primitives.isPrimitive(Integer.class);
        TypeFactory typeFactory= new ObjectMapper().getTypeFactory();
        JavaType javaType  =typeFactory.constructParametricType(ArrayList.class,String.class);
         boolean isTypeOrSubTypeOf = javaType.isTypeOrSubTypeOf(List.class);
        type = new TypeReference<ArrayList<String>>(){}.getType();


    }
}

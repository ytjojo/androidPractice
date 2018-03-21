package com.ytjojo.practice;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.BeanDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBuilder;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.deser.impl.BeanPropertyMap;
import com.fasterxml.jackson.databind.deser.impl.ObjectIdReader;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.impl.BeanAsArraySerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.JsonAdapter;
import com.ytjojo.practice.gson.GsonStringToObjectDeserializer;

import org.junit.Test;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by Administrator on 2017/1/14 0014.
 */

public class JsonTest {

    @Test
    public void testJson() throws IOException {
//        try {

//            InputStream input = getClass().getClassLoader().getResourceAsStream("com/ytjojo/practice/json.txt");

//            Response response = new ObjectMapper().readValue(input, Response.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        objectMapper.getFactory().configure(JsonFactory.Feature.INTERN_FIELD_NAMES, true);
        objectMapper.getFactory().configure(JsonFactory.Feature.CANONICALIZE_FIELD_NAMES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        Response response = null;
//            Response response = objectMapper.readValue(input, Response.class);
        Gson gson = new GsonBuilder().serializeNulls().create();
//            Reader reader = new InputStreamReader(input);
        String json = "{\"code\":200,\"body\": \"{\\\"meetingNumber\\\":\\\"2\\\",\\\"password\\\":\\\"sdwsdwsdw\\\"}\"}";
//        response = objectMapper.readValue(json, Response.class);
            response =  gson.fromJson(json,Response.class);

        System.out.println(response.body.meetingNumber);
        System.out.println(response.body.password);

//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    public static class Response {
        public Response() {
        }

        public int code;
        @JsonDeserialize(using = StringToObjectDeserializer.class)
        @JsonAdapter(GsonStringToObjectDeserializer.class)
        public Body body;

    }

    public static class Body {
        @JsonIgnore
        private String json;

        public Body() {
        }

        //        @JsonCreator
//        public Body(String value){
//            json = value;
//            System.out.println(json);
//        }
        public long meetingNumber;
        public String password;
    }

    /**
     * 用于 注解
     */
    private static class StringToObjectDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {
        private JavaType valueType;

        @Override
        public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
//            JavaType wrapperType = property.getType();
//            JavaType valueType = wrapperType.containedType(0);
            if (property != null)
                valueType = property.getType();  // -> beanProperty is null when the StringConvertible type is a root value
            else {
                valueType = ctxt.getContextualType();
            }

            return this;
        }

        @Override
        public Object deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            String json = parser.getValueAsString();
            return new ObjectMapper().readValue(json, valueType);
        }

        @Override
        public Object getNullValue() {
            Class<?> clazz = valueType.getRawClass();
            try {
                return clazz.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return super.getNullValue();
        }
    }


    public static class StringToBeanDeserializer extends BeanDeserializer {

        /**
         * Constructor used by {@link BeanDeserializerBuilder}.
         */
        public StringToBeanDeserializer(BeanDeserializerBuilder builder, BeanDescription beanDesc,
                                        BeanPropertyMap properties, Map<String, SettableBeanProperty> backRefs,
                                        HashSet<String> ignorableProps, boolean ignoreAllUnknown,
                                        boolean hasViews) {
            super(builder, beanDesc, properties, backRefs,
                    ignorableProps, ignoreAllUnknown, hasViews);
        }

        /**
         * Copy-constructor that can be used by sub-classes to allow
         * copy-on-write style copying of settings of an existing instance.
         */
        public StringToBeanDeserializer(BeanDeserializerBase src) {
            super(src, true);
        }

        protected StringToBeanDeserializer(BeanDeserializerBase src, boolean ignoreAllUnknown) {
            super(src, ignoreAllUnknown);
        }

        public StringToBeanDeserializer(BeanDeserializerBase src, NameTransformer unwrapper) {
            super(src, unwrapper);
        }

        public StringToBeanDeserializer(BeanDeserializerBase src, ObjectIdReader oir) {
            super(src, oir);
        }

        public StringToBeanDeserializer(BeanDeserializerBase src, HashSet<String> ignorableProps) {
            super(src, ignorableProps);
        }
        @Override
        public Object deserializeFromString(JsonParser p, DeserializationContext ctxt) throws IOException {
            // First things first: id Object Id is used, most likely that's it
            if (_objectIdReader != null) {
                return deserializeFromObjectId(p, ctxt);
            }

        /* Bit complicated if we have delegating creator; may need to use it,
         * or might not...
         */
            if (_delegateDeserializer != null) {
                if (!_valueInstantiator.canCreateFromString()) {
                    Object bean = _valueInstantiator.createUsingDelegate(ctxt, _delegateDeserializer.deserialize(p, ctxt));
                    if (_injectables != null) {
                        injectValues(ctxt, bean);
                    }
                    return bean;
                }
            }
            String json = p.getValueAsString();
            JavaType javatype = getValueType();
            return new ObjectMapper().readValue(json, javatype);
        }

    }

    public static class StringToObjectDeserializerModifier extends BeanDeserializerModifier {
        @Override
        public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
            if (deserializer.getClass() == BeanDeserializer.class) {
                JavaType valueType = beanDesc.getType();

                return new StringToBeanDeserializer((BeanDeserializerBase) deserializer);
            }
            return deserializer;
        }

        @Override
        public BeanDeserializerBuilder updateBuilder(DeserializationConfig config, BeanDescription beanDesc, BeanDeserializerBuilder builder) {
//           Iterator<SettableBeanProperty> beanPropertyIterator = builder.getProperties();
//           while (beanPropertyIterator.hasNext()) {
//               SettableBeanProperty settableBeanProperty = beanPropertyIterator.next();
//               if (PropertyName.equals(settableBeanProperty.getName())) {
//                   SettableBeanProperty newSettableBeanProperty = settableBeanProperty.withValueDeserializer(new CustomDeserializer());
//                   builder.addOrReplaceProperty(newSettableBeanProperty, true);
//                   break;
//               }
//           }
//           return builder;
            return super.updateBuilder(config, beanDesc, builder);
        }

        public static ObjectMapper regist() {
//           SerializerFactory serializerFactory = BeanSerializerFactory
//                   .instance
//                   .withSerializerModifier(new MyBeanSerializerModifier());
//
//           DeserializerFactory deserializerFactory = BeanDeserializerFactory
//                   .instance
//                   .withDeserializerModifier(new MyBeanDeserializerModifier());
//
//           ObjectMapper objectMapper = new ObjectMapper();
//           objectMapper.setSerializerFactory(serializerFactory);
//           objectMapper.setDeserializerProvider(new StdDeserializerProvider(deserializerFactory));
            SimpleModule simleModule = new SimpleModule().setDeserializerModifier(new StringToObjectDeserializerModifier());
            return new ObjectMapper().registerModule(simleModule);
        }
    }


    public static class ExtraFieldSerializer extends BeanSerializerBase {

        ExtraFieldSerializer(BeanSerializerBase source) {
            super(source);
        }

        ExtraFieldSerializer(ExtraFieldSerializer source,
                             ObjectIdWriter objectIdWriter) {
            super(source, objectIdWriter);
        }

        protected ExtraFieldSerializer(BeanSerializerBase src, ObjectIdWriter objectIdWriter, Object filterId) {
            super(src, objectIdWriter, filterId);
        }

        ExtraFieldSerializer(ExtraFieldSerializer source,
                             String[] toIgnore) {
            super(source, toIgnore);
        }

        @Override
        public BeanSerializerBase withObjectIdWriter(
                ObjectIdWriter objectIdWriter) {
            return new ExtraFieldSerializer(this, objectIdWriter);
        }

        @Override
        public BeanSerializerBase withIgnorals(String[] toIgnore) {
            return new ExtraFieldSerializer(this, toIgnore);
        }

        @Override
        protected BeanSerializerBase asArraySerializer() {
            return (BeanSerializerBase) (this._objectIdWriter == null && this._anyGetterWriter == null && this._propertyFilterId == null ? new BeanAsArraySerializer(this) : this);
        }

        @Override
        public BeanSerializerBase withFilterId(Object filterId) {
            return new ExtraFieldSerializer(this, this._objectIdWriter, filterId);
        }

        @Override
        public void serialize(Object bean, JsonGenerator jgen,
                              SerializerProvider provider) throws IOException,
                JsonGenerationException {
            jgen.writeStartObject();
            serializeFields(bean, jgen, provider);
            jgen.writeStringField("extraField", "extraFieldValue");
            jgen.writeEndObject();
        }

        public static void regist() {
            ObjectMapper mapper = new ObjectMapper();

            mapper.registerModule(new SimpleModule() {
                @Override
                public void setupModule(SetupContext context) {
                    super.setupModule(context);

                    context.addBeanSerializerModifier(new BeanSerializerModifier() {
                        @Override
                        public JsonSerializer<?> modifySerializer(
                                SerializationConfig config,
                                BeanDescription beanDesc,
                                JsonSerializer<?> serializer) {
                            if (serializer instanceof BeanSerializerBase) {
                                return new ExtraFieldSerializer(
                                        (BeanSerializerBase) serializer);
                            }
                            return serializer;

                        }
                    });
                }
            });
        }
    }

    public String json = "{\"startDate\":\"2016-05-10\",\"endDate\":\"2016-05-10 20:30\"}";

    public static class DateClass {
        @JsonFormat(pattern = "yyyy-MM-dd")
        public Date startDate;
        public Date endDate;

        @JsonInclude(JsonInclude.Include.ALWAYS)
        public String name;
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Integer num;
        @JsonRawValue
        public String json = "{\"startDate\":\"2016-05-10\",\"endDate\":\"2016-05-10 20:30\"}";

        @Override
        public String toString() {
            return startDate + "   " + endDate;
        }
    }

    @Test
    public void testDate() {
        ObjectMapper objectMapper = new ObjectMapper();
        SuperDateDeserializer.regist(objectMapper);
        SimpleDateFormat fmt = new SimpleDateFormat(
                "yyyy-MM-dd");
        String sss;
        try {
//            DateClass dateClass111= objectMapper.readValue(json,DateClass.class);
//            System.out.println(dateClass111.toString());
            DateClass dateClass = new DateClass();
            dateClass.startDate = new Date();
            dateClass.endDate = new Date();
            sss = objectMapper.writeValueAsString(dateClass);
            System.out.println(sss);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static class SuperDateDeserializer extends DateDeserializers.DateDeserializer {

        public static void regist(ObjectMapper mapper) {
            mapper.registerModule(new SimpleModule().addDeserializer(Date.class, new SuperDateDeserializer()));
        }

        public SuperDateDeserializer() {
            super();
        }

        public SuperDateDeserializer(SuperDateDeserializer base, DateFormat df, String formatString) {
            super(base, df, formatString);
        }

        @Override
        protected SuperDateDeserializer withDateFormat(DateFormat df, String formatString) {
            return new SuperDateDeserializer(this, df, formatString);
        }

        @Override
        protected Date _parseDate(JsonParser p, DeserializationContext ctxt) throws IOException {

            if (_customFormat != null) {
                JsonToken t = p.getCurrentToken();
                if (t == JsonToken.VALUE_STRING) {
                    String str = p.getText().trim();
                    if (str.length() == 0) {
                        return (Date) getEmptyValue(ctxt);
                    }
                    SimpleDateFormat simpleDateFormat = (SimpleDateFormat) _customFormat;
                    String patten = simpleDateFormat.toPattern();
                    if (patten.length() != str.length()) {
                        return DateTimeHelper.getDateFromString(str);
                    }
                    synchronized (_customFormat) {
                        try {
                            return _customFormat.parse(str);
                        } catch (ParseException e) {
                            throw new IllegalArgumentException("Failed to parse Date value '" + str
                                    + "' (format: \"" + _formatString + "\"): " + e.getMessage());
                        }
                    }
                }
                // Issue#381
                if (t == JsonToken.START_ARRAY && ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                    p.nextToken();
                    final Date parsed = _parseDate(p, ctxt);
                    t = p.nextToken();
                    if (t != JsonToken.END_ARRAY) {
                        throw ctxt.wrongTokenException(p, JsonToken.END_ARRAY,
                                "Attempted to unwrap single value array for single 'java.util.Date' value but there was more than a single value in the array");
                    }
                    return parsed;
                }
            }
            JsonToken t = p.getCurrentToken();
            if (t == JsonToken.VALUE_STRING) {
                String str = p.getText().trim();
                return DateTimeHelper.getDateFromString(str);
            }
            return super._parseDate(p, ctxt);
        }
    }


}

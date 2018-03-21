package com.ytjojo.http.coverter;

import android.support.v4.util.Pair;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;

/**
 * Created by Administrator on 2018/3/19 0019.
 */

public abstract class JsonAttrRequestBodyConverter implements Converter<Map<String,Pair<Type,Object>>, RequestBody> {
    public static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
    public static final Charset UTF_8 = Charset.forName("UTF-8");
}

/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ytjojo.http.coverter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.$Gson$Types;
import com.ytjojo.http.EntireStringResult;
import com.ytjojo.http.ServerResponse;
import com.ytjojo.http.exception.APIException;
import com.ytjojo.http.exception.JsonException;
import com.ytjojo.http.util.TextUtils;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;
import retrofit2.Converter;

final class GsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
	public static Void DEFAULT_VOID_INSTIANCE;
	private final Gson mGson;
	private final Type type;
	private TypeAdapter<T> adapter;
	enum Irrelevant { INSTANCE; }
	GsonResponseBodyConverter(Gson gson, Type type) {
		this.mGson = gson;
		this.type = type;
	}

	@Override public T convert(ResponseBody responseBody) throws IOException {

		BufferedSource bufferedSource = Okio.buffer(responseBody.source());
		String value = bufferedSource.readUtf8();
		bufferedSource.close();
		if (TextUtils.isEmpty(value)) {
			throw new APIException(-3, "response is null");
		}
		int code = Integer.MAX_VALUE;
		JsonElement root = null;
		JsonObject response;
		try{
			root = mGson.fromJson(value, JsonElement.class);
			if(root.isJsonObject()){
				response = root.getAsJsonObject();
				JsonElement codeElement  = response.get("code");
				if(codeElement != null){
					code = codeElement.getAsInt();
				}else {
					JsonElement bodyJson = response.get("body");
					if(bodyJson == null){
						return parse(root,value);
					}
				}
			}else {
				return parse(root,value);
			}

		}catch (Exception e){
			throw new JsonException("json解析失败",e);
		}

		if (code != ServerResponse.RESULT_OK) {
			JsonElement msgJE = response.get("msg");
			String msg = msgJE == null ? null : msgJE.getAsString();
			throw new APIException(code, msg, value);
		}
		try{
			if (type instanceof Class) {
				if(type == EntireStringResult.class){
					return (T) new EntireStringResult(value);
				}
				if (type == String.class) {
					JsonElement bodyJson = response.get("body");
					if(bodyJson ==null){
						return null;
					}
					if(bodyJson.isJsonPrimitive()){
						return (T) bodyJson.getAsString();
					}
					return (T)(mGson.toJson(bodyJson));
				}
				if (type == Object.class) {
					return (T) Irrelevant.INSTANCE;
				}
				if (type == Void.class) {
					assertVoidInstance();
					return (T) DEFAULT_VOID_INSTIANCE;
				}
				if (type == JsonElement.class) {
					//如果返回结果是JSONObject则无需经过Gson
					return (T)(root);
				}
				if(!(ServerResponse.class.isAssignableFrom((Class<?>) type))){
					Type wrapperType = $Gson$Types.newParameterizedTypeWithOwner(null,ServerResponse.class,type);
					ServerResponse<?> wrapper = mGson.fromJson(value, wrapperType);
					return (T) wrapper.body;
				}
			}
			return mGson.fromJson(value, type);
		}catch (Exception e){
			throw new JsonException("json解析失败",e);
		}

	}
	private void assertVoidInstance(){
		if(DEFAULT_VOID_INSTIANCE ==null){
			Constructor<?>[] cons = Void.class.getDeclaredConstructors();
			cons[0].setAccessible(true);
			try {
				DEFAULT_VOID_INSTIANCE = (Void) cons[0].newInstance(new Object[]{});
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
	private T parse(JsonElement root,String json){
		if(type == EntireStringResult.class){
			return (T) new EntireStringResult(json);
		}
		if (type == Object.class) {
			return (T) Irrelevant.INSTANCE;
		}
		if (type == Void.class) {
			assertVoidInstance();
			return (T) DEFAULT_VOID_INSTIANCE;
		}
		if (type == JsonElement.class) {
			//如果返回结果是JSONObject则无需经过Gson
			return (T)(root);
		}
		return mGson.fromJson(json, type);
	}
}

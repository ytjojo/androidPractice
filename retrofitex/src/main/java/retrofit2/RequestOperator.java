package retrofit2;

import android.support.annotation.Nullable;

import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Created by Administrator on 2018/4/14 0014.
 */

public class RequestOperator implements IRequestOperator {
    RequestBuilder mRequestBuilder;

    private final String httpMethod, relativeUrl;
    private final HttpUrl baseUrl;
    private final MediaType contentType;
    private final boolean hasBody, isFormEncoded, isMultipart;
    public RequestOperator(RequestBuilder requestBuilder, String httpMethod, HttpUrl baseUrl, String relativeUrl,
                           MediaType contentType, boolean hasBody, boolean isFormEncoded, boolean isMultipart){
        this.mRequestBuilder = requestBuilder;
        this.httpMethod = httpMethod;
        this.baseUrl = baseUrl;
        this.relativeUrl = relativeUrl;
        this.contentType = contentType;
        this.hasBody = hasBody;
        this.isFormEncoded = isFormEncoded;
        this.isMultipart = isMultipart;
    }


    @Override
    public void setRelativeUrl(Object relativeUrl) {
        mRequestBuilder.setRelativeUrl(relativeUrl);
    }

    @Override
    public void addHeader(String name, String value) {
        mRequestBuilder.addHeader(name,value);
    }

    @Override
    public void addPathParam(String name, String value, boolean encoded) {
        mRequestBuilder.addPathParam(name,value,encoded);
    }

    @Override
    public void addQueryParam(String name, @Nullable String value, boolean encoded) {
        mRequestBuilder.addQueryParam(name,value,encoded);
    }

    @Override
    public void addFormField(String name, String value, boolean encoded) {
        mRequestBuilder.addFormField(name,value,encoded);
    }

    @Override
    public void addPart(Headers headers, RequestBody body) {
        if(isMultipart()){
            mRequestBuilder.addPart(headers,body);
        }
    }

    @Override
    public void addPart(MultipartBody.Part part) {
        if(isMultipart()){
            mRequestBuilder.addPart(part);
        }
    }

    @Override
    public void setBody(RequestBody body) {
        if(hasBody){
            mRequestBuilder.setBody(body);

        }
    }

    @Override
    public String getHttpMethod() {
        return httpMethod;
    }

    @Override
    public HttpUrl getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getRelativeUrl() {
        return relativeUrl;
    }

    @Override
    public boolean hasBody() {
        return hasBody;
    }

    @Override
    public boolean isFormEncoded() {
        return isFormEncoded;
    }

    @Override
    public boolean isMultipart() {
        return isMultipart;
    }

    @Override
    public MediaType getContentType() {
        return contentType;
    }
}

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

public interface IRequestOperator {


    void setRelativeUrl(Object relativeUrl);

    void addHeader(String name, String value);

    void addPathParam(String name, String value, boolean encoded);


    void addQueryParam(String name, @Nullable String value, boolean encoded);

    void addFormField(String name, String value, boolean encoded);

    void addPart(Headers headers, RequestBody body);

    void addPart(MultipartBody.Part part);

    void setBody(RequestBody body);

    String getHttpMethod();

    HttpUrl getBaseUrl();

    String getRelativeUrl();

    boolean hasBody();

    boolean isFormEncoded();

    boolean isMultipart();

    MediaType getContentType();

}

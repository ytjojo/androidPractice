package com.ytjojo.http;

import android.content.Context;

import com.ytjojo.BaseApplication;
import com.ytjojo.domin.request.LoginRequest;
import com.ytjojo.domin.response.OrganAddrArea;
import com.ytjojo.domin.vo.LoginResponse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.http.ArrayItem;
import retrofit2.http.JsonAttr;
import retrofit2.ProxyHandler;
import retrofit2.Retrofit;
import retrofit2.ServiceAndMethod;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Url;
import rx.Observable;


public class RetrofitClient {
    public static final String BASE_URL="http://192.168.0.46:8080";
    public static volatile   String TOKEN;
    public static  final String TOKEN_HEADER_KEY = "X-Access-Token";
    public static  final String AUTHTYPE_HEADER_KEY = "Auth_Type";
    public static  final String AUTHTYPE_TOKEN = "Auth_Type_Token";
    public static  final String AUTHTYPE_BASIC = "Auth_Type_Basic";
    public static  final String ContentType_JSON = "application/json";
    public static  final String ContentType_FORM = "application/x-www-form-urlencoded; charset=UTF-8";
    public static final int HTTP_RESPONSE_DISK_CACHE_MAX_SIZE=10 * 1024 * 1024;
    private static Retrofit retrofit ;
    static OkHttpClient mOkHttpClient;
    public static void configClient(){
        // Base directory recommended by http://stackoverflow.com/a/32752861/400717.
//        final File baseDir = context.getCacheDir();
//        if (baseDir != null) {
//            final File cacheDir = new File(baseDir, "HttpResponseCache");
//            new Cache(cacheDir, HTTP_RESPONSE_DISK_CACHE_MAX_SIZE);
//        }

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        mOkHttpClient = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .retryOnConnectionFailure(true)
                .connectTimeout(15, TimeUnit.SECONDS)
                .addNetworkInterceptor(new UserAgentInterceptor(""))
                .build();
    }



    private static void create(Context c){
        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(CustomerOkHttpClient.getInitClient(c.getApplicationContext()))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
    public  <T> T getService(Class<T> service){
        return retrofit.create(service);
    }
    public <T> T getHackedService(Class<T> service){
        return ProxyHandler.create(retrofit,service);
    }

    public static Retrofit getRetrofit(Context c) {
        if(retrofit == null){
            create(c);
        }
        return retrofit;
    }

    public static Retrofit getRetrofit() {
        return retrofit;
    }
    public void uploadByPartmap(String token,String catalog, int mode, String id, File file, HashMap<String,String> params){

        RequestBody catalogRB = RequestBody.create(null, catalog);
        RequestBody idRB = RequestBody.create(null, id);
        RequestBody modeRB = RequestBody.create(MediaType.parse("text/plain"), token);
        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        for(Map.Entry<String,String> entry: params.entrySet()){
            RequestBody value =RequestBody.create(null, entry.getValue());
            requestBodyMap.put(entry.getKey(),value);
        }
//        RequestBody requestFile =
//                RequestBody.create(MediaType.parse("application/otcet-stream"), file);
        MultipartBody.Part fileBody = MultipartBody.Part.createFormData(
                "file",
                file.getName(),
                RequestBody.create(MediaType.parse("image/*"), file));
        RetrofitClient.getRetrofit(BaseApplication.getInstance()).create(GitApiInterface.class).uploadImage(token,catalogRB,idRB,modeRB,fileBody);
    }
    public void upload(String token, int mode, String id, File file, HashMap<String,String> params){
        Map<String, RequestBody> requestBodyMap = new HashMap<>();
        for(Map.Entry<String,String> entry: params.entrySet()){
            RequestBody value = RequestBody.create(MediaType.parse("multipart/form-body"), entry.getValue());
            requestBodyMap.put(entry.getKey(),value);
        }
        String fileName = "file\"; filename=\"" + file.getName();
        requestBodyMap.put(fileName, RequestBody.create(MediaType.parse("multipart/form-body"), file));
        RetrofitClient.getRetrofit(BaseApplication.getInstance()).create(GitApiInterface.class).uploadImagePartMap(requestBodyMap);
    }
    public interface GitApiInterface {

        @Multipart
        @POST("File/upload")
        Observable<Boolean> uploadImage(
                @Header(RetrofitClient.TOKEN_HEADER_KEY) String token,
                @Part("catalog") RequestBody catalog,
                @Part("doctorId") RequestBody id,
                @Part("mode") RequestBody mode,
                @Part MultipartBody.Part image
        );
        @Multipart
        @POST("File/upload")
        Observable<Boolean> uploadImagePartMap(
                @PartMap Map<String, RequestBody> params
        );
        @POST("http://ngaribata.ngarihealth.com:8980/ehealth-base-feature3/logon/login")
        Observable<LoginResponse> loginAttr(@JsonAttr("uid") String uid, @JsonAttr("pwd") String pwd, @JsonAttr("rid") String rid, @JsonAttr("forAccessToken") boolean forAccessToken);
        @POST("http://ngaribata.ngarihealth.com:8980/ehealth-base-feature3/logon/login")
        Observable<LoginResponse> login(@Body LoginRequest request);
        @POST("http://ngaribata.ngarihealth.com:8980/ehealth-base-feature3/logon/login")
        Observable<LoginResponse.UserRoles> loginRoles(@Body LoginRequest request);
        @POST()
        @ServiceAndMethod(method = "getOgranAddrArea",serviceId = "eh.organ")
        Observable<OrganAddrArea> loginWithArray(@Url String url, @ArrayItem int id );

//
//        @Headers("User-Agent: Retrofit2.0Tutorial-App")
//        @GET("/search/users")
//        Call<GitResult> getUsersNamedTom(@Query("q") String name);
//
//        @POST("/user/create")
//        Call<Item> createUser(@Body String name, @Body String email);
//
//        @PUT("/user/{id}/update")
//        Call<Item> updateUser(@Path("id") String id , @Body Item user);
    }
}

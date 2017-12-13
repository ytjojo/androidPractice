package com.ytjojo.practice;

import com.google.gson.JsonObject;
import com.ytjojo.http.coverter.GsonConverterFactory;
import com.ytjojo.http.interceptor.HttpLoggingInterceptor;
import com.ytjojo.http.interceptor.ReceivedCookiesInterceptor;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.http.GET;

/**
 * Created by Administrator on 2017/8/19 0019.
 */

public class ArticleTest {
    @Test
    public void test(){
        HttpLoggingInterceptor loginter = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override public void log(String message) {
                System.out.println(decode(message));
            }
        });

        loginter.setLevel( HttpLoggingInterceptor.Level.CONTENT);
        OkHttpClient o = new OkHttpClient.Builder().addInterceptor(loginter).addInterceptor(new ReceivedCookiesInterceptor()).build();

        Retrofit  retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(o)
                .baseUrl("http://www.ngarihealth.com/").build();
        System.out.println("setUp");
        retrofit.create(ArticleService.class).getList1().subscribe(new Observer<Void>() {

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onNext(Void ss) {
                System.out.println("finish ------------------ss");
            }
        });

    }

    public interface ArticleService{

        @GET("api.php/App/getArticlelist_v2?page=0&organid=1&from=app&pubver=2&catid=5")
        Observable<JsonObject>  getList();
        @GET("api.php/App/getArticlelist_v2?page=0&organid=1&from=app&pubver=2&catid=5")
        Observable<Void> getList1();
    }
    public static String decode(String unicodeStr) {
        if (unicodeStr == null) {
            return null;
        }
        StringBuffer retBuf = new StringBuffer();
        int maxLoop = unicodeStr.length();
        for (int i = 0; i < maxLoop; i++) {
            if (unicodeStr.charAt(i) == '\\') {
                if ((i < maxLoop - 5)
                        && ((unicodeStr.charAt(i + 1) == 'u') || (unicodeStr
                        .charAt(i + 1) == 'U')))
                    try {
                        retBuf.append((char) Integer.parseInt(
                                unicodeStr.substring(i + 2, i + 6), 16));
                        i += 5;
                    } catch (NumberFormatException localNumberFormatException) {
                        retBuf.append(unicodeStr.charAt(i));
                    }
                else
                    retBuf.append(unicodeStr.charAt(i));
            } else {
                retBuf.append(unicodeStr.charAt(i));
            }
        }
        return retBuf.toString();
    }
}

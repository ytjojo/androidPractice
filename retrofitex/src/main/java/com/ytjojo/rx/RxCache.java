package com.ytjojo.rx;

import android.content.Context;

import com.ytjojo.http.cache.ACache;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Jam on 16-7-6
 * Description:
 * RxJava + Retrofit 的缓存机制
 */
public class RxCache {


    /**
     * @param context
     * @param cacheKey     缓存key
     * @param expireTime   过期时间 0 表示有缓存就读，没有就从网络获取
     * @param fromNetwork  从网络获取的Observable
     * @param forceRefresh 是否强制刷新
     * @param <T>
     * @return
     */
    public static <T> Observable<T> load(final Context context, final String cacheKey, final long expireTime, Observable<T> fromNetwork, boolean forceRefresh) {
        Observable<T> fromCache = Observable.defer(new Callable<ObservableSource<T>>() {
            @Override
            public ObservableSource<T> call() throws Exception {
                return Observable.create(new ObservableOnSubscribe<T>() {
                    @Override
                    public void subscribe(@NonNull ObservableEmitter<T> e) throws Exception {
                        T cache = (T) CacheManager.readObject(context, cacheKey, expireTime);
                        if(!e.isDisposed()){
                            if (cache != null) {
                                e.onNext(cache);
                            } else {
                                e.onComplete();
                            }
                        }

                    }

                });
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());


        /**
         * 这里的fromNetwork 不需要指定Schedule,在handleRequest中已经变换了
         */
        fromNetwork = fromNetwork.map(new Function<T, T>() {
            @Override
            public T apply(T result) {
                CacheManager.saveObject(context, (Serializable) result, cacheKey);
                return result;
            }
        });
        if (forceRefresh) {
            return fromNetwork;
        } else {
            return Observable.concat(fromCache, fromNetwork).take(1);
        }

    }


    ACache mACache;

    public RxCache(File cacheDir, long max_zise) {
        mACache = ACache.get(cacheDir, max_zise, Integer.MAX_VALUE);
    }


    public Observable<? extends Serializable> get(Class<? extends Serializable> clazz) {
        return Observable.defer(new Callable<ObservableSource<? extends Serializable>>() {
            @Override
            public ObservableSource<? extends Serializable> call() throws Exception {
                Serializable value = (Serializable) mACache.getAsObject(clazz.getName());
                if(value == null){
                    return Observable.error(new RuntimeException("notfound"));
                }
                return Observable.just(value);
            }
        });

    }

}
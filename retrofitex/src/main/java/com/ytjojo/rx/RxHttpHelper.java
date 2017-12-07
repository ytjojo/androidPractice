package com.ytjojo.rx;

import com.trello.rxlifecycle2.LifecycleTransformer;
import com.ytjojo.http.exception.AuthException;
import com.ytjojo.http.exception.TokenInvalidException;

import java.io.EOFException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

/**
 * Created by Administrator on 2017/8/19 0019.
 */

public class RxHttpHelper {

    public static Function<Observable<? extends Throwable>, Observable<?>> getRetryFunc1() {
        return new Function<Observable<? extends Throwable>, Observable<?>>() {
            private int retryDelaySecond = 5;
            private int retryCount = 0;
            private int maxRetryCount = 1;

            @Override
            public Observable<?> apply(Observable<? extends Throwable> observable) {
                return observable.flatMap(new Function<Throwable, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(Throwable throwable) {
                        return checkApiError(throwable);
                    }
                });
            }

            private Observable<?> checkApiError(Throwable throwable) {
                retryCount++;
                if (retryCount <= maxRetryCount) {
                    if (throwable instanceof ConnectException
                            || throwable instanceof SocketTimeoutException
                            || throwable instanceof TimeoutException || throwable instanceof UnknownHostException || throwable instanceof EOFException) {
                        return retry(throwable);
                    } else if (throwable instanceof AuthException) {
                        login();
                        return Observable.error(HttpExceptionHandle.handleException(throwable));
                    } else if (throwable instanceof TokenInvalidException) {
                        login();
                        return Observable.error(HttpExceptionHandle.handleException(throwable));
                    }
                    if (throwable instanceof HttpException) {
                        HttpException he = (HttpException) throwable;
                        if (he.code() != 401 && he.code() != 403 && he.code() != 409) {
                            return Observable.error(HttpExceptionHandle.handleException(throwable));
                        }else {
                            return retry(throwable);
                        }
                    }
                    return Observable.error(HttpExceptionHandle.handleException(throwable));
                }else{
                    if (throwable instanceof HttpException) {
                        HttpException he = (HttpException) throwable;
                        if (he.code() == 401 || he.code() == 403 || he.code() == 409) {
                            login();
                        }
                    }
                    return Observable.error(HttpExceptionHandle.handleException(throwable));
                }

            }

            /**
             *
             * @param throwable
             * @return
             */
            private Observable<?> retry(Throwable throwable) {
                if (retryCount <= maxRetryCount) {
                    return Observable.timer(retryDelaySecond,
                            TimeUnit.SECONDS).observeOn(Schedulers.io());
                } else {
                    return Observable.error(throwable);
                }
            }

            private void login() {

            }
        };
    }

    public static <T> ObservableTransformer<T, T> applySchedulers() {
        return new ObservableTransformer<T, T>() {
            @Override
            public ObservableSource<T> apply(Observable<T> observable) {
                return observable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()).retryWhen(getRetryFunc1());

            }
        };
    }

    public static <T> ObservableTransformer<T, T> applySchedulers(final LifecycleTransformer transformer, Class<T> tClass) {
        return new ObservableTransformer<T, T>() {
            @Override
            public Observable<T> apply(Observable<T> observable) {
                return observable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()).retryWhen(getRetryFunc1()).compose(transformer);
            }
        };
    }
    public static <T> ObservableTransformer<T, T> applySchedulers(final LifecycleTransformer<T> transformer) {
        return new ObservableTransformer<T, T>() {
            @Override
            public Observable<T> apply(Observable<T> observable) {
                return observable.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread()).retryWhen(getRetryFunc1()).compose(transformer);
            }
        };
    }
}

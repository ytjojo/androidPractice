package com.ytjojo.practice;

import com.ytjojo.http.exception.AuthException;
import com.ytjojo.rx.RxBus;

import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.lang.reflect.Constructor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by Administrator on 2017/8/7 0007.
 */

public class Rxtest {


    @Test
    public void errortest() {
        getObservable().subscribeOn(Schedulers.io())
//                .retryWhen(attempts -> {
//            return attempts.zipWith(Observable.range(1, 3), (n, i) -> i).flatMap(i -> {
//                System.out.println("delay retry by " + i + " second(s)");
//                return Observable.timer(i, TimeUnit.SECONDS);
//            });
//        })
                .retryWhen(getRetryFunc1())
//                .forEach(new Action1<Integer>() {
//                    @Override
//                    public void call(Integer integer) {
//
//                    }
//                })
                .observeOn(Schedulers.newThread())
                .subscribe(new Observer<Integer>() {

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onCompleted--" + Thread.currentThread().getName());
                    }

                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(Integer integer) {
                        System.out.println("integer" + integer);
                    }
                });
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Observable<Integer> getObservable() {
        return Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> e) throws Exception {
                //                subscriber.onNext(1);
//                e.onNext(2);
//                e.onNext(3);
                System.out.println("call");
                System.out.println("currentThread--" + Thread.currentThread().getName());
//                e.onError(new NullPointerException());
                e.onError(new IllegalArgumentException());
                e.onComplete();
            }
        });
    }

    public static Function<Observable<? extends Throwable>, Observable<?>> getRetryFunc1() {
        return new Function<Observable<? extends Throwable>, Observable<?>>() {
            private int retryDelaySecond = 5;
            private int retryCount = 0;
            private int maxRetryCount = 3;

            @Override
            public Observable<?> apply(Observable<? extends Throwable> observable) {
                return observable.flatMap(new Function<Throwable, ObservableSource<?>>() {
                    @Override
                    public Observable<?> apply(Throwable throwable) {
                        return checkApiError(throwable);
                    }
                });
            }

            private Observable<?> checkApiError(Throwable throwable) {
                retryCount++;
                System.out.println(retryCount + "retryCount--" + Thread.currentThread().getName());

                if (throwable instanceof IllegalArgumentException) {
                    return retry(true);
                } else if (throwable instanceof NullPointerException) {
                    return retry(false);
                }
                return Observable.error(throwable);
            }

            private Observable<?> retry(boolean throwError) {

                if (retryCount <= maxRetryCount) {
                    return Observable.timer(retryDelaySecond,
                            TimeUnit.SECONDS).observeOn(Schedulers.newThread());
                } else {
                    if (throwError) {
                        return Observable.error(new AuthException(-100, "token超时"));
                    } else {
                        return Observable.error(new AuthException(-100, "token超时"));
                    }
//                    return Observable.error(new AuthException(-100, "token超时"));
                }
            }
        };
    }

    @Test
    public void Rxbustext() {
        Disposable s1 = RxBus.getDefault().registerObservable(Integer.class).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                System.out.println(integer + " s 1");
            }
        });
        Disposable s2 = RxBus.getDefault().registerObservable(Integer.class).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                System.out.println(integer + " s 2");
            }
        });
        Disposable s3 = RxBus.getDefault().registerObservable(Integer.class).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                System.out.println(integer + " s 3");
            }
        });
        RxBus.getDefault().registerObservable(Integer.class).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                System.out.println(integer + " s 4");
            }
        });

        RxBus.getDefault().registerObservable(Integer.class).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                System.out.println(integer + " s 5");
            }
        });

        RxBus.getDefault().post(100);
    }

    @Test
    public void testFlowable() {
        Flowable.just(1).subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(5);
                System.out.println("");
            }

            @Override
            public void onNext(Integer integer) {
                System.out.println("");
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("");
            }

            @Override
            public void onComplete() {
                System.out.println("");
            }
        });
    }

    Subscription subscription;

    @Test
    public void testFlowable1() {
        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull FlowableEmitter<Integer> e) throws Exception {
                e.onNext(1);
                e.onNext(100);
//                e.onComplete();
            }
        }, BackpressureStrategy.BUFFER)
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(3);
                        System.out.println("");
                        subscription = s;
                    }

                    @Override
                    public void onNext(Integer integer) {
                        System.out.println("" + integer);
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        System.out.println(t.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                    }
                });

    }

    @Test
    public void testFlowableVoid() {
        Flowable.create(new FlowableOnSubscribe<Void>() {
            @Override
            public void subscribe(@NonNull FlowableEmitter<Void> e) throws Exception {
                Constructor<?>[] s = Void.class.getDeclaredConstructors();
                s[0].setAccessible(true);
                Void voids = (Void) s[0].newInstance(new Object[]{});
                e.onNext(voids);
                Void voids1 = (Void) s[0].newInstance(new Object[]{});
                e.onNext(voids1);
//                e.onComplete();
            }
        }, BackpressureStrategy.BUFFER)
                .subscribe(new Subscriber<Void>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(3);
                        System.out.println("");
                        subscription = s;
                    }

                    @Override
                    public void onNext(Void integer) {
                        System.out.println("" + integer);
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        System.out.println(t.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                    }
                });

    }
    @Test
    public void testthread(){
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull FlowableEmitter<Integer> e) throws Exception {
                e.onNext(100);
                e.onNext(200);
                e.onComplete();
            }
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.io())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(3);
                        System.out.println("");
                        subscription = s;
                    }

                    @Override
                    public void onNext(Integer integer) {
                        System.out.println("" + integer);
                    }

                    @Override
                    public void onError(Throwable t) {
                        t.printStackTrace();
                        System.out.println(t.getMessage());
                        countDownLatch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("onComplete");
                        countDownLatch.countDown();
                    }
                });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

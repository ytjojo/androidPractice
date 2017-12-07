package com.ytjojo.rx;

import android.support.annotation.NonNull;
import android.util.Log;

import com.trello.rxlifecycle2.LifecycleTransformer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.FlowableSubscriber;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.exceptions.Exceptions;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Consumer;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;

/**
 * Author: 杨腾蛟
 * Email: ytjojo@163.com
 * Date: 7/12/17.
 */
public class RxBus {
    private static final String TAG = RxBus.class.getSimpleName();
    private static volatile RxBus instance;
    public static boolean DEBUG = false;
    private ConcurrentHashMap<String, FlowableProcessor> subjectMapper = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Type, CopyOnWriteArrayList<FlowableEmitter<?>>> classFlowableEmitter = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Type, Disposable> typeOfDisposable = new ConcurrentHashMap<>();
    FlowableProcessor allBus;

    // 单例RxBus
    public static RxBus getDefault() {
        RxBus rxBus = instance;
        if (instance == null) {
            synchronized (RxBus.class) {
                rxBus = instance;
                if (instance == null) {
                    rxBus = new RxBus();
                    instance = rxBus;
                }
            }
        }
        return rxBus;
    }

    private RxBus() {
        allBus = PublishProcessor.create().toSerialized();
    }

    public <T> Flowable<T> register(@NonNull Class<T> clazz) {
        return toObserverable(clazz);
    }




    public <T> Flowable<T> registerObservable(Class<T> clazz) {
        Disposable clazzDisposable = typeOfDisposable.get(clazz);
        if (clazzDisposable == null) {

            Disposable disposable = toObserverable(clazz).subscribe(new Consumer<T>() {
                @Override
                public void accept(T t) throws Exception {
                    final CopyOnWriteArrayList<FlowableEmitter<?>> subscribers = classFlowableEmitter.get(clazz);
                    FlowableEmitter lastsubscriber = subscribers.get(subscribers.size() - 1);
                    try {
                        lastsubscriber.onNext(t);
                    } catch (Throwable e) {
                        Exceptions.throwIfFatal(e);

                        try {
                            lastsubscriber.onError(e);
                        } catch (Exception excep) {
                            Exceptions.throwIfFatal(e);
                            // can't call onError because no way to know if a Subscription has been set or not
                            // can't call onSubscribe because the call might have set a Subscription already
                            RxJavaPlugins.onError(excep);

                            NullPointerException npe = new NullPointerException("Actually not, but can't throw other exceptions due to RS");
                            npe.initCause(excep);
                            throw npe;
                        }

                    }
                }
            });
            typeOfDisposable.put(clazz, disposable);
        }
        final Disposable liftSubscriber = clazzDisposable;
        return Flowable.create(new FlowableOnSubscribe<T>() {

            @Override
            public void subscribe(@io.reactivex.annotations.NonNull FlowableEmitter<T> e) throws Exception {
                e.setCancellable(new Cancellable() {
                    @Override
                    public void cancel() throws Exception {
                        final CopyOnWriteArrayList<FlowableEmitter<?>> subscribers = classFlowableEmitter.get(clazz);
                        subscribers.remove(e);
                        if (subscribers.isEmpty()) {
                            if (!liftSubscriber.isDisposed()) {
                                liftSubscriber.dispose();
                            }
                            typeOfDisposable.remove(clazz);
                        }
                    }
                });
                CopyOnWriteArrayList<FlowableEmitter<?>> subscribers = classFlowableEmitter.get(clazz);
                if (subscribers == null) {
                    subscribers = new CopyOnWriteArrayList<FlowableEmitter<?>>();
                    classFlowableEmitter.put(clazz, subscribers);

                }
                subscribers.add(e);
            }
        }, BackpressureStrategy.BUFFER);

    }


    public <T> Flowable<T> toObserverable(Class<T> eventType) {
        return allBus.ofType(eventType);
    }

    public void unregister(@NonNull String tag, @NonNull Disposable disposable) {
        FlowableProcessor processor = subjectMapper.get(tag);
        if (null != processor) {
            if (!processor.hasSubscribers() && subjectMapper.size() > 16) {
                subjectMapper.remove(tag);
            }
        }
        if (!disposable.isDisposed()) {
            disposable.isDisposed();
        }

        if (DEBUG) Log.d(TAG, "[unregister]subjectMapper: " + subjectMapper);
    }


    public <T> Flowable<T> register(@NonNull String tag, @NonNull Class<T> clazz) {
        FlowableProcessor subject = subjectMapper.get(tag);
        if (null == subject) {
            subject = PublishProcessor.create().toSerialized();
            subjectMapper.put(tag, subject);
        }
        if (DEBUG) Log.d(TAG, "[register]subjectMapper: " + subjectMapper);
        return subject.ofType(clazz);
    }

    @SuppressWarnings("unchecked")
    public void post(@NonNull String tag, @NonNull Object content) {
        FlowableProcessor subject = subjectMapper.get(tag);
        if (subject != null && subject.hasSubscribers()) {
            subject.onNext(content);
        } else {
            if (DEBUG) Log.d(TAG, "[send]subjectMapper: failed non regist this type event");
        }
        if (DEBUG) Log.d(TAG, "[send]subjectMapper: " + subjectMapper);
    }

    public void post(@NonNull Object content) {
//        post(content.getClass().getName(), content);
        allBus.onNext(content);
    }

    /**
     * dosen't designation to use specail thread,It's depending on what the 'send' method use
     *
     * @param lifecycleTransformer rxlifecycle
     * @return
     */
    public <T> Flowable<Object> toObserverable(Class<T> clazz, LifecycleTransformer<T> lifecycleTransformer) {
        return toObserverable(clazz).compose(lifecycleTransformer);
    }

    /**
     * designation use the MainThread, whatever the 'send' method use
     *
     * @param lifecycleTransformer rxlifecycle
     * @return
     */
    public <T> Flowable<T> toMainThreadObserverable(Class<T> clazz, LifecycleTransformer<T> lifecycleTransformer) {
        return toObserverable(clazz).observeOn(AndroidSchedulers.mainThread()).compose(lifecycleTransformer);
    }
    private void test() {
        Flowable.just(true).subscribe(new FlowableSubscriber<Boolean>() {
            @Override
            public void onSubscribe(@io.reactivex.annotations.NonNull Subscription s) {
                s.request(Integer.MAX_VALUE);
                s.cancel();
            }

            @Override
            public void onNext(Boolean aBoolean) {

            }

            @Override
            public void onError(Throwable t) {

            }

            @Override
            public void onComplete() {

            }
        });
        Observable.just(1).subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
            }

            @Override
            public void onNext(@io.reactivex.annotations.NonNull Integer integer) {

            }

            @Override
            public void onError(@io.reactivex.annotations.NonNull Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
        Flowable.create(new FlowableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@io.reactivex.annotations.NonNull FlowableEmitter<Integer> e) throws Exception {
                e.onNext(1);
            }
        }, BackpressureStrategy.BUFFER).subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) throws Exception {

            }
        });
        Flowable.unsafeCreate(new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onNext(1);
            }
        }).subscribeOn(Schedulers.io());

    }
}
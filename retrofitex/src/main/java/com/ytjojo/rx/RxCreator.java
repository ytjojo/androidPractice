package com.ytjojo.rx;

import android.support.v4.util.Pair;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Cancellable;
import io.reactivex.functions.Function;


/**
 * Created by Administrator on 2016/4/1 0001.
 */
public class RxCreator {


    public static <T> Observable<T> periodically(long INITIAL_DELAY, long POLLING_INTERVAL, Function<Long, Observable<T>> func0) {
        return Observable.interval(INITIAL_DELAY, POLLING_INTERVAL, TimeUnit.SECONDS).flatMap(func0);
    }


    public static <T, R> Observable<Pair<T, R>> subscribeAllFinish(Observable<T> o1, Observable<R> o2) {
        return Observable.zip(o1, o2, new BiFunction<T, R, Pair<T, R>>() {
            @Override
            public Pair<T, R> apply(T t, R r) {
                Pair<T, R> pair = new Pair<T, R>(t, r);
                return pair;
            }
        });
    }

    public static Observable<Long> sample(int totalSeconds) {
        return Observable.interval(1, TimeUnit.SECONDS)
                .take(totalSeconds + 1).map(new Function<Long, Long>() {


                    @Override
                    public Long apply(Long aLong) {
                        return totalSeconds - aLong;
                    }
                });
    }

    public static void interval(int totalSeconds) {
        Observable.interval(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .take(totalSeconds + 1)
                .map(new Function<Long, Long>() {


                    @Override
                    public Long apply(Long aLong) {
                        return totalSeconds - aLong;
                    }
                });


    }


    public <T> Observable<T> create(final Callable<T> callable) {
        return Observable.fromCallable(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return callable.call();
            }
        });

    }

    //// 界面按钮需要防止连续点击的情况
    //public void click(View v) {
    //    RxView.clicks(v)
    //            .throttleFirst(680, TimeUnit.MILLISECONDS)
    //            .subscribe(new Action1<Void>() {
    //                @Override
    //                public void call(Void aVoid) {
    //
    //                }
    //            });
    //}


    public static <T> List<T> transferAsyncToSync(Observable<T> observable) {
        return observable.toList().blockingGet();
    }


    public static Observable<Boolean> verifyLogin(Observable<String> ObservableEmail, Observable<String> ObservablePassword) {
        return Observable.combineLatest(ObservableEmail, ObservablePassword, new BiFunction<String, String, Boolean>() {
            @Override
            public Boolean apply(String email, String password) {
                return isEmailValid(email) && isPasswordValid(password);
            }
        });
    }

    private static boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@") && email.contains(".");
    }

    private static boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 6;
    }

    /**
     * 推迟执行动作
     *
     * @param delay
     * @param func1
     * @param <R>
     */
    public <R> Observable<R> delayExecute(long delay, Function<Long, R> func1) {
        return Observable.timer(delay, TimeUnit.SECONDS).map(func1);
    }

    /**
     * 用zip实现推送发送执行结果如下
     */
    public <R> Observable<R> delayDelivery(long delay, Observable<R> observable) {
        return Observable.zip(Observable.timer(delay, TimeUnit.SECONDS), observable, new BiFunction<Long, R, R>() {

            @Override
            public R apply(Long aLong, R r) {
                return r;
            }
        });
    }

    public <T> Observable<T> loadData(Observable<T> memory, Observable<T> disk, Observable<T> net) {
        return Observable.concat(memory, disk, net).takeLast(1);

    }

    public <T> Observable<T> mergeData(Observable<T> memory, Observable<T> disk, Observable<T> net) {
        return Observable.merge(memory, disk, net).observeOn(AndroidSchedulers.mainThread());

    }

    //public void textwatcher(EditText et, Action1<String> action1) {
    //    RxTextView.textChangeEvents(et)
    //            .debounce(400, TimeUnit.MILLISECONDS)
    //            .observeOn(AndroidSchedulers.mainThread())
    //            .subscribe(new Observer<TextViewTextChangeEvent>() {
    //                @Override
    //                public void onCompleted() {
    //                }
    //
    //                @Override
    //                public void onError(Throwable e) {
    //                }
    //
    //                @Override
    //                public void onNext(TextViewTextChangeEvent onTextChangeEvent) {
    //                    action1.call(onTextChangeEvent.text().toString());
    //                }
    //            });
    //}


    //public static <T> Observable<T> click(View view, Action1 onStart, Observable<T> observable, RxFragment fragment) {
    //    return RxView.clicks(view)
    //            .subscribeOn(AndroidSchedulers.mainThread())
    //            .doOnNext(onStart)
    //            .throttleFirst(400, TimeUnit.MILLISECONDS)
    //            .observeOn(Schedulers.io())
    //            .switchMap(new Func1<Void, Observable<T>>() {
    //                @Override
    //                public Observable<T> call(Void aVoid) {
    //                    return observable;
    //                }
    //            })
    //            .compose(fragment.bindUntilEvent(FragmentEvent.DESTROY_VIEW))
    //            .observeOn(AndroidSchedulers.mainThread());
    //}
    //public static Observable<String> search(EditText et){
    //    return RxTextView.textChangeEvents(et)
    //            .debounce(400, TimeUnit.MILLISECONDS)// default Scheduler is Computation
    //            .map(new Func1<TextViewTextChangeEvent, String>() {
    //
    //                @Override
    //                public String call(TextViewTextChangeEvent textViewTextChangeEvent) {
    //                    return textViewTextChangeEvent.text().toString();
    //                }
    //            })
    //            .filter(changes -> !TextUtils.isEmpty(et.getText().toString()))
    //            .observeOn(AndroidSchedulers.mainThread());
    //}


    public static <T> Observable<T> getAsyncObservable(EventSource<T> source) {
        return Observable.defer(new Callable<ObservableSource<T>>() {
            @Override
            public ObservableSource<T> call() throws Exception {
                return Observable.create(new EventObservable<T>(source));
            }
        });

    }

    public static abstract class EventSource<T> {
        private ObservableEmitter<? super T> mSubscriber;

        public void onDelieveryEvent(T event) {
            if (!mSubscriber.isDisposed()) {
                mSubscriber.onNext(event);
            }
        }

        public void onSubscriber(ObservableEmitter<? super T> subscriber) {
            this.mSubscriber = subscriber;
        }

        public abstract void onStart();

        public abstract void onStop();

        public void onError(Throwable error) {
            mSubscriber.onError(error);
        }

        public void onStopClear() {
            mSubscriber = null;
        }

    }

    public static class EventObservable<T> implements ObservableOnSubscribe<T> {
        EventSource mSource;

        public EventObservable(EventSource<T> source) {
            this.mSource = source;
        }


        @Override
        public void subscribe(@NonNull ObservableEmitter<T> e) throws Exception {
            mSource.onSubscriber(e);
            try {
                mSource.onStart();
            } catch (Exception ex) {
                if (!e.isDisposed()) {
                    e.onError(ex);
                }
            }
            e.setCancellable(new Cancellable() {
                @Override
                public void cancel() throws Exception {
                    try {
                        mSource.onStop();
                    } catch (Exception ex) {
                        // checking for subscribers before emitting values
                        if (!e.isDisposed()) {
                            // (2) - reporting exceptions via onError()
                            e.onError(ex);
                        }
                    }
                }
            });
        }
    }


}


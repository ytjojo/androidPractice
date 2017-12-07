package com.ytjojo.rx;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * 用于管理RxBus的事件和Rxjava相关代码的生命周期处理
 * Created by baixiaokang on 16/4/28.
 */
public class RxManage {

    public RxBus mRxBus = RxBus.getDefault();
    private CompositeDisposable mCompositeSubscription = new CompositeDisposable();// 管理订阅者者


    public <T> void register(String tag, Class<T> clazz,Consumer<T> action1) {
        Flowable<T> mObservable = mRxBus.register(tag,clazz);
        mCompositeSubscription.add(mObservable
                .subscribe(action1, (e) -> e.printStackTrace()));
    }
    public <T> void register(Class<T> clazz, Consumer<T> action1){
        Flowable<T> observable = mRxBus.register(clazz);
        mCompositeSubscription.add(observable.observeOn(AndroidSchedulers.mainThread()).subscribe(action1,(e) ->e.printStackTrace()));
    }
    public void add(Disposable m) {
        mCompositeSubscription.add(m);
    }

    public void clear() {
        mCompositeSubscription.clear();// 取消订阅
    }

    public void post(String tag, Object content) {
        mRxBus.post(tag, content);
    }
    public void post(Object object){
        mRxBus.post(object);
    }
}
package com.ytjojo.rx;


import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

/**
 * body为空有两种情况，参考下面两个类的注释
 * see{@link ResponseObserver}
 * sea{@link NullBodyObserver}
 * @param <T>
 */
public abstract class SimpleObserver<T> implements Observer<T> {
    public Disposable disposable;

    @Override
    public void onSubscribe(@NonNull Disposable d) {
        this.disposable = d;
    }


    @Override
    public void onComplete() {

    }
}

package com.ytjojo.rx;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2017/12/11 0011.
 */

public class RxPublish {
    Subject mSubject = PublishSubject.create().toSerialized();
    ConcurrentHashMap<Class<?>,Observable<?>> mMap;
    public <T> void create(Class<T> tClass,Consumer<T> consumer){
        if(mMap == null){
            mMap = new ConcurrentHashMap<>();
        }
        if(mMap.get(tClass)!=null){
            return;
        }
        Observable<T> observable = mSubject.ofType(tClass).throttleFirst(3, TimeUnit.SECONDS)
                .filter(new Predicate<T>() {
                    ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<T>();
                    @Override
                    public boolean test(@NonNull T value) throws Exception {
                        boolean contain = queue.contains(value);
                        if(contain){
                            return false;
                        }
                        add(value);
                        return true;
                    }
                    private void add(T value){
                        queue.add(value);
                        Observable.timer(6,TimeUnit.SECONDS).subscribe(new Consumer<Long>() {
                            @Override
                            public void accept(Long aLong) throws Exception {
                                queue.remove(value);
                            }
                        });
                    }
                });
        mMap.put(tClass,observable);
        observable.subscribe(consumer);
    }
    public void post(Object o){
        mSubject.onNext(o);
    }

}

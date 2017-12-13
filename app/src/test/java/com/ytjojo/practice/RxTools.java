package com.ytjojo.practice;


import org.junit.Test;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

public class RxTools {
    public static void asyncToSync() {
    }

    @Test
    public void testSkip() throws InterruptedException {

        Observable.interval(1, TimeUnit.SECONDS)
                .throttleFirst(4,TimeUnit.SECONDS)
                .map(new Function<Long, Long>() {
                    @Override
                    public Long apply(@NonNull Long aLong) throws Exception {
                        System.out.print("apply"+aLong);
                        if(aLong<12){
                            return new Long(0);
                        }
                        return aLong;
                    }
                })
                .filter(new Predicate<Long>() {
                    ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<Long>();
                    @Override
                    public boolean test(@NonNull Long aLong) throws Exception {
                        if(queue.contains(aLong)){
                            return false;
                        }
                        queue.add(aLong);
                        Observable.timer(8,TimeUnit.SECONDS).subscribe(new Consumer<Long>() {
                            @Override
                            public void accept(Long l1) throws Exception {
                                queue.remove(aLong);
                            }
                        });
                        return true;
                    }
                })
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        System.out.println(aLong +"----along");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
        Thread.sleep(30000);
    }
    @Test
    public void ConcurrentLinkedQueue(){
        ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
//        queue.add("a");
//        queue.add("a");
//        queue.add("a");
        System.out.println(queue.size() +"");
        queue.remove("a");

        System.out.println(queue.size() +"");
    }

}
package com.ytjojo.rx;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.reactivex.FlowableOperator;
import io.reactivex.FlowableSubscriber;
import io.reactivex.Scheduler;
import io.reactivex.internal.subscriptions.SubscriptionHelper;
import io.reactivex.internal.util.BackpressureHelper;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Administrator on 2016/11/10 0010.
 */
public class DoAsync <T> implements FlowableOperator<T,T> {
    final Consumer<T> action;
    final Scheduler scheduler;
    public DoAsync(Scheduler scheduler,Consumer action){
        this.scheduler =scheduler;
        this.action = action;
    }
    public DoAsync(Consumer action){
        this.scheduler = Schedulers.io();
        this.action = action;
    }
    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> s) {

        Scheduler.Worker w = scheduler.createWorker();
        final SubscribeOnSubscriber<T> sos = new SubscribeOnSubscriber<T>(s, w);
        w.schedule(sos);
        return sos;
    }
    static final class SubscribeOnSubscriber<T> extends AtomicReference<Thread>
            implements FlowableSubscriber<T>, Subscription, Runnable {

        private static final long serialVersionUID = 8094547886072529208L;

        final Subscriber<? super T> actual;

        final Scheduler.Worker worker;

        final AtomicReference<Subscription> s;

        final AtomicLong requested;



        SubscribeOnSubscriber(Subscriber<? super T> actual, Scheduler.Worker worker) {
            this.actual = actual;
            this.worker = worker;
            this.s = new AtomicReference<Subscription>();
            this.requested = new AtomicLong();
        }

        @Override
        public void run() {
            lazySet(Thread.currentThread());
            actual.onSubscribe(this);
        }

        @Override
        public void onSubscribe(Subscription s) {
            if (SubscriptionHelper.setOnce(this.s, s)) {
                long r = requested.getAndSet(0L);
                if (r != 0L) {
                    requestUpstream(r, s);
                }
            }
        }

        @Override
        public void onNext(T t) {
            actual.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            actual.onError(t);
            worker.dispose();
        }

        @Override
        public void onComplete() {
            actual.onComplete();
            worker.dispose();
        }

        @Override
        public void request(final long n) {
            if (SubscriptionHelper.validate(n)) {
                Subscription s = this.s.get();
                if (s != null) {
                    requestUpstream(n, s);
                } else {
                    BackpressureHelper.add(requested, n);
                    s = this.s.get();
                    if (s != null) {
                        long r = requested.getAndSet(0L);
                        if (r != 0L) {
                            requestUpstream(r, s);
                        }
                    }
                }
            }
        }

        void requestUpstream(final long n, final Subscription s) {
            if ( Thread.currentThread() == get()) {
                s.request(n);
            } else {
                worker.schedule(new Request(s, n));
            }
        }

        @Override
        public void cancel() {
            SubscriptionHelper.cancel(s);
            worker.dispose();
        }

        static final class Request implements Runnable {
            private final Subscription s;
            private final long n;

            Request(Subscription s, long n) {
                this.s = s;
                this.n = n;
            }

            @Override
            public void run() {
                s.request(n);
            }
        }
    }
}

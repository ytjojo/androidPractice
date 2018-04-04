package com.ytjojo.rx;

import android.support.annotation.CallSuper;

import io.reactivex.annotations.NonNull;

/**
 * Created by Administrator on 2017/12/14 0014.
 * 用于服务器返回body一直为空情况
 * 其实此时你并不用关心返回的数据是什么，只关键是成功或者失败
 * 如{"code":200}
 * 表示请求成功，应该走OnNext
 * 如果不处理Rxjava2会抛出NullPointException ，回调OnError方法
 * 如果不希望这样，定义service的返回值为 Observable<Object>
 * 接口例子:
 *
 * 	@Post("url")
 *   Observable<Object> request(@ArrayItem int id);
 *
 */

public abstract class NullBodyObserver extends SimpleObserver<Object> {
    @Override
    @CallSuper
    public void onNext(@NonNull Object o) {
        onSuccess();
    }

    public  abstract void onSuccess();
}

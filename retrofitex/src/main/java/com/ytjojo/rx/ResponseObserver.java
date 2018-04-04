package com.ytjojo.rx;

import android.support.annotation.CallSuper;

import com.ytjojo.http.ServerResponse;

import io.reactivex.annotations.NonNull;

/**
 * 可用于 返回的body有时候为空，有时不为空
 * {"code":200,"body":true}
 * {"code":200}
 * 如果不用ResponseWraper，Rxjava会发出NullPointException回调onError方法
 * 如果你希望body为空仍然会回调onNext方法你要ResponseWraper作为返回类型
 *  接口例子:
 *
 * @Post("url")
 * Observable<ResponseWraper<Boolean>> request(@ArrayItem int param)
 *
 * (个人见解,对于此种情况回调onError是比较规范的方式，body为空表示数据无返回走OnError正常
 * 不然你每次返回都要判断body是否为null)
 * 如果code 为200时候body一直为空的情况参考{@link NullBodyObserver}
 *
 * @param <U>
 * Created by Administrator on 2017/12/14 0014.
 */
public abstract class ResponseObserver<U> extends SimpleObserver<ServerResponse<U>> {

    @Override
    @CallSuper
    public void onNext(@NonNull ServerResponse<U> responseWraper) {
        body(responseWraper.body);
    }

    public abstract void body(U body);


}
